package com.iflytek.astron.console.hub.controller.wechat;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.astron.console.commons.response.ApiResult;
import com.iflytek.astron.console.hub.dto.wechat.WechatBotConfigDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.service.wechat.WechatBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 企业微信机器人配置控制器
 *
 * @author Lingma
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat-bot")
@RequiredArgsConstructor
@Tag(name = "企业微信机器人管理", description = "企业微信机器人配置管理API")
public class WechatBotCallBackController {

    private final WechatBotService wechatBotService;

    @GetMapping("/page")
    @Operation(summary = "分页查询机器人配置")
    public ApiResult<IPage<WechatBotConfig>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        
        Page<WechatBotConfig> page = new Page<>(current, size);
        IPage<WechatBotConfig> result = wechatBotService.page(page, keyword);
        return ApiResult.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询机器人配置")
    public ApiResult<WechatBotConfig> getById(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        WechatBotConfig config = wechatBotService.getById(id);
        return ApiResult.success(config);
    }

    @PostMapping
    @Operation(summary = "创建机器人配置")
    public ApiResult<Boolean> create(
            @Parameter(description = "配置信息") @Valid @RequestBody WechatBotConfigDto dto) {
        boolean result = wechatBotService.create(dto);
        return ApiResult.success(result);
    }

    @PutMapping
    @Operation(summary = "更新机器人配置")
    public ApiResult<Boolean> update(
            @Parameter(description = "配置信息") @Valid @RequestBody WechatBotConfigDto dto) {
        boolean result = wechatBotService.update(dto);
        return ApiResult.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除机器人配置")
    public ApiResult<Boolean> delete(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        boolean result = wechatBotService.delete(id);
        return ApiResult.success(result);
    }

    @GetMapping("/generate-key")
    @Operation(summary = "生成机器人唯一标识")
    public ApiResult<String> generateBotKey() {
        String botKey = java.util.UUID.randomUUID().toString().replace("-", "");
        return ApiResult.success(botKey);
    }

    /**
     * URL验证接口（GET请求）
     * 当在企业微信后台配置回调URL时，会发送GET请求进行URL有效性验证
     *
     * @param msgSignature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 加密的随机字符串
     * @return 解密后的明文echostr
     */
    @RequestMapping(value = "/callback/{botKey}",method = {RequestMethod.GET, RequestMethod.POST})
    public String handleCallback(
            @Parameter(description = "机器人key") @PathVariable String botKey,
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr,
            @RequestBody(required = false) String postData) {

        log.info("WeChat robot callback received: botKey={},msg_signature={}, timestamp={}, nonce={}, echostr={}, postData={}",
                botKey,msgSignature, timestamp, nonce, echostr, postData);

        try {

            // URL验证（GET请求）
            if (StringUtils.hasText(echostr)) {
                return wechatBotService.verifyUrl(botKey,msgSignature, timestamp, nonce, echostr);
            }

            // 消息接收与处理（POST请求）
            if (StringUtils.hasText(postData)) {
                return wechatBotService.handleMessage(botKey,msgSignature, timestamp, nonce, postData);
            }

            log.warn("Invalid request: neither echostr nor postData provided");
            return "fail";

        } catch (Exception e) {
            log.error("Failed to handle WeChat robot callback: msg_signature={}, timestamp={}, nonce={}",
                    msgSignature, timestamp, nonce, e);
            return "fail";
        }
    }

}