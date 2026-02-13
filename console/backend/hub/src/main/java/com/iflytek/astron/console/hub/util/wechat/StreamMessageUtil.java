package com.iflytek.astron.console.hub.util.wechat;

import com.iflytek.astron.console.hub.dto.wechat.WechatRobotReplyDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.service.wechat.WechatRobotMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 企业微信流式消息处理工具类
 * 提供流式消息的分块发送和管理功能
 *
 * @author Lingma
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamMessageUtil {

    private final WechatRobotMessageService wechatRobotMessageService;

    /**
     * 发送流式消息
     * 
     * @param content 完整的回复内容
     * @param chatId 会话ID
     * @param aiBotId 机器人ID
     * @param config 机器人配置
     * @param chunkSize 每个片段的字符数
     * @return CompletableFuture表示异步处理完成
     */
    public CompletableFuture<Void> sendStreamMessage(String content, String chatId, String aiBotId, 
                                                   WechatBotConfig config, int chunkSize) {
        return CompletableFuture.runAsync(() -> {
            try {
                String streamId = UUID.randomUUID().toString();
                
                // 发送开始消息
                WechatRobotReplyDto startReply = WechatRobotReplyDto.createStreamStart(streamId, "");
                wechatRobotMessageService.sendPassiveReply(startReply, chatId, aiBotId, config);
                log.debug("Sent stream start message: streamId={}", streamId);
                
                // 分块发送中间内容
                int totalLength = content.length();
                int sentLength = 0;
                
                while (sentLength < totalLength) {
                    int endIndex = Math.min(sentLength + chunkSize, totalLength);
                    String chunk = content.substring(sentLength, endIndex);
                    
                    WechatRobotReplyDto continueReply = WechatRobotReplyDto.createStreamContinue(streamId, chunk);
                    wechatRobotMessageService.sendPassiveReply(continueReply, chatId, aiBotId, config);
                    log.debug("Sent stream continue message: streamId={}, chunkLength={}", streamId, chunk.length());
                    
                    sentLength = endIndex;
                    
                    // 添加小延迟避免发送过于频繁
                    Thread.sleep(100);
                }
                
                // 发送结束消息
                WechatRobotReplyDto endReply = WechatRobotReplyDto.createStreamEnd(streamId, content);
                wechatRobotMessageService.sendPassiveReply(endReply, chatId, aiBotId, config);
                log.debug("Sent stream end message: streamId={}", streamId);
                
            } catch (Exception e) {
                log.error("Failed to send stream message", e);
                throw new RuntimeException("Failed to send stream message", e);
            }
        });
    }

    /**
     * 发送带图片的流式消息结束
     * 
     * @param content 完整的回复内容
     * @param chatId 会话ID
     * @param aiBotId 机器人ID
     * @param config 机器人配置
     * @param imageItems 图片消息项列表
     * @param feedbackId 反馈ID
     * @return CompletableFuture表示异步处理完成
     */
    public CompletableFuture<Void> sendStreamMessageWithImages(String content, String chatId, String aiBotId,
                                                             WechatBotConfig config, List<WechatRobotReplyDto.MsgItem> imageItems,
                                                             String feedbackId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String streamId = UUID.randomUUID().toString();
                
                // 发送开始消息
                WechatRobotReplyDto startReply = WechatRobotReplyDto.createStreamStart(streamId, "");
                wechatRobotMessageService.sendPassiveReply(startReply, chatId, aiBotId, config);
                log.debug("Sent stream start message with images: streamId={}", streamId);
                
                // 发送结束消息，包含图片
                WechatRobotReplyDto endReply = WechatRobotReplyDto.createStreamEnd(streamId, content, imageItems, feedbackId);
                wechatRobotMessageService.sendPassiveReply(endReply, chatId, aiBotId, config);
                log.debug("Sent stream end message with images: streamId={}", streamId);
                
            } catch (Exception e) {
                log.error("Failed to send stream message with images", e);
                throw new RuntimeException("Failed to send stream message with images", e);
            }
        });
    }

    /**
     * 发送简单流式消息（默认分块大小）
     * 
     * @param content 完整的回复内容
     * @param chatId 会话ID
     * @param aiBotId 机器人ID
     * @param config 机器人配置
     * @return CompletableFuture表示异步处理完成
     */
    public CompletableFuture<Void> sendStreamMessage(String content, String chatId, String aiBotId, WechatBotConfig config) {
        return sendStreamMessage(content, chatId, aiBotId, config, 100);
    }

    /**
     * 创建图片消息项
     * 
     * @param base64 图片base64编码
     * @param md5 图片MD5值
     * @return 消息项
     */
    public WechatRobotReplyDto.MsgItem createImageMsgItem(String base64, String md5) {
        return WechatRobotReplyDto.createImageMsgItem(base64, md5);
    }
}