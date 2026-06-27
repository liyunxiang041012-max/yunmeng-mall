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
(1005, '新用户满50减15（兑换码）', 1, 4, 0, 1500, 5000, 0, 2, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 30, NULL, NULL, 3, 100, 0, 0, 1),
(1006, '618年中大促满300减50', 1, 4, 0, 5000, 30000, 0, 1, '2026-06-15 00:00:00', '2026-06-20 23:59:59', 0, '2026-06-15 00:00:00', '2026-07-15 23:59:59', 3, 2000, 0, 0, 1),
(1007, '双11全场9折（最高减100）', 1, 2, 0, 90, 0, 10000, 1, '2026-11-01 00:00:00', '2026-11-12 23:59:59', 0, '2026-11-01 00:00:00', '2026-11-30 23:59:59', 2, 10000, 0, 0, 3),
(1008, '新人专享无门槛20元券', 1, 3, 0, 2000, 0, 0, 1, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 7, NULL, NULL, 3, 500, 0, 0, 1),
(1009, '数码品类满500减80', 1, 4, 1, 8000, 50000, 0, 1, '2026-07-01 00:00:00', '2026-08-31 23:59:59', 0, '2026-07-01 00:00:00', '2026-08-31 23:59:59', 3, 1000, 0, 0, 2),
(1010, '每满100减15（上不封顶）', 1, 1, 0, 1500, 10000, 0, 1, '2026-05-15 00:00:00', '2026-10-31 23:59:59', 30, NULL, NULL, 3, 3000, 0, 0, 5),
(1011, '七夕限定满199减40', 1, 4, 0, 4000, 19900, 0, 1, '2026-08-20 00:00:00', '2026-08-27 23:59:59', 0, '2026-08-20 00:00:00', '2026-09-03 23:59:59', 2, 1500, 0, 0, 1),
(1012, '国庆大促满88减25', 1, 4, 0, 2500, 8800, 0, 1, '2026-09-28 00:00:00', '2026-10-08 23:59:59', 0, '2026-09-28 00:00:00', '2026-10-15 23:59:59', 2, 3000, 0, 0, 1),
(1013, '会员专属6折券（最高减100）', 1, 2, 0, 60, 0, 10000, 2, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 0, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 3, 200, 0, 0, 1),
(1014, '清仓专区满200减100', 1, 4, 1, 10000, 20000, 0, 1, '2026-06-15 00:00:00', '2026-07-15 23:59:59', 0, '2026-06-15 00:00:00', '2026-07-31 23:59:59', 3, 500, 0, 0, 2),
(1015, '周末闪购无门槛减5元', 1, 3, 0, 500, 0, 0, 1, '2026-06-01 00:00:00', '2026-09-30 23:59:59', 3, NULL, NULL, 3, 2000, 0, 0, 10),
-- 以下为已过期/未开始的券，用于测试状态展示
(1016, '五一劳动节满200减50', 1, 4, 0, 5000, 20000, 0, 1, '2026-04-28 00:00:00', '2026-05-05 23:59:59', 0, '2026-04-28 00:00:00', '2026-05-10 23:59:59', 4, 2000, 1500, 800, 1),
(1017, '元旦开门红全场8.5折', 1, 2, 0, 85, 0, 8000, 1, '2027-01-01 00:00:00', '2027-01-07 23:59:59', 0, '2027-01-01 00:00:00', '2027-01-15 23:59:59', 1, 5000, 0, 0, 3),
(1018, '春季焕新满300减60', 1, 4, 0, 6000, 30000, 0, 1, '2026-03-01 00:00:00', '2026-04-30 23:59:59', 0, '2026-03-01 00:00:00', '2026-05-31 23:59:59', 4, 3000, 2800, 2100, 1),
(1019, '直播专属满99减30', 1, 4, 0, 3000, 9900, 0, 2, '2026-07-01 00:00:00', '2026-12-31 23:59:59', 7, NULL, NULL, 3, 300, 0, 0, 1),
(1020, '学生特惠满50减20', 1, 4, 0, 2000, 5000, 0, 1, '2026-06-15 00:00:00', '2026-09-15 23:59:59', 15, NULL, NULL, 3, 1000, 0, 0, 1);

-- =============================================
-- 秒杀服务
-- =============================================

DROP TABLE IF EXISTS `flash_sale`;
CREATE TABLE `flash_sale` (
    `id` BIGINT NOT NULL COMMENT '秒杀活动ID',
    `name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` INT DEFAULT 1 COMMENT '状态：1=未开始,2=进行中,3=已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

DROP TABLE IF EXISTS `flash_sale_item`;
CREATE TABLE `flash_sale_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `flash_sale_id` BIGINT NOT NULL COMMENT '秒杀活动ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `spu_id` BIGINT NOT NULL COMMENT 'SPU ID',
    `flash_price` BIGINT NOT NULL COMMENT '秒杀价格（分）',
    `stock` INT NOT NULL COMMENT '秒杀库存',
    `sold` INT DEFAULT 0 COMMENT '已售数量',
    `limit_per_user` INT DEFAULT 1 COMMENT '每人限购',
    `sort` INT DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_flash_sale_id` (`flash_sale_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品';
