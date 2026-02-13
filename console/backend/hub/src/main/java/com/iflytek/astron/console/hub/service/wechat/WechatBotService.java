package com.iflytek.astron.console.hub.service.wechat;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.astron.console.hub.dto.wechat.WechatBotConfigDto;
import com.iflytek.astron.console.hub.entity.WechatBotConfig;
import com.iflytek.astron.console.hub.util.wechat.AesException;

import java.util.List;

/**
 * 企业微信机器人服务接口
 *
 * @author Lingma
 */
public interface WechatBotService extends IService<WechatBotConfig> {

    /**
     * 分页查询机器人配置
     *
     * @param page 分页参数
     * @param keyword 搜索关键字
     * @return 分页结果
     */
    IPage<WechatBotConfig> page(Page<WechatBotConfig> page, String keyword);

    /**
     * 查询所有启用的机器人配置
     *
     * @return 启用的机器人配置列表
     */
    List<WechatBotConfig> getAllActiveConfigs();

    /**
     * 根据botKey查询机器人配置
     *
     * @param botKey 机器人key
     * @return 机器人配置
     */
    WechatBotConfig getByBotKey(String botKey);

    /**
     * 创建机器人配置
     *
     * @param dto 配置信息
     * @return 是否成功
     */
    boolean create(WechatBotConfigDto dto);

    /**
     * 更新机器人配置
     *
     * @param dto 配置信息
     * @return 是否成功
     */
    boolean update(WechatBotConfigDto dto);

    /**
     * 删除机器人配置
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 生成机器人回调URL
     *
     * @param botKey 机器人key
     * @return 回调URL
     */
    String generateCallbackUrl(String botKey);

    String verifyUrl(String botKey, String msgSignature, String timestamp, String nonce, String echostr) throws AesException;

    String handleMessage(String botKey, String msgSignature, String timestamp, String nonce, String postData) throws AesException;
}