-- Migration script for wechat_work_config table

DROP TABLE IF EXISTS `wechat_work_config`;
CREATE TABLE `wechat_work_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `enterprise_id` bigint       NOT NULL COMMENT '企业ID',
    `corpid`        varchar(128) NOT NULL COMMENT '企业微信企业ID',
    `corpsecret`    varchar(256) NOT NULL COMMENT '企业微信应用凭证密钥',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `enterprise_id_uni_key` (`enterprise_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业微信配置表';
