package com.iflytek.astron.console.hub.entity.wechat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wechat_work_sync")
@Schema(name = "WechatWorkSync", description = "企业微信用户同步表")
public class WechatWorkSync {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "企业微信用户ID")
    private String userid;

    @Schema(description = "用户所属部门ID（取第一个部门）")
    private Long department;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "企业ID")
    private Long enterpriseId;

    @Schema(description = "同步批次号")
    private String syncBatchId;

    @Schema(description = "同步状态: 0-待处理, 1-成功, 2-失败")
    private Integer syncStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
