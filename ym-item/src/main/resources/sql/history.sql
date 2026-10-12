/*
 浏览历史表
*/

-- 切换到 item 数据库
USE ym_item;

-- 创建浏览历史表
CREATE TABLE `history` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `user_id`     bigint NOT NULL COMMENT '用户ID',
  `item_id`     bigint NOT NULL COMMENT '商品ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_item_id` (`item_id`),
  INDEX `idx_user_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览历史';
