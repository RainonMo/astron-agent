package com.iflytek.astron.console.hub.controller.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.astron.console.commons.annotation.RateLimit;
import com.iflytek.astron.console.commons.annotation.space.EnterprisePreAuth;
import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkConfig;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkSync;
import com.iflytek.astron.console.hub.mapper.wechat.WechatWorkSyncMapper;
import com.iflytek.astron.console.hub.service.casdoor.CasdoorService;
import com.iflytek.astron.console.hub.service.wechat.WechatWorkConfigService;
import com.iflytek.astron.console.hub.service.wechat.WechatWorkSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业微信用户同步控制器
 */
@Slf4j
@RestController
@RequestMapping("/hub/wechat-work")
@Tag(name = "企业微信管理")
@Validated
@RequiredArgsConstructor
public class WechatWorkSyncController {

    private final WechatWorkSyncService wechatWorkSyncService;
    private final WechatWorkConfigService wechatWorkConfigService;
    private final CasdoorService casdoorService;

    /**
     * 获取企微配置
     */
    @GetMapping("/config")
//    @EnterprisePreAuth(module = "企业微信管理", description = "获取企微配置", key = "WechatWorkController_getConfig_GET")
    @Operation(summary = "获取企微配置")
    public ApiResult<WechatWorkConfig> getConfig() {
        return wechatWorkConfigService.getConfig();
    }

    /**
     * 保存或更新企微配置
     */
    @PostMapping("/config")
//    @EnterprisePreAuth(module = "企业微信管理", description = "保存企微配置", key = "WechatWorkController_saveConfig_POST")
    @Operation(summary = "保存或更新企微配置")
    @RateLimit(dimension = "USER", window = 60, limit = 10)
    public ApiResult<String> saveConfig(
            @RequestParam("corpid") String corpid,
            @RequestParam("corpsecret") String corpsecret) {
        return wechatWorkConfigService.saveOrUpdateConfig(corpid, corpsecret);
    }

    /**
     * 同步企业微信用户
     */
    @PostMapping("/sync")
//    @EnterprisePreAuth(module = "企业微信管理", description = "同步企业微信用户", key = "WechatWorkController_sync_POST")
    @Operation(summary = "同步企业微信用户")
    @RateLimit(dimension = "USER", window = 60, limit = 5)
    public ApiResult<String> sync() {
        log.info("收到同步企业微信用户请求");
        return wechatWorkSyncService.syncUsers();
    }

    /**
     * 获取同步用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "获取同步用户列表")
    public ApiResult<List<WechatWorkSync>> getSyncUsers() {
        log.info("收到获取同步用户列表请求");
        return wechatWorkSyncService.getSyncUserList();
    }

    /**
     * 新增Casdoor用户
     */
    @PostMapping("/add-user")
    @Operation(summary = "新增Casdoor用户")
    @RateLimit(dimension = "USER", window = 60, limit = 10)
    public ApiResult<String> addUser(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String phone = request.get("phone");
        log.info("收到新增Casdoor用户请求, userId={}, phone={}", userId, phone);

        if (userId == null || userId.isEmpty()) {
            return ApiResult.error(400, "用户ID不能为空");
        }

        if (phone == null || phone.isEmpty()) {
            return ApiResult.error(400, "手机号不能为空");
        }

        // 简单的手机号格式校验
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return ApiResult.error(400, "手机号格式不正确");
        }

        // 调用Casdoor服务创建用户
        ApiResult<String> result = casdoorService.addUser(userId, phone);

        return result;
    }

}
