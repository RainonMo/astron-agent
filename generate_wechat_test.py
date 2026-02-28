#!/usr/bin/env python
# coding=utf-8
# 企业微信消息测试数据生成器

import sys
import os
import json
import random
import string
import time
import hashlib

# 添加 demo_server.py 所在的目录到路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'aibot_demo_python3'))

from WXBizJsonMsgCrypt import WXBizJsonMsgCrypt

# 配置信息
TOKEN = "QDG6eK"
ENCODING_AES_KEY = "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C"
BOT_KEY = "200aa5d88d79439c8d0a40273a867f61"
RECEIVE_ID = ""  # 智能机器人使用空字符串

# 输出文件
OUTPUT_FILE = "wechat_test_data1.txt"


def generate_random_string(length):
    """生成随机字符串"""
    letters = string.ascii_letters + string.digits
    return ''.join(random.choice(letters) for _ in range(length))


def create_original_message(content):
    """创建原始消息"""
    original_msg = {
    "msgid": "123",
    "aibotid": "1",
    "chatid": "2",
    "from": {
        "userid": "64a3fbf2-4a68-41c2-ad30-a64eb1a61bee"
    },
    "msgtype": "stream",
    "stream": {
        "id": "e3078575-1"
    }
}
    original_msg1 =  {
    "msgid": "123456",
    "aibotid": "12",
    "chatid": "13",
    "chattype": "single",
    "from": {
        "userid": "64a3fbf2-4a68-41c2-ad30-a64eb1a61bee"
    },
    "response_url": "RESPONSEURL",
    "msgtype": "text",
    "text": {
        "content": "我问的是我的名字是什么"
    }
}
    return json.dumps(original_msg, ensure_ascii=False)


def generate_signature(token, timestamp, nonce, encrypt):
    """生成 SHA1 签名"""
    params = sorted([token, timestamp, nonce, encrypt])
    sign_str = ''.join(params)
    signature = hashlib.sha1(sign_str.encode('utf-8')).hexdigest()
    return signature


def main():
    # 1. 生成测试参数
    timestamp = str(int(time.time()))
    nonce = generate_random_string(10)

    # 2. 创建要发送的消息（用户输入"你好"）
    original_msg = create_original_message("你好")

    # 3. 加密消息
    wxcpt = WXBizJsonMsgCrypt(TOKEN, ENCODING_AES_KEY, RECEIVE_ID)
    ret, encrypted_msg = wxcpt.EncryptMsg(original_msg, timestamp, nonce)

    if ret != 0:
        print(f"\nEncryption failed with error code: {ret}")
        return

    # 4. 解析加密后的消息获取签名和加密内容
    encrypted_data = json.loads(encrypted_msg)
    encrypt = encrypted_data['encrypt']
    msg_signature = encrypted_data['msgsignature']

    # 5. 构造 JSON 格式的 POST 请求数据
    post_data = json.dumps(encrypted_data, ensure_ascii=False)

    # 6. 构造 curl 命令
    post_data_escaped = post_data.replace("'", "'\\''")
    curl_command = f"""curl -X POST "http://localhost/api/wechat-bot/callback/{BOT_KEY}?msg_signature={msg_signature}&timestamp={timestamp}&nonce={nonce}" \\
  -H "Content-Type: application/json" \\
  -d '{post_data_escaped}'"""

    # 7. 验证解密
    ret, decrypted_msg = wxcpt.DecryptMsg(post_data, msg_signature, timestamp, nonce)

    # 8. 输出到文件
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write("=== WeChat Message Test Data Generator ===\n\n")
        f.write(f"Original message: {original_msg}\n")
        f.write(f"\nEncrypted message: {encrypted_msg}\n")
        f.write(f"\nMessage signature: {msg_signature}\n")
        f.write(f"Encrypted content: {encrypt}\n")
        f.write("\n=== Complete curl command ===\n")
        f.write(curl_command + "\n")
        f.write("\n=== POST request data (copy this directly) ===\n")
        f.write(post_data + "\n")
        f.write("\n=== Verification: Decryption ===\n")
        if ret == 0:
            f.write(f"Decrypted message: {decrypted_msg}\n")
            f.write("Decryption successful!\n")
        else:
            f.write(f"Decryption failed with error code: {ret}\n")

    print(f"Test data generated successfully!")
    print(f"Saved to: {OUTPUT_FILE}")
    print("\n=== Generated curl command (copy this to test) ===\n")
    print(curl_command)
    print(f"\n=== POST request data ===\n{post_data}")


if __name__ == "__main__":
    main()
