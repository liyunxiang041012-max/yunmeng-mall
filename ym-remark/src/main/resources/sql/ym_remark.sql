-- =============================================
-- 云梦商城 - 评论互动服务数据库
-- =============================================

-- ----------------------------
-- 1. 点赞记录表
-- ----------------------------
DROP TABLE IF EXISTS `liked_record`;
CREATE TABLE `liked_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `user_id` BIGINT NOT NULL COMMENT '用户id',
    `biz_id` BIGINT NOT NULL COMMENT '点赞的业务id',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '点赞的业务类型，如：comment、product等',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_biz` (`user_id`, `biz_id`, `biz_type`),
    KEY `idx_biz` (`biz_id`, `biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

-- ----------------------------
-- 2. 评论表
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL COMMENT '评论id，雪花算法生成',
    `user_id` BIGINT NOT NULL COMMENT '评论用户id',
    `biz_id` BIGINT NOT NULL COMMENT '评论目标业务id（如商品id）',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '评论业务类型，如：product、article',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论id，0表示为一级评论',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `liked_times` INT DEFAULT 0 COMMENT '点赞次数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数量',
    `status` INT DEFAULT 1 COMMENT '状态：1=正常，0=删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_id`, `biz_type`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- =============================================
-- 测试数据
-- =============================================

INSERT INTO `comment` (`id`, `user_id`, `biz_id`, `biz_type`, `parent_id`, `content`, `liked_times`, `reply_count`, `status`) VALUES
(5001, 1, 100, 'product', 0, '质量很好，面料舒服，推荐购买！', 12, 3, 1),
(5002, 2, 100, 'product', 0, '颜色比图片稍微深一点，但整体还不错', 5, 1, 1),
(5003, 3, 100, 'product', 0, '物流很快，第二天就到了', 8, 0, 1),
(5004, 4, 100, 'product', 5001, '谢谢支持！', 2, 0, 1),
(5005, 1, 100, 'product', 5001, '会的，这款性价比很高', 1, 0, 1),
(5006, 5, 100, 'product', 5001, '请问洗了会缩水吗？', 0, 0, 1),
(5007, 6, 100, 'product', 0, '买给老婆的，她很喜欢', 3, 0, 1),
(5008, 1, 100, 'product', 5002, '是的，实物确实偏深一点', 0, 0, 1);

INSERT INTO `liked_record` (`user_id`, `biz_id`, `biz_type`) VALUES
(1, 5001, 'comment'),
(2, 5001, 'comment'),
(3, 5001, 'comment'),
(1, 5002, 'comment'),
(3, 5002, 'comment'),
(1, 100, 'product');
