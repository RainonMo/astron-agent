package com.iflytek.astron.console.hub;

import com.iflytek.astron.console.hub.util.wechat.WXBizMsgCrypt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * 企业微信智能机器人HTTP客户端测试示例
 * 演示如何通过HTTP请求测试接口
 *
 * @author Lingma
 */
public class WechatRobotHttpClientTest {

    private static final String BASE_URL = "http://localhost/api/wechat-bot/callback/453990fd541a40d6a95797330c76f8c3";
    private static final String TEST_TOKEN = "QDG6eK";
    private static final String TEST_ENCODING_AES_KEY = "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C";
    private static final String TEST_CORP_ID = "";

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("企业微信智能机器人HTTP客户端测试");
        System.out.println("===========================================\n");

        try {
            // 测试URL验证
//            testUrlVerification();
            
            // 测试消息发送
            testSendMessage();
            
        } catch (Exception e) {
            System.err.println("❌ HTTP测试出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试URL验证
     */
    private static void testUrlVerification() throws Exception {
        System.out.println("📍 测试URL验证 (GET请求)");
        System.out.println("-------------------------------------------");
        
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonce();
        String echostr = "http_client_test_" + System.currentTimeMillis();
        
        // 加密echostr
        WXBizMsgCrypt crypt = new WXBizMsgCrypt(TEST_TOKEN, TEST_ENCODING_AES_KEY, TEST_CORP_ID);
        String encryptedEchostr;
        encryptedEchostr = crypt.encrypt(getRandomStr(), echostr);

        // 计算签名
        String signature = calculateSignature(TEST_TOKEN, timestamp, nonce, encryptedEchostr);
        
        // 构造URL
        String url = BASE_URL + 
            "?msg_signature=" + URLEncoder.encode(signature, "UTF-8") +
            "&timestamp=" + timestamp +
            "&nonce=" + URLEncoder.encode(nonce, "UTF-8") +
            "&echostr=" + URLEncoder.encode(encryptedEchostr, "UTF-8");
        
        System.out.println("请求URL: " + url);
        
        // 发送GET请求
        String response = sendGetRequest(url);
        System.out.println("响应结果: " + response);
        System.out.println("验证是否正确: " + echostr.equals(response.trim()));
        System.out.println();
    }

    /**
     * 测试发送消息
     */
    private static void testSendMessage() throws Exception {
        System.out.println("📍 测试发送文本消息 (POST请求)");
        System.out.println("-------------------------------------------");
        
        // 准备消息数据
        String messageJson = "{\n" +
                "  \"msgid\": \"HTTP_CLIENT_TEST_001\",\n" +
                "  \"aibotid\": \"TEST_BOT_001\",\n" +
                "  \"chatid\": \"TEST_CHAT_001\",\n" +
                "  \"chattype\": \"single\",\n" +
                "  \"from\": {\n" +
                "    \"userid\": \"64a3fbf2-4a68-41c2-ad30-a64eb1a61bee\"\n" +
                "  },\n" +
                "  \"msgtype\": \"text\",\n" +
                "  \"text\": {\n" +
                "    \"content\": \"你好！\"\n" +
                "  }\n" +
                "}";
        
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonce();
        
        // 加密消息
        WXBizMsgCrypt crypt = new WXBizMsgCrypt(TEST_TOKEN, TEST_ENCODING_AES_KEY, TEST_CORP_ID);
        String encryptedMessage = crypt.encrypt(getRandomStr(), messageJson);
        
        // 构造XML
        String xmlData = "<xml>\n" +
                "  <Encrypt><![CDATA[" + encryptedMessage + "]]></Encrypt>\n" +
                "</xml>";
        
        // 计算签名
        String signature = calculateSignature(TEST_TOKEN, timestamp, nonce, encryptedMessage);
        
        // 构造URL
        String url = BASE_URL + 
            "?msg_signature=" + URLEncoder.encode(signature, "UTF-8") +
            "&timestamp=" + timestamp +
            "&nonce=" + URLEncoder.encode(nonce, "UTF-8");
        
        System.out.println("POST数据:");
        System.out.println(xmlData);
        System.out.println("请求URL: " + url);
        
        // 发送POST请求
        String response = sendPostRequest(url, xmlData);
        System.out.println("响应结果: " + response);
        System.out.println();
    }

    /**
     * 发送GET请求
     */
    private static String sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP状态码: " + responseCode);
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }

    /**
     * 发送POST请求
     */
    private static String sendPostRequest(String urlString, String postData) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        // 发送数据
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP状态码: " + responseCode);
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? 
                    conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }

    /**
     * 计算SHA1签名
     */
    private static String calculateSignature(String token, String timestamp, String nonce, String encrypt) {
        try {
            String[] array = new String[]{token, timestamp, nonce, encrypt};
            Arrays.sort(array);
            StringBuilder sb = new StringBuilder();
            for (String s : array) {
                sb.append(s);
            }
            String str = sb.toString();
            
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(str.getBytes("UTF-8"));
            byte[] digest = md.digest();

            StringBuilder hexStr = new StringBuilder();
            for (byte b : digest) {
                String shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexStr.append(0);
                }
                hexStr.append(shaHex);
            }
            return hexStr.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算签名失败", e);
        }
    }

    private static String getRandomStr() {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private static String generateNonce() {
        return String.valueOf(System.currentTimeMillis()) + new Random().nextInt(1000);
    }
}