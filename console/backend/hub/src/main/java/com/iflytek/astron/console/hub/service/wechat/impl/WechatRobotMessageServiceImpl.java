package com.iflytek.astron.console.hub.service.wechat.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.iflytek.astron.console.commons.dto.bot.ChatBotReqDto;
import com.iflytek.astron.console.commons.util.SseEmitterUtil;
import com.iflytek.astron.console.commons.workflow.WorkflowListener;
import com.iflytek.astron.console.hub.dto.wechat.WechatRobotMessageDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.service.chat.BotChatService;
import com.iflytek.astron.console.hub.service.wechat.WechatRobotMessageService;
import com.iflytek.astron.console.hub.util.wechat.json.WXBizJsonMsgCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业微信智能机器人消息服务实现
 *
 * @author Lingma
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatRobotMessageServiceImpl implements WechatRobotMessageService {

    private final BotChatService botChatService;
        
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
                    
                case "event":
                    JSONObject eventJson = json.getJSONObject("event");
                    if (eventJson != null) {
                        WechatRobotMessageDto.EventContent eventContent = new WechatRobotMessageDto.EventContent();
                        eventContent.setEventType(eventJson.getString("eventType"));
                        eventContent.setEventData(eventJson.getString("eventData"));
                        messageDto.setEvent(eventContent);
                    }
                    break;
                    
                case "stream":
                    JSONObject streamRefreshJson = json.getJSONObject("stream");
                    if (streamRefreshJson != null) {
                        WechatRobotMessageDto.StreamRefreshContent streamRefreshContent = new WechatRobotMessageDto.StreamRefreshContent();
                        streamRefreshContent.setStreamId(streamRefreshJson.getString("id"));
                        messageDto.setStreamRefresh(streamRefreshContent);
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
     * 同步处理消息并生成流式回复
     */
    @Override
    public String processMessageSync(WechatBotConfig config, WechatRobotMessageDto messageDto, String timestamp, String nonce) {
        try {
            log.info("Processing WeChat robot message synchronously: msgId={}, msgType={}", 
                    messageDto.getMsgId(), messageDto.getMsgType());
            
            String streamReply = null;
            
            // 根据消息类型进行不同处理
            switch (messageDto.getMsgType()) {
                case "text":
                    streamReply = processTextMessageSync(config, messageDto, timestamp, nonce);
                    break;
                case "image":
                    streamReply = processImageMessageSync(messageDto, timestamp, nonce, config);
                    break;
                case "voice":
                    streamReply = processVoiceMessageSync(messageDto, timestamp, nonce, config);
                    break;
                case "file":
                    streamReply = processFileMessageSync(messageDto, timestamp, nonce, config);
                    break;
                case "mixed":
                case "quote":
                    // 对于图文混排和引用消息，按文本消息处理
                    streamReply = processTextMessageSync(config, messageDto, timestamp, nonce);
                    break;
                case "event":
                    // 处理事件消息
                    streamReply = processEventMessageSync(messageDto, timestamp, nonce, config);
                    break;
                case "stream":
                    // 处理流式消息刷新请求
                    streamReply = processStreamRefreshMessage(messageDto, timestamp, nonce, config);
                    break;
                default:
                    log.warn("Unsupported message type: {}", messageDto.getMsgType());
                    streamReply = createTextStreamReply("暂不支持此类消息类型", timestamp, nonce, config);
                    break;
            }
            
            return streamReply;
            
        } catch (Exception e) {
            log.error("Failed to process WeChat robot message synchronously: msgId={}", messageDto.getMsgId(), e);
            return createTextStreamReply("处理消息时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }

    /**
     * 同步处理文本消息并返回流式回复 - 改造为支持企业微信真流式消息
     * 遵循企微流式消息刷新机制：先发送开始消息，然后等待刷新请求
     */
    public String processTextMessageSync(WechatBotConfig config, WechatRobotMessageDto messageDto, String timestamp, String nonce) {
        try {
            String content = "";
            if (messageDto.getText() != null) {
                content = messageDto.getText().getContent();
            } else if (messageDto.getMixed() != null) {
                content = messageDto.getMixed().getContent();
            } else if (messageDto.getQuote() != null) {
                content = messageDto.getQuote().getContent();
            }

            log.info("Processing text message synchronously from user {}: {}",
                    messageDto.getFrom().getUserId(), content);

            // 生成流式ID
            String streamId = UUID.randomUUID().toString().substring(0, 10);

            // 构建请求 DTO
            ChatBotReqDto chatBotReqDto = new ChatBotReqDto();
            chatBotReqDto.setAsk(content);
            chatBotReqDto.setUid(messageDto.getFrom().getUserId());
            // 使用消息中的chatId，如果没有则生成唯一ID
            Long chatId = messageDto.getChatId() != null ? 
                         Long.valueOf(messageDto.getChatId()) : 
                         Math.abs(messageDto.getFrom().getUserId().hashCode() % 1000000L) + 1000000L;
            chatBotReqDto.setChatId(chatId);
            chatBotReqDto.setBotId(Integer.valueOf(config.getAgentIdRef()));
            chatBotReqDto.setEdit(false);

            // 创建一个虚拟的 SseEmitter，不需要实际发送数据
            // WorkflowListener 会在完成时将结果存入静态映射
            SseEmitter listener = new SseEmitter(8 * 60 * 1000L);

            // 调用原来聊天接口
            botChatService.chatMessageBot(chatBotReqDto, listener, streamId, null, null);

            // 立即返回流式开始消息（符合企微要求）
            return createStreamReply(streamId, "🔍 正在为您处理请求...", false, timestamp, nonce, config);

        } catch (Exception e) {
            log.error("Failed to process text message synchronously", e);
            return createTextStreamReply("处理消息时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }
    
    /**
     * 处理流式消息刷新请求
     * 企业微信会在未收到结束前持续推送刷新请求
     */
    public String processStreamRefresh(String streamId, String timestamp, String nonce, WechatBotConfig config) {
        try {
            log.info("=== PROCESSING STREAM REFRESH FOR STREAM ID: {} ===", streamId);
            log.info("Processing stream refresh for streamId: {}, timestamp={}, nonce={}", streamId, timestamp, nonce);

            // 从 WorkflowListener 静态映射中获取结果
            String streamResult = WorkflowListener.getStreamResult(streamId);
            log.info("Stream result from WorkflowListener for streamId: {}, result: {}", streamId,
                    streamResult != null ? "found (length=" + streamResult.length() + ")" : "not found");

            if (streamResult != null && !streamResult.isEmpty()) {
                // 已有完整结果，返回给企业微信
                log.info("Returning complete content for streamId: {}, length: {}", streamId, streamResult.length());
                // 使用后返回并清理结果
                String removed = WorkflowListener.removeStreamResult(streamId);
                log.info("Removed stream result for streamId: {}, removed length: {}", streamId,
                        removed != null ? removed.length() : 0);
                return createStreamReply(streamId, streamResult, true, timestamp, nonce, config);
            }

            // 还在处理中，返回等待消息
            log.info("Stream still processing for streamId: {}", streamId);
            return createStreamReply(streamId, "⏳ 仍在处理中，请稍候...", false, timestamp, nonce, config);

        } catch (Exception e) {
            log.error("Failed to process stream refresh for streamId: {}", streamId, e);
            return createStreamReply(streamId, "❌ 刷新处理失败", true, timestamp, nonce, config);
        }
    }

    /**
     * 同步处理图片消息并返回流式回复todo
     */
    public String processImageMessageSync(WechatRobotMessageDto messageDto, String timestamp, String nonce, WechatBotConfig config) {
        try {
            String fileName = "未知图片";
            if (messageDto.getImage() != null) {
                fileName = messageDto.getImage().getFileName();
            }
            
            log.info("Processing image message synchronously: fileName={}", fileName);
            
            String streamId = UUID.randomUUID().toString().substring(0, 10);
            
            // 提供真实的图片处理反馈
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("感谢您分享图片：").append(fileName).append("\n\n");
            responseBuilder.append("我已成功接收到您发送的图片文件。\n");
            responseBuilder.append("目前我主要专注于文本对话交流，\n");
            responseBuilder.append("如果您有关于这张图片的问题或需要讨论图片相关内容，\n");
            responseBuilder.append("我很乐意与您进行交流！\n\n");
            responseBuilder.append("您也可以继续发送其他类型的消息，我会尽力为您提供帮助。");
            
            return createStreamReply(streamId, responseBuilder.toString(), true, timestamp, nonce, config);
            
        } catch (Exception e) {
            log.error("Failed to process image message synchronously", e);
            return createTextStreamReply("处理图片消息时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }

    /**
     * 同步处理语音消息并返回流式回复todo
     */
    public String processVoiceMessageSync(WechatRobotMessageDto messageDto, String timestamp, String nonce, WechatBotConfig config) {
        try {
            String duration = "未知时长";
            if (messageDto.getVoice() != null) {
                duration = messageDto.getVoice().getDuration() + "秒";
            }
            
            log.info("Processing voice message synchronously: duration={}", duration);
            
            String streamId = UUID.randomUUID().toString().substring(0, 10);
            
            // 提供真实的语音处理反馈
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("感谢您发送语音消息（时长：").append(duration).append("）\n\n");
            responseBuilder.append("我已成功接收到您的语音消息。\n");
            responseBuilder.append("目前我主要通过文本方式进行交流，\n");
            responseBuilder.append("如果您有任何问题或需要帮助，\n");
            responseBuilder.append("可以直接发送文字消息给我，我会及时回复您。\n\n");
            responseBuilder.append("期待与您的文字交流！");
            
            return createStreamReply(streamId, responseBuilder.toString(), true, timestamp, nonce, config);
            
        } catch (Exception e) {
            log.error("Failed to process voice message synchronously", e);
            return createTextStreamReply("处理语音消息时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }
    
    /**
     * 同步处理文件消息并返回流式回复todo
     */
    public String processFileMessageSync(WechatRobotMessageDto messageDto, String timestamp, String nonce, WechatBotConfig config) {
        try {
            String fileName = "未知文件";
            if (messageDto.getFile() != null) {
                fileName = messageDto.getFile().getFileName();
            }
            
            log.info("Processing file message synchronously: fileName={}", fileName);
            
            String streamId = UUID.randomUUID().toString().substring(0, 10);
            
            // 提供真实的文件处理反馈
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("感谢您分享文件：").append(fileName).append("\n\n");
            responseBuilder.append("我已成功接收到您发送的文件。\n");
            responseBuilder.append("目前我主要专注于文本对话交流，\n");
            responseBuilder.append("如果您有关于这个文件的问题或需要讨论文件相关内容，\n");
            responseBuilder.append("我很乐意与您进行交流！\n\n");
            responseBuilder.append("您也可以继续发送其他消息，我会尽力为您提供帮助。\n");
            responseBuilder.append("如有具体问题，请随时告诉我。\n");
            
            return createStreamReply(streamId, responseBuilder.toString(), true, timestamp, nonce, config);
            
        } catch (Exception e) {
            log.error("Failed to process file message synchronously", e);
            return createTextStreamReply("处理文件消息时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }

    /**
     * 处理流式消息刷新请求
     * 根据streamId从存储中获取对应的流式消息
     */
    public String processStreamRefreshMessage(WechatRobotMessageDto messageDto, String timestamp, String nonce, WechatBotConfig config) {
        try {
            log.info("Processing stream refresh message: streamId={}", 
                    messageDto.getStreamRefresh() != null ? messageDto.getStreamRefresh().getStreamId() : "unknown");
            
            String streamId = messageDto.getStreamRefresh() != null ? 
                             messageDto.getStreamRefresh().getStreamId() : null;
            
            if (streamId == null || streamId.isEmpty()) {
                log.warn("Invalid stream refresh message: missing streamId");
                return createTextStreamReply("无效的刷新请求", timestamp, nonce, config);
            }
            
            // 调用已有的刷新处理逻辑
            return processStreamRefresh(streamId, timestamp, nonce, config);
            
        } catch (Exception e) {
            log.error("Failed to process stream refresh message", e);
            return createTextStreamReply("处理刷新请求时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }
    
    /**
     * 处理事件消息todo
     */
    public String processEventMessageSync(WechatRobotMessageDto messageDto, String timestamp, String nonce, WechatBotConfig config) {
        try {
            log.info("Processing event message synchronously: eventType={}", 
                    messageDto.getEvent() != null ? messageDto.getEvent().getEventType() : "unknown");
            
            String response = "收到了事件消息";
            if (messageDto.getEvent() != null) {
                response = "收到了事件：" + messageDto.getEvent().getEventType();
            }
            
            String streamId = UUID.randomUUID().toString().substring(0, 10);
            return createStreamReply(streamId, response, true, timestamp, nonce, config);
            
        } catch (Exception e) {
            log.error("Failed to process event message synchronously", e);
            return createTextStreamReply("处理事件消息时发生错误：" + e.getMessage(), timestamp, nonce, config);
        }
    }
    
    /**
     * 创建文本流式回复
     */
    private String createTextStreamReply(String content, String timestamp, String nonce, WechatBotConfig config) {
        try {
            String streamId = UUID.randomUUID().toString().substring(0, 10);
            return createStreamReply(streamId, content, true, timestamp, nonce, config);
        } catch (Exception e) {
            log.error("Failed to create text stream reply", e);
            return "";
        }
    }
    
    /**
     * 创建流式回复消息
     */
    private String createStreamReply(String streamId, String content, boolean finish, String timestamp, String nonce, WechatBotConfig config) {
        try {
            // 构造流式消息JSON
            JSONObject streamJson = new JSONObject();
            streamJson.put("msgtype", "stream");
            
            JSONObject streamObj = new JSONObject();
            streamObj.put("id", streamId);
            streamObj.put("finish", finish);
            streamObj.put("content", content);
            
            streamJson.put("stream", streamObj);
            
            String replyJson = streamJson.toJSONString();
            log.debug("Stream reply JSON: {}", replyJson);
            
            // 加密回复消息
            WXBizJsonMsgCrypt pc = new WXBizJsonMsgCrypt(config.getToken(), config.getEncodingAesKey(), "");
            String encryptedReply = pc.EncryptMsg(replyJson, timestamp, nonce);
            
            log.debug("Encrypted stream reply: {}", encryptedReply);
            return encryptedReply;

        } catch (Exception e) {
            log.error("Failed to create stream reply", e);
            return "";
        }
    }


}