-- Item 表添加 AI 审核结果字段
ALTER TABLE `item` ADD COLUMN `ai_review` TEXT NULL COMMENT 'AI审核结果' AFTER `audit_status`;
