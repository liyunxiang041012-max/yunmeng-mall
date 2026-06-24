/*
 收藏与关注功能 SQL 脚本
 包含：商品收藏表、店铺关注表
*/

-- 切换到 item 数据库
USE ym_item;

-- 1. 创建收藏表
CREATE TABLE `favorite` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `user_id`     bigint NOT NULL COMMENT '用户ID',
  `item_id`     bigint NOT NULL COMMENT '商品ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_item_id` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏';

-- 2. 创建店铺关注表
CREATE TABLE `shop_follow` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `user_id`     bigint NOT NULL COMMENT '用户ID',
  `shop_id`     bigint NOT NULL COMMENT '店铺ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_shop` (`user_id`, `shop_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注店铺';

-- ----------------------------
-- 测试数据
-- ----------------------------

-- 插入测试收藏数据（用户ID: 1, 商品ID: 1, 2, 3）
INSERT INTO `favorite` (`user_id`, `item_id`) VALUES (1, 1);
INSERT INTO `favorite` (`user_id`, `item_id`) VALUES (1, 2);
INSERT INTO `favorite` (`user_id`, `item_id`) VALUES (1, 3);

-- 插入测试店铺关注数据（用户ID: 1, 店铺ID: 1, 2）
INSERT INTO `shop_follow` (`user_id`, `shop_id`) VALUES (1, 1);
INSERT INTO `shop_follow` (`user_id`, `shop_id`) VALUES (1, 2);

-- 注意：以上测试数据需要根据实际的数据库中的数据来调整
-- 执行前请确认用户ID和商品ID/店铺ID是否存在
