package com.iflytek.astron.console.hub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.astron.console.hub.entity.WechatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业微信消息记录Mapper接口
 *
 * @author Lingma
 */
@Mapper
public interface WechatMessageMapper extends BaseMapper<WechatMessage> {
}