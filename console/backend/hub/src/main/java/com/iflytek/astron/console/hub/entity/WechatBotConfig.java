package com.iflytek.astron.console.hub.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业微信机器人配置实体类
 *
 * @author Lingma
 */
@Data
@TableName("wechat_bot_config")
@Schema(name = "WechatBotConfig", description = "企业微信机器人配置表")
public class WechatBotConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "机器人唯一标识key")
    private String botKey;

    @Schema(description = "企业ID")
    private String corpId;

    @Schema(description = "应用ID")
    private String agentId;

    @Schema(description = "回调Token")
    private String token;

    @Schema(description = "消息加解密密钥")
    private String encodingAesKey;

    @Schema(description = "关联的智能体ID")
    private String agentIdRef;

    @Schema(description = "回调URL")
    private String callbackUrl;

    @Schema(description = "是否启用")
    private Boolean isActive;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "删除标识")
    @TableLogic
    private Integer deleted;
}