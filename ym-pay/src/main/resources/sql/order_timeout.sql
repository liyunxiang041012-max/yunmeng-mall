-- ============================================
-- 订单超时取消功能 - 数据库变更
-- 执行方式: 在 ym-pay 数据库执行
-- ============================================

-- 给 order 表添加 expire_time 字段
ALTER TABLE `order` ADD COLUMN `expire_time` DATETIME NULL COMMENT '订单过期时间（创建时间+30分钟）' AFTER `pay_time`;

-- 为超时扫描查询建索引（可选，提升定时任务性能）
ALTER TABLE `order` ADD INDEX `idx_status_create` (`status`, `create_time`);
