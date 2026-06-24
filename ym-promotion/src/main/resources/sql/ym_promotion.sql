-- =============================================
-- 云梦商城 - 优惠券促销服务数据库
-- =============================================

-- ----------------------------
-- 1. 优惠券规则表
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id` BIGINT NOT NULL COMMENT '优惠券id，雪花算法生成',
    `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `type` INT DEFAULT 1 COMMENT '优惠券类型，1：普通券',
    `discount_type` INT NOT NULL COMMENT '折扣类型：1=每满减,2=折扣,3=无门槛,4=满减',
    `specific` TINYINT(1) DEFAULT 0 COMMENT '是否限定作用范围，0=不限定，1=限定',
    `discount_value` INT DEFAULT NULL COMMENT '折扣值，满减金额或折扣率(80=8折)',
    `threshold_amount` INT DEFAULT 0 COMMENT '使用门槛，0=无门槛，其他为最低消费金额',
    `max_discount_amount` INT DEFAULT 0 COMMENT '最高优惠金额，0=不限制',
    `obtain_way` INT NOT NULL COMMENT '获取方式：1=手动领取，2=兑换码',
    `issue_begin_time` DATETIME DEFAULT NULL COMMENT '开始发放时间',
    `issue_end_time` DATETIME DEFAULT NULL COMMENT '结束发放时间',
    `term_days` INT DEFAULT 0 COMMENT '有效期天数，0=指定有效期',
    `term_begin_time` DATETIME DEFAULT NULL COMMENT '有效期开始时间',
    `term_end_time` DATETIME DEFAULT NULL COMMENT '有效期结束时间',
    `status` INT DEFAULT 1 COMMENT '状态：1=待发放,2=未开始,3=进行中,4=已结束,5=暂停',
    `total_num` INT DEFAULT 0 COMMENT '总数量，不超过5000',
    `issue_num` INT DEFAULT 0 COMMENT '已发行数量',
    `used_num` INT DEFAULT 0 COMMENT '已使用数量',
    `user_limit` INT DEFAULT 1 COMMENT '每人限领数量',
    `ext_param` VARCHAR(255) DEFAULT NULL COMMENT '拓展参数字段',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creater` BIGINT DEFAULT NULL COMMENT '创建人',
    `updater` BIGINT DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_issue_time` (`issue_begin_time`, `issue_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券规则信息';

-- ----------------------------
-- 2. 优惠券作用范围表
-- ----------------------------
DROP TABLE IF EXISTS `coupon_scope`;
CREATE TABLE `coupon_scope` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type` INT DEFAULT 1 COMMENT '范围类型：1=分类，2=商品',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券id',
    `biz_id` BIGINT NOT NULL COMMENT '作用范围的业务id',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券作用范围';

-- ----------------------------
-- 3. 兑换码表
-- ----------------------------
DROP TABLE IF EXISTS `exchange_code`;
CREATE TABLE `exchange_code` (
    `id` BIGINT NOT NULL COMMENT '兑换码序列号id',
    `code` VARCHAR(20) NOT NULL COMMENT 'Base32编码的兑换码（10位）',
    `status` INT DEFAULT 1 COMMENT '状态：1=待兑换，2=已兑换，3=已过期',
    `user_id` BIGINT DEFAULT NULL COMMENT '兑换人用户ID',
    `type` INT DEFAULT 1 COMMENT '兑换类型：1=优惠券',
    `exchange_target_id` BIGINT NOT NULL COMMENT '兑换目标id（如优惠券id）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expired_time` DATETIME DEFAULT NULL COMMENT '兑换码过期时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_exchange_target` (`exchange_target_id`),
    KEY `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换码表';

-- ----------------------------
-- 4. 用户优惠券表
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
    `id` BIGINT NOT NULL COMMENT '用户券id，雪花算法生成',
    `user_id` BIGINT NOT NULL COMMENT '用户id',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券模板id',
    `term_begin_time` DATETIME DEFAULT NULL COMMENT '有效期开始时间',
    `term_end_time` DATETIME DEFAULT NULL COMMENT '有效期结束时间',
    `used_time` DATETIME DEFAULT NULL COMMENT '使用时间（核销时间）',
    `status` INT DEFAULT 1 COMMENT '状态：1=未使用，2=已使用，3=已过期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_coupon_id` (`coupon_id`),
    KEY `idx_user_coupon_status` (`user_id`, `coupon_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领取优惠券记录';

-- =============================================
-- 测试数据
-- =============================================

INSERT INTO `coupon` (`id`, `name`, `type`, `discount_type`, `specific`, `discount_value`, `threshold_amount`, `max_discount_amount`, `obtain_way`, `issue_begin_time`, `issue_end_time`, `term_days`, `term_begin_time`, `term_end_time`, `status`, `total_num`, `issue_num`, `used_num`, `user_limit`) VALUES
(1001, '满100减20', 1, 4, 0, 2000, 10000, 0, 1, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 30, NULL, NULL, 3, 5000, 0, 0, 1),
(1002, '8折优惠券（最高减50）', 1, 2, 0, 80, 0, 5000, 1, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 15, NULL, NULL, 3, 5000, 0, 0, 2),
(1003, '无门槛减10元', 1, 3, 0, 1000, 0, 0, 1, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 7, NULL, NULL, 3, 3000, 0, 0, 5),
(1004, '每满200减30', 1, 1, 0, 3000, 20000, 0, 1, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 30, NULL, NULL, 3, 5000, 0, 0, 3),
(1005, '新用户满50减15（兑换码）', 1, 4, 0, 1500, 5000, 0, 2, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 30, NULL, NULL, 3, 100, 0, 0, 1);
