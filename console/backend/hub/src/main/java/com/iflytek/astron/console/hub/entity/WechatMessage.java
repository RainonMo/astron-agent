package com.iflytek.astron.console.hub.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业微信消息记录实体类
 *
 * @author Lingma
 */
@Data
@TableName("wechat_message")
@Schema(name = "WechatMessage", description = "企业微信消息记录表")
public class WechatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "机器人key")
    private String botKey;

    @Schema(description = "消息ID")
    private String messageId;

    @Schema(description = "发送方用户ID")
    private String fromUser;

    @Schema(description = "接收方用户ID")
    private String toUser;

    @Schema(description = "消息类型(text/image/voice/video/location/link)")
    private String msgType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "媒体文件ID")
    private String mediaId;

    @Schema(description = "图片URL")
    private String picUrl;

    @Schema(description = "语音格式")
    private String format;

    @Schema(description = "语音识别结果")
    private String recognition;

    @Schema(description = "缩略图媒体ID")
    private String thumbMediaId;

    @Schema(description = "地理位置纬度")
    private BigDecimal locationX;

    @Schema(description = "地理位置经度")
    private BigDecimal locationY;

    @Schema(description = "地图缩放大小")
    private Integer scale;

    @Schema(description = "地理位置信息")
    private String label;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "链接URL")
    private String url;

    @Schema(description = "智能体回复内容")
    private String agentResponse;

    @Schema(description = "回复状态(success/error)")
    private String responseStatus;

    @Schema(description = "回复时间")
    private LocalDateTime responseTime;

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