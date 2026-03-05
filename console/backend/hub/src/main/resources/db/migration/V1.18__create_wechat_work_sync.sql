-- Migration script for wechat_work_sync table

DROP TABLE IF EXISTS `wechat_work_sync`;
CREATE TABLE `wechat_work_sync`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `userid`        varchar(128) NOT NULL COMMENT '企业微信用户ID',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `enterprise_id` bigint DEFAULT NULL COMMENT '企业ID',
    `department`    bigint       DEFAULT NULL COMMENT '用户所属部门ID（取第一个部门）',
    `sync_batch_id` varchar(64)  DEFAULT NULL COMMENT '同步批次号',
    `sync_status`   tinyint      DEFAULT '0' COMMENT '同步状态: 0-待处理, 1-成功, 2-失败',
    `error_msg`     varchar(512) DEFAULT NULL COMMENT '错误信息',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `userid_uni_key` (`userid`) USING BTREE,
    KEY             `sync_batch_id_key` (`sync_batch_id`) USING BTREE,
    KEY             `sync_status_key` (`sync_status`) USING BTREE,
    KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业微信用户同步表';
