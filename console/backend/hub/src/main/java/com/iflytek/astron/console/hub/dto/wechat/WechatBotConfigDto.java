package com.iflytek.astron.console.hub.dto.wechat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * 企业微信机器人配置DTO
 *
 * @author Lingma
 */
@Data
@Schema(name = "WechatBotConfigDto", description = "企业微信机器人配置DTO")
public class WechatBotConfigDto {

    @Schema(description = "主键ID")
    private Long id;

    @NotBlank(message = "机器人key不能为空")
    @Schema(description = "机器人唯一标识key")
    private String botKey;

    @NotBlank(message = "企业ID不能为空")
    @Schema(description = "企业ID")
    private String corpId;

    @NotBlank(message = "应用ID不能为空")
    @Schema(description = "应用ID")
    private String agentId;

    @NotBlank(message = "Token不能为空")
    @Schema(description = "回调Token")
    private String token;

    @NotBlank(message = "EncodingAESKey不能为空")
    @Schema(description = "消息加解密密钥")
    private String encodingAesKey;

    @NotBlank(message = "智能体ID不能为空")
    @Schema(description = "关联的智能体ID")
    private String agentIdRef;

    @Schema(description = "回调URL")
    private String callbackUrl;

    @Schema(description = "是否启用")
    private Boolean isActive;
}