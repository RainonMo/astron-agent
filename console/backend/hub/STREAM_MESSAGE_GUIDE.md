# 企业微信智能机器人流式消息功能说明

## 功能概述

本功能实现了企业微信智能机器人的标准流式消息回复，完全符合[企业微信官方文档](https://developer.work.weixin.qq.com/document/path/101031)的要求。

## 主要特性

1. **标准流式消息格式** - 完全遵循企业微信官方规范
2. **自动分块发送** - 支持大文本内容的分块流式传输
3. **图文混排支持** - 支持在消息结束时发送图片内容
4. **异步处理** - 不阻塞主线程，提升响应速度
5. **错误处理** - 完善的异常处理机制

## 核心组件

### 1. WechatRobotReplyDto (流式消息数据传输对象)

支持完整的流式消息格式：

```json
{
  "msgtype": "stream",
  "stream": {
    "id": "STREAMID",
    "finish": false,
    "content": "广州今日天气：29度，大部分多云，降雨概率：60%",
    "msg_item": [
      {
        "msgtype": "image",
        "image": {
          "base64": "BASE64",
          "md5": "MD5"
        }
      }
    ],
    "feedback": {
      "id": "FEEDBACKID"
    }
  }
}
```

### 2. StreamMessageUtil (流式消息工具类)

提供便捷的流式消息发送方法：

```java
// 发送简单流式消息
streamMessageUtil.sendStreamMessage(content, chatId, aiBotId, config);

// 发送带图片的流式消息
List<MsgItem> images = Arrays.asList(
    streamMessageUtil.createImageMsgItem(base64Data, md5Hash)
);
streamMessageUtil.sendStreamMessageWithImages(content, chatId, aiBotId, config, images, feedbackId);
```

### 3. 自动消息流程

1. **开始消息** - 发送空内容的开始信号
2. **中间片段** - 分块发送实际内容（可选）
3. **结束消息** - 发送完整内容和可能的图片/反馈

## 使用示例

### 基础文本流式回复

```java
@RestController
@RequestMapping("/api/wechat-bot")
@RequiredArgsConstructor
public class WechatBotController {
    
    private final WechatBotService wechatBotService;
    private final StreamMessageUtil streamMessageUtil;
    
    @PostMapping("/callback/{botKey}")
    public String handleCallback(@PathVariable String botKey,
                               @RequestParam String msg_signature,
                               @RequestParam String timestamp,
                               @RequestParam String nonce,
                               @RequestBody String postData) {
        try {
            // 处理消息并返回成功响应
            return wechatBotService.handleMessage(botKey, msg_signature, timestamp, nonce, postData);
        } catch (Exception e) {
            log.error("处理微信回调失败", e);
            return "fail";
        }
    }
}
```

### 手动发送流式消息

```java
// 在任何服务中手动发送流式消息
public void sendCustomStreamMessage(String chatId, String aiBotId, String content) {
    WechatBotConfig config = wechatBotService.getByBotKey("your-bot-key");
    
    // 异步发送流式消息
    CompletableFuture<Void> future = streamMessageUtil.sendStreamMessage(
        content, 
        chatId, 
        aiBotId, 
        config
    );
    
    // 可以选择等待完成或直接返回
    future.thenRun(() -> log.info("流式消息发送完成"));
}
```

### 带图片的流式消息

```java
public void sendStreamMessageWithImage(String chatId, String aiBotId, String content, 
                                     String imageBase64, String imageMd5) {
    WechatBotConfig config = wechatBotService.getByBotKey("your-bot-key");
    
    // 创建图片消息项
    List<WechatRobotReplyDto.MsgItem> images = Arrays.asList(
        streamMessageUtil.createImageMsgItem(imageBase64, imageMd5)
    );
    
    // 发送带图片的流式消息
    streamMessageUtil.sendStreamMessageWithImages(
        content, 
        chatId, 
        aiBotId, 
        config, 
        images, 
        "feedback-123"
    );
}
```

## 配置要求

确保机器人配置包含以下必要字段：

```java
WechatBotConfig config = new WechatBotConfig();
config.setToken("your-token");           // 回调Token
config.setEncodingAesKey("your-aes-key"); // 消息加解密密钥
config.setAgentIdRef("agent-id");        // 关联的智能体ID
```

## 注意事项

1. **消息大小限制** - 单条流式消息内容不超过20480字节
2. **图片限制** - 图片base64编码前不超过10M，支持JPG/PNG格式
3. **并发控制** - 同一用户与机器人的并发消息数限制为3条
4. **超时机制** - 企业微信等待回复超时时间为6分钟
5. **加密要求** - 所有回复消息必须使用WXBizMsgCrypt进行加密

## 测试验证

运行测试用例验证功能：

```bash
mvn test -Dtest=StreamMessageTest
```

测试涵盖：
- DTO对象创建和序列化
- 流式消息各阶段创建
- 图片消息项创建
- 完整的消息流程测试

## 错误处理

系统包含完善的错误处理机制：
- 网络异常重试
- 消息加密失败处理
- 异步任务异常捕获
- 日志记录和监控

## 性能优化

- 异步非阻塞处理
- 连接池复用
- 消息批处理
- 内存优化管理

通过以上实现，您的企业微信智能机器人现在完全支持标准的流式消息回复功能，能够提供更好的用户体验和更丰富的交互方式。