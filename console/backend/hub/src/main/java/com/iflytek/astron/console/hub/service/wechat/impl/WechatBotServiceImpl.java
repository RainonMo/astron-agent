package com.iflytek.astron.console.hub.service.wechat.impl;

import cn.hutool.core.text.UnicodeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.astron.console.commons.exception.BusinessException;
import com.iflytek.astron.console.commons.constant.ResponseEnum;
import com.iflytek.astron.console.hub.dto.wechat.WechatBotConfigDto;
import com.iflytek.astron.console.hub.dto.wechat.WechatRobotMessageDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.mapper.WechatBotConfigMapper;
import com.iflytek.astron.console.hub.service.wechat.WechatBotService;
import com.iflytek.astron.console.hub.service.wechat.WechatRobotMessageService;
import com.iflytek.astron.console.hub.util.wechat.AesException;
import com.iflytek.astron.console.hub.util.wechat.WXBizMsgCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 企业微信机器人服务实现类
 *
 * @author Lingma
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatBotServiceImpl extends ServiceImpl<WechatBotConfigMapper, WechatBotConfig> implements WechatBotService {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    private final WechatRobotMessageService wechatRobotMessageService;

    @Override
    public IPage<WechatBotConfig> page(Page<WechatBotConfig> page, String keyword) {
        LambdaQueryWrapper<WechatBotConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(WechatBotConfig::getBotKey, keyword)
                   .or()
                   .like(WechatBotConfig::getCorpId, keyword)
                   .or()
                   .like(WechatBotConfig::getAgentId, keyword);
        }
        wrapper.orderByDesc(WechatBotConfig::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public List<WechatBotConfig> getAllActiveConfigs() {
        LambdaQueryWrapper<WechatBotConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WechatBotConfig::getIsActive, true);
        return this.list(wrapper);
    }

    @Override
    public WechatBotConfig getByBotKey(String botKey) {
        LambdaQueryWrapper<WechatBotConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WechatBotConfig::getBotKey, botKey);
        return this.getOne(wrapper);
    }

    @Override
    public boolean create(WechatBotConfigDto dto) {
        // 检查botKey是否已存在
        if (this.getByBotKey(dto.getBotKey()) != null) {
            throw new BusinessException(ResponseEnum.PARAMS_ERROR, "机器人key已存在");
        }

        WechatBotConfig config = new WechatBotConfig();
        BeanUtils.copyProperties(dto, config);
        
        // 生成唯一的botKey（如果未提供）
        if (!StringUtils.hasText(config.getBotKey())) {
            config.setBotKey(UUID.randomUUID().toString().replace("-", ""));
        }
        
        // 设置默认状态
        if (config.getIsActive() == null) {
            config.setIsActive(true);
        }
        
        // 生成回调URL
        config.setCallbackUrl(generateCallbackUrl(config.getBotKey()));
        
        return this.save(config);
    }

    @Override
    public boolean update(WechatBotConfigDto dto) {
        WechatBotConfig config = this.getById(dto.getId());
        if (config == null) {
            throw new BusinessException(ResponseEnum.DATA_NOT_FOUND, "机器人配置不存在");
        }

        // 检查botKey是否被其他记录使用
        WechatBotConfig existingConfig = this.getByBotKey(dto.getBotKey());
        if (existingConfig != null && !existingConfig.getId().equals(dto.getId())) {
            throw new BusinessException(ResponseEnum.PARAMS_ERROR, "机器人key已被其他记录使用");
        }

        BeanUtils.copyProperties(dto, config);
        config.setCallbackUrl(generateCallbackUrl(config.getBotKey()));
        
        return this.updateById(config);
    }

    @Override
    public boolean delete(Long id) {
        return this.removeById(id);
    }

    @Override
    public String generateCallbackUrl(String botKey) {
        String baseUrl = String.format("http://localhost:%s", contextPath);
        if (contextPath.equals("/")) {
            baseUrl = String.format("http://localhost");
        }
        return String.format("%s/api/wechat-bot/callback/%s", baseUrl, botKey);
    }

    /**
     * 验证URL有效性
     *
     * @param botKey
     * @param msgSignature 消息签名
     * @param timestamp    时间戳
     * @param nonce        随机数
     * @param echostr      加密的随机字符串
     * @return 解密后的明文echostr
     */
    @Override
    public String verifyUrl(String botKey, String msgSignature, String timestamp, String nonce, String echostr) throws AesException {
        log.info("Verifying WeChat robot URL: msg_signature={}, timestamp={}, nonce={}",
                msgSignature, timestamp, nonce);
        // 获取机器人配置
        WechatBotConfig config = this.getByBotKey(botKey);
        if (config == null || !config.getIsActive()) {
            log.error("机器人配置不存在或未启用: botKey={}", botKey);
            return "fail";
        }

        // 使用空字符串作为receiveId，符合智能机器人场景
        WXBizMsgCrypt pc = new WXBizMsgCrypt(config.getToken(), config.getEncodingAesKey(), "");

        // 验证URL并解密echostr
        String decryptedEchostr = pc.verifyUrl(msgSignature, timestamp, nonce, echostr);
        log.info("WeChat robot URL verification successful");

        return decryptedEchostr;
    }

    /**
     * 处理消息回调
     *
     * @param botKey
     * @param msgSignature 消息签名
     * @param timestamp    时间戳
     * @param nonce        随机数
     * @param postData     加密的消息体
     * @return 成功响应
     * @throws AesException 加解密异常
     */
    @Override
    public String handleMessage(String botKey, String msgSignature, String timestamp, String nonce, String postData) throws AesException {
        log.info("Handling WeChat robot message callback");

        // 获取机器人配置
        WechatBotConfig config = this.getByBotKey(botKey);
        if (config == null || !config.getIsActive()) {
            log.error("机器人配置不存在或未启用: botKey={}", botKey);
            return "fail";
        }

        // 清理postData
        if (postData.endsWith("\\n")) {
            postData = postData.substring(0, postData.length() - 2);
        }
        postData = UnicodeUtil.toString(postData);

        // 使用空字符串作为receiveId，符合智能机器人场景
        WXBizMsgCrypt pc = new WXBizMsgCrypt(config.getToken(), config.getEncodingAesKey(), "");

        // 解密消息
        String decryptedMessage = pc.decryptMsg(msgSignature, timestamp, nonce, postData);
        log.debug("Decrypted WeChat robot message: {}", decryptedMessage);

        // 解析并处理消息
        WechatRobotMessageDto messageDto = wechatRobotMessageService.parseMessage(decryptedMessage);

          // 异步处理消息并生成流式回复
        wechatRobotMessageService.processMessageAsync(config, messageDto);

        // 立即返回成功响应
        return "success";
    }

}