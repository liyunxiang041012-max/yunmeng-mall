-- 系统通知表
DROP TABLE IF EXISTS `system_notice`;
CREATE TABLE `system_notice` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title`       VARCHAR(100) NOT NULL              COMMENT '标题',
  `content`     TEXT         NOT NULL              COMMENT '通知详情',
  `admin_id`    BIGINT       NOT NULL              COMMENT '发送管理员ID',
  `target_role` TINYINT      DEFAULT -1            COMMENT '目标角色: -1=全部用户, 0=普通用户, 1=商家, 2=管理员',
  `deleted`     TINYINT      DEFAULT 0             COMMENT '0=正常 1=已删除',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知';

-- 用户已读记录表
DROP TABLE IF EXISTS `user_notice`;
CREATE TABLE `user_notice` (
  `id`        BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`   BIGINT   NOT NULL COMMENT '用户ID',
  `notice_id` BIGINT   NOT NULL COMMENT '通知ID',
  `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notice_id` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知已读记录';
