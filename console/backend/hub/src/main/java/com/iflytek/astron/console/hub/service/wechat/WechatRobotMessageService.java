package com.iflytek.astron.console.hub.service.wechat;

import com.iflytek.astron.console.hub.dto.wechat.WechatRobotMessageDto;
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
     * 同步处理消息并生成流式回复
     *
     * @param config 机器人配置
     * @param messageDto 消息DTO
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @return 加密后的回复消息
     */
    String processMessageSync(WechatBotConfig config, WechatRobotMessageDto messageDto, String timestamp, String nonce);

}