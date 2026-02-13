package com.iflytek.astron.console.hub.service.wechat.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.iflytek.astron.console.commons.dto.bot.ChatBotReqDto;
import com.iflytek.astron.console.commons.service.bot.ChatBotDataService;
import com.iflytek.astron.console.commons.util.SseEmitterUtil;
import com.iflytek.astron.console.hub.dto.wechat.WechatRobotMessageDto;
import com.iflytek.astron.console.hub.dto.wechat.WechatRobotReplyDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.service.chat.BotChatService;
import com.iflytek.astron.console.hub.service.wechat.WechatRobotMessageService;
import com.iflytek.astron.console.hub.util.wechat.StreamMessageUtil;
import com.iflytek.astron.console.hub.util.wechat.WXBizMsgCrypt;
import com.iflytek.astron.console.hub.util.wechat.AesException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 企业微信智能机器人消息服务实现
 *
 * @author Lingma
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatRobotMessageServiceImpl implements WechatRobotMessageService {

    private final ChatBotDataService chatBotDataService;
    private final BotChatService botChatService;
    private final StreamMessageUtil streamMessageUtil;

    /**
     * 解析企业微信机器人消息
     */
    @Override
    public WechatRobotMessageDto parseMessage(String xmlContent) {
        try {
            log.debug("Parsing WeChat robot message: {}", xmlContent);
            
            JSONObject json = JSON.parseObject(xmlContent);
            WechatRobotMessageDto messageDto = new WechatRobotMessageDto();
            
            messageDto.setMsgId(json.getString("msgid"));
            messageDto.setAiBotId(json.getString("aibotid"));
            messageDto.setChatId(json.getString("chatid")); //todo
            messageDto.setChatType(json.getString("chattype"));
            
            // 解析发送者信息 todo
            JSONObject fromJson = json.getJSONObject("from");
            if (fromJson != null) {
                WechatRobotMessageDto.FromInfo fromInfo = new WechatRobotMessageDto.FromInfo();
                fromInfo.setUserId(fromJson.getString("userid"));
                messageDto.setFrom(fromInfo);
            }
            
            messageDto.setMsgType(json.getString("msgtype"));
            
            // 根据消息类型解析具体内容
            String msgType = messageDto.getMsgType();
            switch (msgType) {
                case "text":
                    JSONObject textJson = json.getJSONObject("text");
                    if (textJson != null) {
                        WechatRobotMessageDto.TextContent textContent = new WechatRobotMessageDto.TextContent();
                        textContent.setContent(textJson.getString("content"));
                        messageDto.setText(textContent);
                    }
                    break;
                    
                case "image":
                    JSONObject imageJson = json.getJSONObject("image");
                    if (imageJson != null) {
                        WechatRobotMessageDto.ImageContent imageContent = new WechatRobotMessageDto.ImageContent();
                        imageContent.setImageUrl(imageJson.getString("imageUrl"));
                        imageContent.setFileName(imageJson.getString("fileName"));
                        messageDto.setImage(imageContent);
                    }
                    break;
                    
                case "voice":
                    JSONObject voiceJson = json.getJSONObject("voice");
                    if (voiceJson != null) {
                        WechatRobotMessageDto.VoiceContent voiceContent = new WechatRobotMessageDto.VoiceContent();
                        voiceContent.setVoiceUrl(voiceJson.getString("voiceUrl"));
                        voiceContent.setDuration(voiceJson.getInteger("duration"));
                        messageDto.setVoice(voiceContent);
                    }
                    break;
                    
                case "file":
                    JSONObject fileJson = json.getJSONObject("file");
                    if (fileJson != null) {
                        WechatRobotMessageDto.FileContent fileContent = new WechatRobotMessageDto.FileContent();
                        fileContent.setFileUrl(fileJson.getString("fileUrl"));
                        fileContent.setFileName(fileJson.getString("fileName"));
                        fileContent.setFileSize(fileJson.getLong("fileSize"));
                        messageDto.setFile(fileContent);
                    }
                    break;
                    
                case "mixed":
                    JSONObject mixedJson = json.getJSONObject("mixed");
                    if (mixedJson != null) {
                        WechatRobotMessageDto.MixedContent mixedContent = new WechatRobotMessageDto.MixedContent();
                        mixedContent.setContent(mixedJson.getString("content"));
                        messageDto.setMixed(mixedContent);
                    }
                    break;
                    
                case "quote":
                    JSONObject quoteJson = json.getJSONObject("quote");
                    if (quoteJson != null) {
                        WechatRobotMessageDto.QuoteContent quoteContent = new WechatRobotMessageDto.QuoteContent();
                        quoteContent.setQuotedContent(quoteJson.getString("quotedContent"));
                        quoteContent.setContent(quoteJson.getString("content"));
                        messageDto.setQuote(quoteContent);
                    }
                    break;
            }
            
            log.info("Parsed WeChat robot message: msgId={}, msgType={}, chatType={}", 
                    messageDto.getMsgId(), messageDto.getMsgType(), messageDto.getChatType());
            
            return messageDto;
            
        } catch (Exception e) {
            log.error("Failed to parse WeChat robot message: {}", xmlContent, e);
            throw new RuntimeException("Failed to parse WeChat robot message", e);
        }
    }

    /**
     * 异步处理消息并生成回复
     */
    @Override
    @Async
    public void processMessageAsync(WechatBotConfig config, WechatRobotMessageDto messageDto) {
        try {
            log.info("Processing WeChat robot message asynchronously: msgId={}, msgType={}", 
                    messageDto.getMsgId(), messageDto.getMsgType());
            
            WechatRobotReplyDto replyDto = null;
            
            // 根据消息类型进行不同处理
            switch (messageDto.getMsgType()) {
                case "text":
                    replyDto = processTextMessage(config, messageDto);
                    break;
                case "image":
                    replyDto = processImageMessage(messageDto);
                    break;
                case "voice":
                    replyDto = processVoiceMessage(messageDto);
                    break;
                case "file":
                    replyDto = processFileMessage(messageDto);
                    break;
                case "mixed":
                case "quote":
                    // 对于图文混排和引用消息，按文本消息处理
                    replyDto = processTextMessage(config,messageDto);
                    break;
                default:
                    log.warn("Unsupported message type: {}", messageDto.getMsgType());
                    replyDto = WechatRobotReplyDto.createTextReply("暂不支持此类消息类型");
                    break;
            }
            
            // 发送被动回复
            if (replyDto != null) {
                sendPassiveReply(replyDto, messageDto.getChatId(), messageDto.getAiBotId(),config);
            }
            
        } catch (Exception e) {
            log.error("Failed to process WeChat robot message: msgId={}", messageDto.getMsgId(), e);
        }
    }

    /**
     * 处理文本消息
     */
    @Override
    public WechatRobotReplyDto processTextMessage(WechatBotConfig config,WechatRobotMessageDto messageDto) {
        try {
            String content = "";
            if (messageDto.getText() != null) {
                content = messageDto.getText().getContent();
            } else if (messageDto.getMixed() != null) {
                content = messageDto.getMixed().getContent();
            } else if (messageDto.getQuote() != null) {
                content = messageDto.getQuote().getContent();
            }
            
            log.info("Processing text message from user {}: {}", 
                    messageDto.getFrom().getUserId(), content);

            // 构造聊天请求
            ChatBotReqDto chatBotReqDto = new ChatBotReqDto();
            chatBotReqDto.setAsk(content);
            chatBotReqDto.setUid(messageDto.getFrom().getUserId());
            chatBotReqDto.setBotId(Integer.valueOf(config.getAgentIdRef()));
            // 使用chatId作为临时chatId todo
            chatBotReqDto.setChatId(1L);
            
            // 创建SSE Emitter进行流式回复
            SseEmitter sseEmitter = SseEmitterUtil.createSseEmitter();
            String sseId = UUID.randomUUID().toString();
            
            // 创建CompletableFuture来收集回复内容
            CompletableFuture<String> replyFuture = new CompletableFuture<>();
            StringBuilder replyContent = new StringBuilder();
            
            // 设置SSE监听器收集回复内容
            sseEmitter.onCompletion(() -> {
                if (!replyFuture.isDone()) {
                    replyFuture.complete(replyContent.toString());
                }
            });
            
            sseEmitter.onError(throwable -> {
                if (!replyFuture.isDone()) {
                    replyFuture.completeExceptionally(throwable);
                }
            });
            
            // 使用流式消息工具发送回复
            CompletableFuture<Void> streamFuture = streamMessageUtil.sendStreamMessage(
                "正在为您处理请求...", 
                messageDto.getChatId(), 
                messageDto.getAiBotId(), 
                config
            );
            
            // 异步处理聊天请求
            CompletableFuture.runAsync(() -> {
                try {
                    botChatService.chatMessageBot(chatBotReqDto, sseEmitter, sseId, null, null);
                } catch (Exception e) {
                    log.error("Failed to process bot chat", e);
                    replyFuture.completeExceptionally(e);
                }
            });
            
            // 等待回复完成
            String finalReply = replyFuture.get();
            
            // 返回null表示已经通过流式工具发送了回复
            return null;
            
        } catch (Exception e) {
            log.error("Failed to process text message", e);
            // 出错时发送文本回复
            return WechatRobotReplyDto.createTextReply("处理消息时发生错误：" + e.getMessage());
        }
    }

    /**
     * 处理图片消息
     */
    @Override
    public WechatRobotReplyDto processImageMessage(WechatRobotMessageDto messageDto) {
        if (messageDto.getImage() != null) {
            return WechatRobotReplyDto.createTextReply("收到了图片：" + messageDto.getImage().getFileName());
        }
        return WechatRobotReplyDto.createTextReply("收到了图片消息");
    }

    /**
     * 处理语音消息
     */
    @Override
    public WechatRobotReplyDto processVoiceMessage(WechatRobotMessageDto messageDto) {
        if (messageDto.getVoice() != null) {
            return WechatRobotReplyDto.createTextReply("收到了语音消息，时长：" + messageDto.getVoice().getDuration() + "秒");
        }
        return WechatRobotReplyDto.createTextReply("收到了语音消息");
    }

    /**
     * 处理文件消息
     */
    @Override
    public WechatRobotReplyDto processFileMessage(WechatRobotMessageDto messageDto) {
        if (messageDto.getFile() != null) {
            return WechatRobotReplyDto.createTextReply("收到了文件：" + messageDto.getFile().getFileName());
        }
        return WechatRobotReplyDto.createTextReply("收到了文件消息");
    }

    /**
     * 发送被动回复消息 todo
     */
    @Override
    public void sendPassiveReply(WechatRobotReplyDto replyDto, String chatId, String aiBotId,WechatBotConfig config) {
        try {
            log.info("Sending passive reply: chatId={}, aiBotId={}, msgType={}", 
                    chatId, aiBotId, replyDto.getMsgType());
            
            // 将回复DTO转换为JSON
            String replyJson = JSON.toJSONString(replyDto);
            
            // 加密回复消息
            WXBizMsgCrypt pc = new WXBizMsgCrypt(config.getToken(), config.getEncodingAesKey(), "");
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = String.valueOf(System.currentTimeMillis() % 1000000);
            String encryptedReply = pc.encryptMsg(replyJson, timestamp, nonce);
            
            // TODO: 这里应该调用企业微信API发送回复消息
            // 由于需要具体的API端点，暂时只记录日志
            log.debug("Encrypted reply message: {}", encryptedReply);
            
        } catch (AesException e) {
            log.error("Failed to encrypt reply message", e);
        } catch (Exception e) {
            log.error("Failed to send passive reply", e);
        }
    }
}