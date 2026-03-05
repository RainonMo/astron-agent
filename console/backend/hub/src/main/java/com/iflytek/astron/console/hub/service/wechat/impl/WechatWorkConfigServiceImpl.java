package com.iflytek.astron.console.hub.service.wechat.impl;

import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.commons.util.space.EnterpriseInfoUtil;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkConfig;
import com.iflytek.astron.console.hub.mapper.wechat.WechatWorkConfigMapper;
import com.iflytek.astron.console.hub.service.wechat.WechatWorkConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业微信配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatWorkConfigServiceImpl implements WechatWorkConfigService {

    private final WechatWorkConfigMapper wechatWorkConfigMapper;

    @Override
    public ApiResult<WechatWorkConfig> getConfig() {
        Long enterpriseId = EnterpriseInfoUtil.getEnterpriseId();
        log.info("获取企业微信配置, enterpriseId={}", enterpriseId);

        WechatWorkConfig config = wechatWorkConfigMapper.selectByEnterpriseId(enterpriseId);

        if (config != null) {
            // 不返回secret，前端不需要知道
            config.setCorpsecret(null);
        }

        return ApiResult.success(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<String> saveOrUpdateConfig(String corpid, String corpsecret) {
        Long enterpriseId = EnterpriseInfoUtil.getEnterpriseId();
        log.info("保存企业微信配置, enterpriseId={}", enterpriseId);

        if (corpid == null || corpid.isEmpty() || corpsecret == null || corpsecret.isEmpty()) {
            return ApiResult.error(400, "corpid和corpsecret不能为空");
        }

        // 查询是否已有配置
        WechatWorkConfig existingConfig = wechatWorkConfigMapper.selectByEnterpriseId(enterpriseId);

        WechatWorkConfig config = new WechatWorkConfig();
        config.setEnterpriseId(enterpriseId);
        config.setCorpid(corpid);
        config.setCorpsecret(corpsecret);

        if (existingConfig != null) {
            // 更新
            config.setId(existingConfig.getId());
            wechatWorkConfigMapper.updateById(config);
            log.info("更新企业微信配置成功, id={}", config.getId());
        } else {
            // 新增
            wechatWorkConfigMapper.insert(config);
            log.info("新增企业微信配置成功, id={}", config.getId());
        }

        return ApiResult.success("保存成功");
    }
}
