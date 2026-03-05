package com.iflytek.astron.console.hub.entity.wechat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wechat_work_config")
@Schema(name = "WechatWorkConfig", description = "企业微信配置表")
public class WechatWorkConfig {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "企业ID")
    private Long enterpriseId;

    @Schema(description = "企业微信企业ID")
    private String corpid;

    @Schema(description = "企业微信应用凭证密钥")
    private String corpsecret;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
