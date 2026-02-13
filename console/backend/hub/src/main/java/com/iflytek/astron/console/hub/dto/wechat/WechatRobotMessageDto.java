package com.iflytek.astron.console.hub.dto.wechat;

import lombok.Data;

/**
 * 企业微信智能机器人消息DTO
 * 支持多种消息类型：文本、图片、语音、文件等
 *
 * @author Lingma
 */
@Data
public class WechatRobotMessageDto {

    /**
     * 消息ID
     */
    private String msgId;

    /**
     * 智能机器人ID
     */
    private String aiBotId;

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 会话类型：single(单聊)、group(群聊)
     */
    private String chatType;

    /**
     * 消息发送者信息
     */
    private FromInfo from;

    /**
     * 消息类型：text(文本)、image(图片)、voice(语音)、file(文件)、mixed(图文混排)、quote(引用)
     */
    private String msgType;

    /**
     * 文本消息内容
     */
    private TextContent text;

    /**
     * 图片消息内容
     */
    private ImageContent image;

    /**
     * 语音消息内容
     */
    private VoiceContent voice;

    /**
     * 文件消息内容
     */
    private FileContent file;

    /**
     * 图文混排消息内容
     */
    private MixedContent mixed;

    /**
     * 引用消息内容
     */
    private QuoteContent quote;

    /**
     * 发送者信息
     */
    @Data
    public static class FromInfo {
        /**
         * 用户ID
         */
        private String userId;
    }

    /**
     * 文本消息内容
     */
    @Data
    public static class TextContent {
        /**
         * 文本内容
         */
        private String content;
    }

    /**
     * 图片消息内容
     */
    @Data
    public static class ImageContent {
        /**
         * 图片URL
         */
        private String imageUrl;
        
        /**
         * 图片文件名
         */
        private String fileName;
    }

    /**
     * 语音消息内容
     */
    @Data
    public static class VoiceContent {
        /**
         * 语音URL
         */
        private String voiceUrl;
        
        /**
         * 语音时长（秒）
         */
        private Integer duration;
    }

    /**
     * 文件消息内容
     */
    @Data
    public static class FileContent {
        /**
         * 文件URL
         */
        private String fileUrl;
        
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 文件大小（字节）
         */
        private Long fileSize;
    }

    /**
     * 图文混排消息内容
     */
    @Data
    public static class MixedContent {
        /**
         * 图文混排内容（HTML格式）
         */
        private String content;
    }

    /**
     * 引用消息内容
     */
    @Data
    public static class QuoteContent {
        /**
         * 被引用的消息内容
         */
        private String quotedContent;
        
        /**
         * 当前回复内容
         */
        private String content;
    }
}