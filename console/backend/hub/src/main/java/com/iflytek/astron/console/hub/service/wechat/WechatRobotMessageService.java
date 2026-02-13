package com.iflytek.astron.console.hub.service.wechat;

import com.iflytek.astron.console.hub.dto.wechat.WechatRobotMessageDto;
import com.iflytek.astron.console.hub.dto.wechat.WechatRobotReplyDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;

/**
 * 企业微信智能机器人消息服务接口
 *
 * @author Lingma
 */
public interface WechatRobotMessageService {

    /**
     * 解析企业微信机器人消息
     * 
     * @param xmlContent 解密后的XML消息内容
     * @return 解析后的消息DTO
     */
    WechatRobotMessageDto parseMessage(String xmlContent);

    /**
     * 异步处理消息并生成回复
     *
     * @param config
     * @param messageDto 消息DTO
     */
    void processMessageAsync(WechatBotConfig config, WechatRobotMessageDto messageDto);

    /**
     * 处理文本消息
     * 
     * @param messageDto 消息DTO
     * @return 回复消息
     */
    WechatRobotReplyDto processTextMessage(WechatBotConfig config,WechatRobotMessageDto messageDto);

    /**
     * 处理图片消息
     * 
     * @param messageDto 消息DTO
     * @return 回复消息
     */
    WechatRobotReplyDto processImageMessage(WechatRobotMessageDto messageDto);

    /**
     * 处理语音消息
     * 
     * @param messageDto 消息DTO
     * @return 回复消息
     */
    WechatRobotReplyDto processVoiceMessage(WechatRobotMessageDto messageDto);

    /**
     * 处理文件消息
     * 
     * @param messageDto 消息DTO
     * @return 回复消息
     */
    WechatRobotReplyDto processFileMessage(WechatRobotMessageDto messageDto);

    /**
     * 发送被动回复消息
     * 
     * @param replyDto 回复消息DTO
     * @param chatId 会话ID
     * @param aiBotId 机器人ID
     *
     */
    void sendPassiveReply(WechatRobotReplyDto replyDto, String chatId, String aiBotId,WechatBotConfig config);
}