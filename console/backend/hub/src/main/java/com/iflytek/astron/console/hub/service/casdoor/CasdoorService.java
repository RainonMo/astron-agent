package com.iflytek.astron.console.hub.service.casdoor;

import com.iflytek.astron.console.commons.response.ApiResult;

/**
 * Casdoor服务接口
 */
public interface CasdoorService {

    /**
     * 新增用户到Casdoor
     *
     * @param userId 用户ID
     * @param phone 手机号
     * @return 操作结果
     */
    ApiResult<String> addUser(String userId, String phone);
}
