package com.iflytek.astron.console.hub.service.casdoor.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.hub.config.CasdoorConfig;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkSync;
import com.iflytek.astron.console.hub.mapper.wechat.WechatWorkSyncMapper;
import com.iflytek.astron.console.hub.service.casdoor.CasdoorService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Casdoor服务实现类
 */
@Slf4j
@Service
public class CasdoorServiceImpl implements CasdoorService {

    private final CasdoorConfig casdoorConfig;
    private final WechatWorkSyncMapper wechatWorkSyncMapper;

    // Casdoor API路径
    private static final String LOGIN_URL = "/api/login";
    private static final String ADD_USER_URL = "/api/add-user";

    // 用于保持会话的 OkHttpClient
    private final OkHttpClient sessionClient;

    // 登录状态标志
    private volatile boolean isLoggedIn = false;

    public CasdoorServiceImpl(CasdoorConfig casdoorConfig, WechatWorkSyncMapper wechatWorkSyncMapper) {
        this.casdoorConfig = casdoorConfig;
        this.wechatWorkSyncMapper = wechatWorkSyncMapper;
        // 创建带有 Cookie 管理的 OkHttpClient
        this.sessionClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    private final Map<String, java.util.List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, java.util.List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                        log.debug("保存Cookie: host={}, cookies={}", url.host(), cookies);
                    }

                    @Override
                    public java.util.List<Cookie> loadForRequest(HttpUrl url) {
                        java.util.List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new java.util.ArrayList<>();
                    }
                })
                .build();
    }

    @Override
    public ApiResult<String> addUser(String userId, String phone) {
        log.info("开始添加用户到Casdoor, userId={}, phone={}", userId, phone);

        try {
            // 1. 登录获取会话（如果尚未登录）
            ensureLoggedIn();
            log.info("登录Casdoor成功");

            // 2. 构建用户数据
            JSONObject userData = buildUserData(userId, phone);
            log.info("构建用户数据: {}", userData.toJSONString());

            // 3. 调用Casdoor新增用户接口
            String result = addUserToCasdoor(userData);
            log.info("Casdoor添加用户结果: {}", result);

            JSONObject resultObj = JSON.parseObject(result);
            String status = resultObj.getString("status");
            String msg = resultObj.getString("msg");

            if ("ok".equals(status)) {
                log.info("用户添加成功, userId={}, phone={}", userId, phone);
                updateUserPhone(userId, phone);
                return ApiResult.success("用户添加成功");
            } else {
                String errorMsg = msg != null ? msg : result;
                log.error("Casdoor添加用户失败: {}", errorMsg);
                return ApiResult.error(500, "添加用户失败: " + errorMsg);
            }

        } catch (Exception e) {
            log.error("添加用户到Casdoor失败", e);
            return ApiResult.error(500, "添加用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户手机号
     */
    private void updateUserPhone(String userId, String phone) {
        try {
            // 查询用户记录
            WechatWorkSync user = wechatWorkSyncMapper.selectOne(
                    new LambdaQueryWrapper<WechatWorkSync>()
                            .eq(WechatWorkSync::getUserid, userId)
            );

            if (user != null) {
                user.setPhone(phone);
                wechatWorkSyncMapper.updateById(user);
                log.info("更新用户手机号成功, userId={}, phone={}", userId, phone);
            } else {
                log.warn("未找到用户记录, userId={}", userId);
            }
        } catch (Exception e) {
            log.error("更新用户手机号失败, userId={}, phone={}", userId, phone, e);
        }
    }

    /**
     * 确保已登录（如果尚未登录则执行登录）
     */
    private synchronized void ensureLoggedIn() throws IOException {
        if (isLoggedIn) {
            log.info("已经登录，跳过登录步骤");
            return;
        }
        login();
        isLoggedIn = true;
    }

    /**
     * 登录Casdoor获取会话
     */
    private void login() throws IOException {
        String url = casdoorConfig.getEndpoint() + LOGIN_URL;

        log.info("Casdoor配置信息: endpoint={}, loginOrganization={}, loginApplication={}",
                casdoorConfig.getEndpoint(), casdoorConfig.getLoginOrganization(), casdoorConfig.getLoginApplication());

        // 构建登录请求体（使用 login-organization 和 login-application）
        JSONObject requestBody = new JSONObject();
        requestBody.put("application", casdoorConfig.getLoginApplication());
        requestBody.put("organization", casdoorConfig.getLoginOrganization());
        requestBody.put("username", casdoorConfig.getAdminUsername());
        requestBody.put("password", casdoorConfig.getAdminPassword());
        requestBody.put("autoSignin", true);
        requestBody.put("signinMethod", "Password");
        requestBody.put("type", "login");

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        log.info("请求登录 URL: {}, 用户名: {}", url, casdoorConfig.getAdminUsername());

        try (Response response = sessionClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("登录响应: {}", responseBody);

            JSONObject jsonObject = JSON.parseObject(responseBody);
            String status = jsonObject.getString("status");

            if (!"ok".equals(status)) {
                String msg = jsonObject.getString("msg");
                throw new RuntimeException("登录失败: " + msg);
            }

            // Cookie 会自动保存在 sessionClient 中
            log.info("登录成功，会话Cookie已保存");
        }
    }

    /**
     * 构建用户数据
     */
    private JSONObject buildUserData(String userId, String phone) {
        JSONObject user = new JSONObject();
        // 使用 user-organization 作为新用户的所属组织
        user.put("owner", casdoorConfig.getUserOrganization());
        user.put("name", userId);
        user.put("displayName", userId);
        user.put("phone", phone);
        user.put("id", userId);
        user.put("type", "normal-user");
        user.put("password", "123456");
        user.put("isGlobalAdmin", false);
        user.put("isForbidden", false);
        user.put("isDeleted", false);

        return user;
    }

    /**
     * 调用Casdoor新增用户接口
     */
    private String addUserToCasdoor(JSONObject userData) throws IOException {
        String url = casdoorConfig.getEndpoint() + ADD_USER_URL;

        RequestBody body = RequestBody.create(
                userData.toJSONString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        log.info("请求新增用户 URL: {}", url);

        try (Response response = sessionClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            return responseBody;
        }
    }

    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 12) + "Aa1!";
    }
}
