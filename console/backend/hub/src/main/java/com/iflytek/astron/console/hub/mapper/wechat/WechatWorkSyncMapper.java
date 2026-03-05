package com.iflytek.astron.console.hub.mapper.wechat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.astron.console.hub.entity.wechat.WechatWorkSync;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WechatWorkSyncMapper extends BaseMapper<WechatWorkSync> {

    /**
     * 批量插入或更新
     */
    int batchInsertOrUpdate(@Param("list") List<WechatWorkSync> list);

    /**
     * 根据同步批次号删除
     */
    int deleteBySyncBatchId(@Param("syncBatchId") String syncBatchId);
}
