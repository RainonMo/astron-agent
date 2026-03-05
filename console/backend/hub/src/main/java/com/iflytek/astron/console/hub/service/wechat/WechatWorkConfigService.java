package com.iflytek.astron.console.hub.service.wechat;

import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkConfig;

/**
 * 企业微信配置服务接口
 */
public interface WechatWorkConfigService {

    /**
     * 获取当前企业的企微配置
     */
    ApiResult<WechatWorkConfig> getConfig();

    /**
     * 保存或更新企微配置
     */
    ApiResult<String> saveOrUpdateConfig(String corpid, String corpsecret);
}
