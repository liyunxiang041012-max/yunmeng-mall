/*
 Navicat Premium Dump SQL

 Source Server Type    : MySQL
 Source Server Version : 90400 (9.4.0)
 Source Schema         : ym_pay

 Date: 22/05/2026
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Database: ym_pay
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `ym_pay` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ym_pay`;

-- ----------------------------
-- Table structure for cart
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID（关联 ym_item.item_sku）',
  `spu_id` bigint NOT NULL COMMENT '商品SPU ID（冗余，方便查同店商品）',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（结算时按店铺分组）',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称（冗余快照）',
  `image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品主图（冗余快照）',
  `spec_info` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '规格信息，如"颜色:红/尺码:XL"（冗余快照）',
  `price` bigint NOT NULL COMMENT '加入时单价，分为单位（冗余快照）',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `selected` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否选中 0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_sku`(`user_id` ASC, `sku_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_shop_id`(`shop_id` ASC) USING BTREE,
  INDEX `idx_user_shop`(`user_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_spu_id`(`spu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '购物车表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay
-- ----------------------------
DROP TABLE IF EXISTS `pay`;
CREATE TABLE `pay`  (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL               COMMENT '用户ID',
  `order_id`     VARCHAR(64)  NOT NULL               COMMENT '订单号',
  `pay_no`       VARCHAR(64)  NOT NULL               COMMENT '支付单号',
  `pay_channel`  VARCHAR(32)  DEFAULT NULL            COMMENT '支付渠道（ALIPAY/WECHAT）',
  `amount`       BIGINT       NOT NULL DEFAULT 0     COMMENT '支付金额，分为单位',
  `status`       TINYINT      NOT NULL DEFAULT 0     COMMENT '支付状态：0-待支付 1-已支付 2-已退款 3-已关闭',
  `pay_time`     DATETIME     DEFAULT NULL            COMMENT '支付时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no`   (`pay_no`),
  KEY        `idx_user_id` (`user_id`),
  KEY        `idx_order_id`(`order_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
