package com.iflytek.astron.console.hub.mapper.wechat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WechatWorkConfigMapper extends BaseMapper<WechatWorkConfig> {

    /**
     * 根据企业ID获取配置
     */
    WechatWorkConfig selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);
}
