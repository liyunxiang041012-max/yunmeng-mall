-- 添加审核状态字段
ALTER TABLE `item` ADD COLUMN `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0=待审核, 1=审核通过, 2=审核驳回' AFTER `status`;

-- 现有数据全部标记为已通过（已有的 status=1 的数据）
UPDATE `item` SET `audit_status` = 1 WHERE `status` = 1;
UPDATE `item` SET `audit_status` = 1 WHERE `status` = 0 AND `deleted` = 0;
