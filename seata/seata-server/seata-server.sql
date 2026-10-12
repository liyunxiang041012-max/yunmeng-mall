-- ===========================================
-- Seata Server 数据库初始化（新建 seata 库）
-- 用于存储全局事务/分支事务/锁数据
-- ===========================================
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARSET utf8mb4;
USE seata;

-- 全局事务表
CREATE TABLE IF NOT EXISTS `global_table`
(
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT       NULL,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32)  NULL,
    `transaction_service_group` VARCHAR(32)  NULL,
    `transaction_name`          VARCHAR(128) NULL,
    `timeout`                   INT          NULL,
    `begin_time`                BIGINT       NULL,
    `application_data`          VARCHAR(2000) NULL,
    `gmt_create`                DATETIME     NULL,
    `gmt_modified`              DATETIME     NULL,
    PRIMARY KEY (`xid`),
    KEY `idx_status_gmt_modified` (`status`, `gmt_modified`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 分支事务表
CREATE TABLE IF NOT EXISTS `branch_table`
(
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT       NULL,
    `resource_group_id` VARCHAR(32)  NULL,
    `resource_id`       VARCHAR(256) NULL,
    `branch_type`       VARCHAR(8)   NULL,
    `status`            TINYINT      NULL,
    `client_id`         VARCHAR(64)  NULL,
    `application_data`  VARCHAR(2000) NULL,
    `gmt_create`        DATETIME(6)  NULL,
    `gmt_modified`      DATETIME(6)  NULL,
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 全局锁表
CREATE TABLE IF NOT EXISTS `lock_table`
(
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(128) NULL,
    `transaction_id` BIGINT       NULL,
    `branch_id`      BIGINT       NOT NULL,
    `resource_id`    VARCHAR(256) NULL,
    `table_name`     VARCHAR(32)  NULL,
    `pk`             VARCHAR(36)  NULL,
    `status`         TINYINT      NOT NULL DEFAULT '0' COMMENT '0:locked,1:rollbacking',
    `gmt_create`     DATETIME     NULL,
    `gmt_modified`   DATETIME     NULL,
    PRIMARY KEY (`row_key`),
    KEY `idx_status` (`status`),
    KEY `idx_branch_id` (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
