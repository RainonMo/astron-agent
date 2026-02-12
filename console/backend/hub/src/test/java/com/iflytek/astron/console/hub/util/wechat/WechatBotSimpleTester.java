package com.iflytek.astron.console.hub.util.wechat;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;

public class WechatBotSimpleTester {

    private static final String CORP_ID = "wx5823bf96d3bd56c7";
    private static final String TOKEN = "QDG6eK";
    private static final String ENCODING_AES_KEY = "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C";
    private static final String CALLBACK_URL = "http://localhost/api/wechat-bot/callback/453990fd541a40d6a95797330c76f8c3";

    // 超时配置（毫秒）
    private static final int CONNECT_TIMEOUT = 120000;   // 2分钟连接超时
    private static final int SOCKET_TIMEOUT = 1800000;  // 30分钟Socket超时
    private static final int REQUEST_TIMEOUT = 1800000; // 30分钟请求超时

    public static void main(String[] args) {
        System.out.println("=== 企业微信机器人测试开始 ===");
        System.out.println("时间: " + new java.util.Date());

        try {
            // 测试不同类型的消息
            testMessageTypes();

        } catch (Exception e) {
            System.err.println("测试执行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("=== 测试结束 ===");
        }
    }

    /**
     * 测试多种消息类型
     */
    private static void testMessageTypes() throws Exception {
        String[] testMessages = {
                "你好"
        };

        for (int i = 0; i < testMessages.length; i++) {
            System.out.println("\n--- 测试消息 " + (i + 1) + "/" + testMessages.length + " ---");
            sendTextMessage(testMessages[i]);

            // 在消息间添加间隔
            if (i < testMessages.length - 1) {
                System.out.println("等待2秒...");
                Thread.sleep(2000);
            }
        }
    }

    public static void sendTextMessage(String content) throws Exception {
        long startTime = System.currentTimeMillis();
        System.out.println("发送消息: '" + content + "'");
        System.out.println("开始时间: " + new java.util.Date());

        try {
            // 生成随机字符串和时间戳
            String nonce = generateRandomStr();
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

            // 构建明文XML消息
            String plainXml = buildPlainXml(content, CORP_ID);
            System.out.println("明文XML长度: " + plainXml.length());

            // 加密消息
            String encryptedMsg = encrypt(plainXml, ENCODING_AES_KEY, CORP_ID);
            System.out.println("加密消息长度: " + encryptedMsg.length());

            // 生成签名
            String signature = generateSignature(TOKEN, timestamp, nonce, encryptedMsg);

            // 构建最终请求XML
            String requestXml = buildRequestXml(encryptedMsg, signature, timestamp, nonce);
            System.out.println("请求XML长度: " + requestXml.length());

            // 发送POST请求
            // 构建带参数的URL
            String urlWithParams = CALLBACK_URL +
                    "?msg_signature=" + URLEncoder.encode(signature, "UTF-8") +
                    "&timestamp=" + timestamp +
                    "&nonce=" + URLEncoder.encode(nonce, "UTF-8");

            System.out.println("请求URL: " + urlWithParams);

            long requestStartTime = System.currentTimeMillis();
            String response = sendPostRequest(urlWithParams, requestXml);
            long requestEndTime = System.currentTimeMillis();

            System.out.println("请求耗时: " + (requestEndTime - requestStartTime) + "ms");
            System.out.println("原始响应长度: " + (response != null ? response.length() : 0));

            if (response != null && !response.trim().isEmpty()) {
                System.out.println("原始响应预览: " +
                        (response.length() > 200 ? response.substring(0, 200) + "..." : response));
            } else {
                System.out.println("警告: 收到空响应");
            }

            // 解密响应
            String decryptedResponse = decryptResponse(response, timestamp, nonce);
            System.out.println("解密后的响应: " + decryptedResponse);

        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            System.out.println("总耗时: " + (endTime - startTime) + "ms");
        }
    }

    private static String decryptResponse(String responseXml, String timestamp, String nonce) throws Exception {
        if (responseXml == null || responseXml.trim().isEmpty()) {
            return "响应为空";
        }

        try {
            // 解析响应XML，提取加密消息
            String encryptedMsg = extractEncryptedMsg(responseXml);
            if (encryptedMsg == null) {
                return "响应中没有加密消息";
            }

            // 解密消息
            String decryptedMsg = decrypt(encryptedMsg, ENCODING_AES_KEY, CORP_ID);

            // 验证签名（如果需要）
            String signature = generateSignature(TOKEN, timestamp, nonce, encryptedMsg);

            return decryptedMsg;
        } catch (Exception e) {
            return "解密失败: " + e.getMessage() + "\n原始响应: " + responseXml;
        }
    }

    private static String extractEncryptedMsg(String xml) throws Exception {
        try {
            // 使用XPath提取Encrypt节点内容
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // 尝试获取Encrypt节点
            String encrypt = xpath.evaluate("/xml/Encrypt", doc);
            if (encrypt != null && !encrypt.trim().isEmpty()) {
                return encrypt.trim();
            }

            // 如果格式不同，尝试其他可能的位置
            encrypt = xpath.evaluate("//Encrypt", doc);
            if (encrypt != null && !encrypt.trim().isEmpty()) {
                return encrypt.trim();
            }

            return null;
        } catch (Exception e) {
            // 尝试简单的字符串提取
            int start = xml.indexOf("<Encrypt><![CDATA[");
            if (start != -1) {
                start += 18; // 跳过"<Encrypt><![CDATA["
                int end = xml.indexOf("]]></Encrypt>", start);
                if (end != -1) {
                    return xml.substring(start, end);
                }
            }

            start = xml.indexOf("<Encrypt>");
            if (start != -1) {
                start += 9; // 跳过"<Encrypt>"
                int end = xml.indexOf("</Encrypt>", start);
                if (end != -1) {
                    return xml.substring(start, end);
                }
            }

            return null;
        }
    }

    // 解密方法
    public static String decrypt(String encryptedText, String encodingAesKey, String corpId) throws Exception {
        try {
            // Base64解码AES Key
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");

            // Base64解码加密文本
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            // AES解密
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(aesKey, 0, 16);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // 去除PKCS#7填充
            decryptedBytes = pkcs7Decode(decryptedBytes);

            // 提取有效内容
            ByteArrayOutputStream stream = new ByteArrayOutputStream(decryptedBytes.length);
            stream.write(decryptedBytes, 0, decryptedBytes.length);
            byte[] bytes = stream.toByteArray();

            // 跳过16位随机字符串
            int pos = 16;

            // 读取4位消息长度
            int msgLength = bytesToInt(bytes, pos);
            pos += 4;

            // 读取消息内容
            byte[] msgBytes = Arrays.copyOfRange(bytes, pos, pos + msgLength);
            String msg = new String(msgBytes, StandardCharsets.UTF_8);

            pos += msgLength;

            // 读取企业ID（可选验证）
            String receiveId = new String(Arrays.copyOfRange(bytes, pos, bytes.length), StandardCharsets.UTF_8);

            // 验证企业ID（可选）
            if (!corpId.equals(receiveId)) {
                System.out.println("警告: 企业ID不匹配, 期望: " + corpId + ", 实际: " + receiveId);
            }

            return msg;
        } catch (Exception e) {
            throw new Exception("解密失败: " + e.getMessage(), e);
        }
    }

    private static byte[] pkcs7Decode(byte[] src) {
        int padding = src[src.length - 1];
        if (padding < 1 || padding > 32) {
            return src;
        }
        return Arrays.copyOfRange(src, 0, src.length - padding);
    }

    private static int bytesToInt(byte[] bytes, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            value += (bytes[offset + i] & 0xFF) << shift;
        }
        return value;
    }

    private static String buildPlainXml(String content, String corpId) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("xml");
        doc.appendChild(root);

        Element toUserName = doc.createElement("ToUserName");
        toUserName.appendChild(doc.createCDATASection(corpId));
        root.appendChild(toUserName);

        Element fromUserName = doc.createElement("FromUserName");
        fromUserName.appendChild(doc.createCDATASection("64a3fbf2-4a68-41c2-ad30-a64eb1a61bss"));
        root.appendChild(fromUserName);

        Element createTime = doc.createElement("CreateTime");
        createTime.setTextContent(String.valueOf(System.currentTimeMillis() / 1000));
        root.appendChild(createTime);

        Element msgType = doc.createElement("MsgType");
        msgType.appendChild(doc.createCDATASection("text"));
        root.appendChild(msgType);

        Element contentElem = doc.createElement("Content");
        contentElem.appendChild(doc.createCDATASection(content));
        root.appendChild(contentElem);

        Element msgId = doc.createElement("MsgId");
        msgId.setTextContent(String.valueOf(new Random().nextLong()));
        root.appendChild(msgId);

        Element agentId = doc.createElement("AgentID");
        agentId.setTextContent("1000002");
        root.appendChild(agentId);

        return documentToString(doc);
    }

    private static String buildRequestXml(String encryptedMsg, String signature,
                                          String timestamp, String nonce) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("xml");
        doc.appendChild(root);

        Element encrypt = doc.createElement("Encrypt");
        encrypt.appendChild(doc.createCDATASection(encryptedMsg));
        root.appendChild(encrypt);

        Element msgSignature = doc.createElement("MsgSignature");
        msgSignature.appendChild(doc.createCDATASection(signature));
        root.appendChild(msgSignature);

        Element timeStamp = doc.createElement("TimeStamp");
        timeStamp.setTextContent(timestamp);
        root.appendChild(timeStamp);

        Element nonceElem = doc.createElement("Nonce");
        nonceElem.appendChild(doc.createCDATASection(nonce));
        root.appendChild(nonceElem);

        return documentToString(doc);
    }

    private static String encrypt(String plainText, String encodingAesKey, String corpId) throws Exception {
        // Base64解码AES Key
        byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");

        // 生成16位随机字符串
        String randomStr = generateRandomStr(16);

        // 构建待加密字符串: randomStr + networkBytesOrder + text + corpId
        byte[] randomStrBytes = randomStr.getBytes(StandardCharsets.UTF_8);
        byte[] textBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] corpIdBytes = corpId.getBytes(StandardCharsets.UTF_8);

        // 网络字节序: 文本长度（4字节）
        byte[] networkBytesOrder = intToBytes(textBytes.length);

        // 拼接待加密数据
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(randomStrBytes);
        stream.write(networkBytesOrder);
        stream.write(textBytes);
        stream.write(corpIdBytes);

        // PKCS#7填充
        byte[] bytes = pkcs7Encode(stream.toByteArray());

        // AES加密
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(aesKey, 0, 16);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(bytes);

        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String generateSignature(String token, String timestamp,
                                            String nonce, String encryptedMsg) throws Exception {
        String[] arr = new String[]{token, timestamp, nonce, encryptedMsg};
        Arrays.sort(arr);

        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private static String sendPostRequest(String url, String xmlData) throws Exception {
        // 配置请求超时参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            System.out.println("创建HTTP客户端，超时设置: 连接=" + CONNECT_TIMEOUT + "ms, Socket=" + SOCKET_TIMEOUT + "ms");

            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            httpPost.setHeader("Content-Type", "application/xml; charset=utf-8");
            httpPost.setHeader("User-Agent", "WechatBotTester/1.0");
            httpPost.setEntity(new StringEntity(xmlData, StandardCharsets.UTF_8));

            System.out.println("发送POST请求，数据长度: " + xmlData.length() + " 字符");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                System.out.println("HTTP状态码: " + statusCode);

                if (statusCode != 200) {
                    System.err.println("HTTP错误: " + response.getStatusLine().getReasonPhrase());
                }

                HttpEntity entity = response.getEntity();
                String result = entity != null ? EntityUtils.toString(entity, "UTF-8") : "";
                System.out.println("响应头信息:");
                Arrays.stream(response.getAllHeaders())
                        .forEach(header -> System.out.println("  " + header.getName() + ": " + header.getValue()));
                return result;
            }
        }
    }

    // 辅助方法
    private static String generateRandomStr() {
        return generateRandomStr(16);
    }

    private static String generateRandomStr(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    private static byte[] pkcs7Encode(byte[] src) {
        int blockSize = 32;
        int padding = blockSize - (src.length % blockSize);
        byte[] padBytes = new byte[padding];
        Arrays.fill(padBytes, (byte) padding);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(src, 0, src.length);
        stream.write(padBytes, 0, padBytes.length);
        return stream.toByteArray();
    }
}