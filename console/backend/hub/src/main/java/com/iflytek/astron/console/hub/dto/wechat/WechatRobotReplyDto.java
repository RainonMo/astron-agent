package com.iflytek.astron.console.hub.dto.wechat;

import lombok.Data;
import java.util.List;

/**
 * 企业微信智能机器人被动回复消息DTO
 * 支持文本回复和流式消息回复
 *
 * @author Lingma
 */
@Data
public class WechatRobotReplyDto {

    /**
     * 回复消息类型：text(文本)、stream(流式消息)
     */
    private String msgType;

    /**
     * 文本消息内容
     */
    private TextReply text;

    /**
     * 流式消息内容（符合企业微信官方格式）
     */
    private StreamReply stream;

    /**
     * 文本回复内容
     */
    @Data
    public static class TextReply {
        /**
         * 文本内容
         */
        private String content;
    }

    /**
     * 流式消息回复内容（符合企业微信官方格式）
     */
    @Data
    public static class StreamReply {
        /**
         * 流式消息ID（用于标识同一回复的不同片段）
         */
        private String id;
        
        /**
         * 流式消息是否结束
         */
        private Boolean finish;
        
        /**
         * 流式消息内容，最长不超过20480个字节
         */
        private String content;
        
        /**
         * 流式消息图文混排消息列表
         */
        private List<MsgItem> msgItem;
        
        /**
         * 反馈信息
         */
        private Feedback feedback;
    }
    
    /**
     * 消息项（用于图文混排）
     */
    @Data
    public static class MsgItem {
        /**
         * 消息类型，目前支持：image
         */
        private String msgtype;
        
        /**
         * 图片内容
         */
        private ImageContent image;
    }
    
    /**
     * 图片内容
     */
    @Data
    public static class ImageContent {
        /**
         * 图片内容的base64编码
         */
        private String base64;
        
        /**
         * 图片MD5值
         */
        private String md5;
    }
    
    /**
     * 反馈信息
     */
    @Data
    public static class Feedback {
        /**
         * 反馈ID
         */
        private String id;
    }

    /**
     * 创建文本回复
     * 
     * @param content 文本内容
     * @return 文本回复对象
     */
    public static WechatRobotReplyDto createTextReply(String content) {
        WechatRobotReplyDto reply = new WechatRobotReplyDto();
        reply.setMsgType("text");
        
        TextReply textReply = new TextReply();
        textReply.setContent(content);
        reply.setText(textReply);
        
        return reply;
    }

    /**
     * 创建流式消息回复开始
     * 
     * @param streamId 流式消息ID
     * @param content 初始内容
     * @return 流式消息回复对象
     */
    public static WechatRobotReplyDto createStreamStart(String streamId, String content) {
        return createStreamReply(streamId, content, false, null, null);
    }

    /**
     * 创建流式消息回复中间片段
     * 
     * @param streamId 流式消息ID
     * @param content 内容片段
     * @return 流式消息回复对象
     */
    public static WechatRobotReplyDto createStreamContinue(String streamId, String content) {
        return createStreamReply(streamId, content, false, null, null);
    }

    /**
     * 创建流式消息回复结束
     * 
     * @param streamId 流式消息ID
     * @param content 最终内容
     * @param msgItems 图文混排消息列表（可选）
     * @param feedbackId 反馈ID（可选）
     * @return 流式消息回复对象
     */
    public static WechatRobotReplyDto createStreamEnd(String streamId, String content, List<MsgItem> msgItems, String feedbackId) {
        return createStreamReply(streamId, content, true, msgItems, feedbackId);
    }

    /**
     * 创建简单的流式消息回复结束（无图文和反馈）
     * 
     * @param streamId 流式消息ID
     * @param content 最终内容
     * @return 流式消息回复对象
     */
    public static WechatRobotReplyDto createStreamEnd(String streamId, String content) {
        return createStreamEnd(streamId, content, null, null);
    }

    /**
     * 创建流式消息回复
     * 
     * @param streamId 流式消息ID
     * @param content 内容
     * @param finish 是否结束
     * @param msgItems 图文混排消息列表
     * @param feedbackId 反馈ID
     * @return 流式消息回复对象
     */
    private static WechatRobotReplyDto createStreamReply(String streamId, String content, Boolean finish, List<MsgItem> msgItems, String feedbackId) {
        WechatRobotReplyDto reply = new WechatRobotReplyDto();
        reply.setMsgType("stream");
        
        StreamReply streamReply = new StreamReply();
        streamReply.setId(streamId);
        streamReply.setFinish(finish);
        streamReply.setContent(content);
        streamReply.setMsgItem(msgItems);
        
        if (feedbackId != null) {
            Feedback feedback = new Feedback();
            feedback.setId(feedbackId);
            streamReply.setFeedback(feedback);
        }
        
        reply.setStream(streamReply);
        
        return reply;
    }
    
    /**
     * 创建图片消息项
     * 
     * @param base64 图片base64编码
     * @param md5 图片MD5值
     * @return 消息项
     */
    public static MsgItem createImageMsgItem(String base64, String md5) {
        MsgItem msgItem = new MsgItem();
        msgItem.setMsgtype("image");
        
        ImageContent image = new ImageContent();
        image.setBase64(base64);
        image.setMd5(md5);
        msgItem.setImage(image);
        
        return msgItem;
    }
}