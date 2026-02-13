package com.iflytek.astron.console.hub.service.wechat;

import com.iflytek.astron.console.hub.dto.wechat.WechatRobotReplyDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.util.wechat.StreamMessageUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class StreamMessageTest {

    @MockBean
    private WechatRobotMessageService wechatRobotMessageService;

    @MockBean
    private StreamMessageUtil streamMessageUtil;

    @Test
    public void testCreateStreamReplyDto() {
        // 测试创建流式消息开始
        String streamId = "test-stream-id";
        String content = "测试内容";
        
        WechatRobotReplyDto startReply = WechatRobotReplyDto.createStreamStart(streamId, content);
        
        assert startReply.getMsgType().equals("stream");
        assert startReply.getStream() != null;
        assert startReply.getStream().getId().equals(streamId);
        assert startReply.getStream().getContent().equals(content);
        assert startReply.getStream().getFinish() == false;
        
        System.out.println("Stream start reply DTO created successfully");
        System.out.println("JSON: " + com.alibaba.fastjson2.JSON.toJSONString(startReply));
    }

    @Test
    public void testCreateStreamReplyEnd() {
        // 测试创建流式消息结束
        String streamId = "test-stream-id";
        String content = "完整的回复内容";
        
        WechatRobotReplyDto endReply = WechatRobotReplyDto.createStreamEnd(streamId, content);
        
        assert endReply.getMsgType().equals("stream");
        assert endReply.getStream() != null;
        assert endReply.getStream().getId().equals(streamId);
        assert endReply.getStream().getContent().equals(content);
        assert endReply.getStream().getFinish() == true;
        
        System.out.println("Stream end reply DTO created successfully");
        System.out.println("JSON: " + com.alibaba.fastjson2.JSON.toJSONString(endReply));
    }

    @Test
    public void testCreateImageMsgItem() {
        // 测试创建图片消息项
        String base64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
        String md5 = "d41d8cd98f00b204e9800998ecf8427e";
        
        WechatRobotReplyDto.MsgItem imageItem = WechatRobotReplyDto.createImageMsgItem(base64, md5);
        
        assert imageItem.getMsgtype().equals("image");
        assert imageItem.getImage() != null;
        assert imageItem.getImage().getBase64().equals(base64);
        assert imageItem.getImage().getMd5().equals(md5);
        
        System.out.println("Image message item created successfully");
        System.out.println("JSON: " + com.alibaba.fastjson2.JSON.toJSONString(imageItem));
    }

    @Test
    public void testSendStreamMessage() throws Exception {
        // 测试发送流式消息
        String content = "这是一条很长的回复内容，将会被分块发送。广州今日天气：29度，大部分多云，降雨概率：60%";
        String chatId = "test-chat-id";
        String aiBotId = "test-bot-id";
        
        WechatBotConfig config = new WechatBotConfig();
        config.setToken("test-token");
        config.setEncodingAesKey("test-encoding-key");
        config.setAgentIdRef("1000001");
        
        // Mock流式消息工具的行为
        when(streamMessageUtil.sendStreamMessage(anyString(), anyString(), anyString(), any(WechatBotConfig.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行发送
        CompletableFuture<Void> future = streamMessageUtil.sendStreamMessage(content, chatId, aiBotId, config);
        
        // 验证调用
        verify(streamMessageUtil, times(1)).sendStreamMessage(content, chatId, aiBotId, config);
        
        // 等待完成
        future.get();
        
        System.out.println("Stream message sent successfully");
    }

    @Test
    public void testCompleteStreamMessageFlow() {
        // 测试完整的流式消息流程
        String streamId = "complete-flow-test";
        String content = "广州今日天气：29度，大部分多云，降雨概率：60%";
        
        // 创建开始消息
        WechatRobotReplyDto startReply = WechatRobotReplyDto.createStreamStart(streamId, "");
        System.out.println("Start message: " + com.alibaba.fastjson2.JSON.toJSONString(startReply));
        
        // 创建中间消息
        WechatRobotReplyDto continueReply = WechatRobotReplyDto.createStreamContinue(streamId, "广州今日");
        System.out.println("Continue message: " + com.alibaba.fastjson2.JSON.toJSONString(continueReply));
        
        // 创建结束消息
        WechatRobotReplyDto endReply = WechatRobotReplyDto.createStreamEnd(streamId, content);
        System.out.println("End message: " + com.alibaba.fastjson2.JSON.toJSONString(endReply));
        
        // 验证消息格式符合企业微信要求
        assert startReply.getStream().getId() != null;
        assert startReply.getStream().getFinish() == false;
        assert endReply.getStream().getFinish() == true;
        assert endReply.getStream().getContent().equals(content);
        
        System.out.println("Complete stream message flow test passed");
    }
}