package com.iflytek.astron.console.hub.service.wechat;

import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkSync;

import java.util.List;

/**
 * 企业微信用户同步服务接口
 */
public interface WechatWorkSyncService {

    /**
     * 同步企业微信用户
     * 1. 获取access_token
     * 2. 获取成员ID列表
     * 3. 保存到数据库
     *
     * @return 同步结果
     */
    ApiResult<String> syncUsers();

    /**
     * 获取当前企业的同步用户列表
     *
     * @return 用户列表
     */
    ApiResult<List<WechatWorkSync>> getSyncUserList();
}
