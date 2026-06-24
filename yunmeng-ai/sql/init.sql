-- ========================
-- 云梦 AI 服务数据库
-- 在 MySQL 中执行此 SQL
-- ========================

CREATE DATABASE IF NOT EXISTS `ym_ai`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `ym_ai`;

-- AI 服务目前对话历史存在 Redis，暂不需要表
-- 预留此库用于后续功能扩展（如用户偏好、AI 反馈记录等）
