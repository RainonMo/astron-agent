package com.iflytek.astron.console.hub.service.wechat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.commons.util.space.EnterpriseInfoUtil;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkConfig;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkSync;
import com.iflytek.astron.console.hub.mapper.wechat.WechatWorkConfigMapper;
import com.iflytek.astron.console.hub.mapper.wechat.WechatWorkSyncMapper;
import com.iflytek.astron.console.hub.service.wechat.WechatWorkSyncService;
import com.iflytek.astron.console.toolkit.util.OkHttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.time.Duration;
import java.util.*;

/**
 * 企业微信用户同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatWorkSyncServiceImpl implements WechatWorkSyncService {

    private final WechatWorkSyncMapper wechatWorkSyncMapper;
    private final WechatWorkConfigMapper wechatWorkConfigMapper;
    private final RedissonClient redissonClient;

    // 企业微信API地址
    private static final String WECHAT_WORK_API_BASE = "https://qyapi.weixin.qq.com/cgi-bin";
    private static final String GET_TOKEN_URL = WECHAT_WORK_API_BASE + "/gettoken";
    private static final String GET_USER_LIST_ID_URL = WECHAT_WORK_API_BASE + "/user/list_id";

    // Redis缓存key前缀
    private static final String ACCESS_TOKEN_KEY_PREFIX = "wechat:work:access_token:";
    private static final Duration ACCESS_TOKEN_EXPIRE = Duration.ofSeconds(7000);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<String> syncUsers() {
        Long enterpriseId = EnterpriseInfoUtil.getEnterpriseId();
        log.info("开始同步企业微信用户, enterpriseId={}", enterpriseId);

        // 从数据库获取配置
        WechatWorkConfig config = wechatWorkConfigMapper.selectByEnterpriseId(enterpriseId);
        if (config == null) {
            log.error("企业微信配置未设置, enterpriseId={}", enterpriseId);
            return ApiResult.error(500, "企业微信配置未设置，请先配置corpid和corpsecret");
        }

        String corpId = config.getCorpid();
        String corpSecret = config.getCorpsecret();

        if (corpId == null || corpId.isEmpty() || corpSecret == null || corpSecret.isEmpty()) {
            log.error("企业微信配置不完整, enterpriseId={}", enterpriseId);
            return ApiResult.error(500, "企业微信配置不完整，请检查corpid和corpsecret");
        }

        try {
            // 1. 获取access_token
            String accessToken = getAccessToken(enterpriseId, corpId, corpSecret);
            log.info("获取access_token成功");

            // 2. 获取成员ID列表
            List<WechatWorkSync> userList = fetchUserList(accessToken);
            log.info("获取到{}个企业微信用户", userList.size());

            if (userList.isEmpty()) {
                return ApiResult.success("同步完成，未获取到用户数据");
            }

            // 3. 生成同步批次号
            String syncBatchId = generateSyncBatchId();

            // 4. 设置批次号、状态和企业ID
            for (WechatWorkSync user : userList) {
                user.setSyncBatchId(syncBatchId);
                user.setSyncStatus(1);
                user.setEnterpriseId(enterpriseId);
            }

            // 5. 批量保存到数据库
            wechatWorkSyncMapper.batchInsertOrUpdate(userList);
            log.info("成功保存{}个用户到数据库", userList.size());

            return ApiResult.success("同步成功，共同步" + userList.size() + "个用户");

        } catch (Exception e) {
            log.error("同步企业微信用户失败", e);
            return ApiResult.error(500, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取access_token
     */
    private String getAccessToken(Long enterpriseId, String corpId, String corpSecret) {
        String cacheKey = ACCESS_TOKEN_KEY_PREFIX + enterpriseId;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String cachedToken = bucket.get();

        if (cachedToken != null && !cachedToken.isEmpty()) {
            log.debug("从缓存获取access_token, enterpriseId={}", enterpriseId);
            return cachedToken;
        }

        String url = GET_TOKEN_URL + "?corpid=" + corpId + "&corpsecret=" + corpSecret;
        String response = OkHttpUtil.get(url, null);

        log.debug("获取access_token响应: {}", response);

        JSONObject jsonObject = JSON.parseObject(response);
        Integer errCode = jsonObject.getInteger("errcode");

        if (errCode != null && errCode != 0) {
            String errMsg = jsonObject.getString("errmsg");
            throw new RuntimeException("获取access_token失败: " + errMsg);
        }

        String accessToken = jsonObject.getString("access_token");
        Integer expiresIn = jsonObject.getInteger("expires_in");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new RuntimeException("获取access_token失败: 返回结果中无access_token");
        }

        Duration expireDuration = expiresIn != null
                ? Duration.ofSeconds(expiresIn - 200)
                : ACCESS_TOKEN_EXPIRE;
        bucket.set(accessToken, expireDuration);

        return accessToken;
    }

    /**
     * 获取成员ID列表
     */
    private List<WechatWorkSync> fetchUserList(String accessToken) {
        List<WechatWorkSync> allUsers = new ArrayList<>();
        String cursor = null;
        int limit = 10000;

        do {
            String url = GET_USER_LIST_ID_URL + "?access_token=" + accessToken;

            JSONObject requestBody = new JSONObject();
            requestBody.put("limit", limit);
            if (cursor != null && !cursor.isEmpty()) {
                requestBody.put("cursor", cursor);
            }

            String response = OkHttpUtil.post(url, requestBody.toJSONString());
            log.debug("获取用户列表响应: {}", response);

            JSONObject jsonObject = JSON.parseObject(response);
            Integer errCode = jsonObject.getInteger("errcode");

            if (errCode != null && errCode != 0) {
                String errMsg = jsonObject.getString("errmsg");
                throw new RuntimeException("获取成员列表失败: " + errMsg);
            }

            List<JSONObject> deptUserList = jsonObject.getList("dept_user", JSONObject.class);
            if (deptUserList != null && !deptUserList.isEmpty()) {
                Map<String, WechatWorkSync> userMap = new LinkedHashMap<>();

                for (JSONObject deptUser : deptUserList) {
                    String userid = deptUser.getString("userid");
                    Long department = deptUser.getLong("department");

                    if (!userMap.containsKey(userid)) {
                        WechatWorkSync sync = new WechatWorkSync();
                        sync.setUserid(userid);
                        sync.setDepartment(department);
                        userMap.put(userid, sync);
                    }
                }

                allUsers.addAll(userMap.values());
            }

            cursor = jsonObject.getString("next_cursor");

        } while (cursor != null && !cursor.isEmpty());

        return allUsers;
    }

    /**
     * 生成同步批次号
     */
    private String generateSyncBatchId() {
        return "SYNC_" + System.currentTimeMillis();
    }

    @Override
    public ApiResult<List<WechatWorkSync>> getSyncUserList() {
        Long enterpriseId = EnterpriseInfoUtil.getEnterpriseId();
        log.info("获取企业微信同步用户列表, enterpriseId={}", enterpriseId);

        try {
            // 使用 MyBatis-Plus 的 query wrapper 查询当前企业的用户
            List<WechatWorkSync> userList = wechatWorkSyncMapper.selectList(
                new LambdaQueryWrapper<WechatWorkSync>()
                    .eq(WechatWorkSync::getEnterpriseId, enterpriseId)
                    .orderByDesc(WechatWorkSync::getCreateTime)
            );

            log.info("获取到{}个同步用户", userList.size());
            return ApiResult.success(userList);
        } catch (Exception e) {
            log.error("获取同步用户列表失败", e);
            return ApiResult.error(500, "获取用户列表失败: " + e.getMessage());
        }
    }
}
