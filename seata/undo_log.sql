-- ===========================================
-- Seata AT 模式 undo_log 表
-- 在 ym_pay / ym_item / ym_promotion 三个库分别执行
-- 用 Navicat 分别连每个库跑，或者取消注释下面 USE 语句
-- ===========================================

-- USE ym_pay;
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `branch_id` bigint NOT NULL COMMENT 'branch transaction id',
  `xid` varchar(128) NOT NULL COMMENT 'global transaction id',
  `context` varchar(128) NOT NULL COMMENT 'undo_log context,such as serialization',
  `rollback_info` longblob NOT NULL COMMENT 'rollback info',
  `log_status` int NOT NULL COMMENT '0:normal status,1:defense status',
  `log_created` datetime(6) NOT NULL COMMENT 'create datetime',
  `log_modified` datetime(6) NOT NULL COMMENT 'modify datetime',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';

-- ===========================================
-- 下一个库：取消注释下面两行并注释上面
-- ===========================================
-- USE ym_item;
-- DROP TABLE IF EXISTS `undo_log`;
-- CREATE TABLE `undo_log` (
--   `branch_id` bigint NOT NULL,
--   `xid` varchar(128) NOT NULL,
--   `context` varchar(128) NOT NULL,
--   `rollback_info` longblob NOT NULL,
--   `log_status` int NOT NULL,
--   `log_created` datetime(6) NOT NULL,
--   `log_modified` datetime(6) NOT NULL,
--   UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- USE ym_promotion;
-- DROP TABLE IF EXISTS `undo_log`;
-- CREATE TABLE `undo_log` (
--   `branch_id` bigint NOT NULL,
--   `xid` varchar(128) NOT NULL,
--   `context` varchar(128) NOT NULL,
--   `rollback_info` longblob NOT NULL,
--   `log_status` int NOT NULL,
--   `log_created` datetime(6) NOT NULL,
--   `log_modified` datetime(6) NOT NULL,
--   UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
