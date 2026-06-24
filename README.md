# 云梦商城 (yunmeng-mall)

> **云梦商城** — 基于 Spring Cloud 微服务架构的电商后端系统

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue)](https://spring.io/projects/spring-cloud)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-red)](https://baomidou.com/)

---

## 📋 项目简介

云梦商城是一款采用 **Spring Cloud 微服务架构** 的全功能电商后端系统。系统涵盖了用户管理、商品管理、订单支付、优惠券、评论点赞、AI 智能助手等核心电商功能模块，适用于中小型电商平台的快速搭建和二次开发。

### 核心特性

- 🔐 **统一认证鉴权**：基于 JWT 的认证体系 + Spring Cloud Gateway 网关鉴权
- 🏪 **完整的商品体系**：分类管理、商品 CRUD、SKU 管理、品牌管理、规格模板
- 🛒 **购物车与订单**：购物车管理、订单创建/支付/取消流程
- 📢 **营销模块**：优惠券创建/发放/领取/兑换完整流程
- 💬 **互动功能**：商品评论/回复、点赞、收藏、浏览历史、店铺关注
- 🤖 **AI 智能助手**：基于 DeepSeek 大模型的智能对话与商品智能审核
- ⏰ **分布式定时任务**：基于 XXL-JOB 的订单超时取消、商品审核等
- 🖼️ **OSS 文件存储**：阿里云 OSS 集成，支持商品图片、店铺头像上传
- 📊 **商品搜索**：基于 Elasticsearch 的商品全文搜索

---

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                        前端 (UniApp)                          │
│                    http://localhost:8080                      │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                    ym-gateway (网关 :8080)                     │
│              Spring Cloud Gateway + CORS + JWT               │
│              StripPrefix=1 路由规则                            │
└────┬──────┬──────┬──────┬──────┬──────┬──────┬──────────────┘
     │      │      │      │      │      │      │
     ▼      ▼      ▼      ▼      ▼      ▼      ▼
┌─────────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ ym-user │ │ym-item│ │ym-pay│ │ym-   │ │ym-remark │ │yunmeng-ai│ │ym-auth   │
│ 用户服务│ │商品服务│ │支付服│ │promotion│ │评论服务  │ │AI服务    │ │认证服务  │
│ /us/**  │ │/it/** │ │/ca/**│ │优惠券 │ │/rm/**    │ │/ai/**    │ │          │
│         │ │       │ │/py/**│ │/pm/**  │ │          │ │          │ │          │
└────┬────┘ └───┬───┘ └──┬───┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────────┘
     │          │        │         │             │            │
     └──────────┴────────┴─────────┴─────────────┴────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   Nacos 注册中心   │
                    │   配置中心         │
                    └───────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐  ┌────────────┐  ┌────────────┐
        │  MySQL   │  │   Redis    │  │ RabbitMQ   │
        └──────────┘  └────────────┘  └────────────┘
              │               │
              ▼               ▼
        ┌──────────┐  ┌────────────┐
        │Elasticsearch│ │ Aliyun OSS │
        └──────────┘  └────────────┘
```

---

## 📦 模块说明

### 1. ym-gateway — 网关服务
- **端口**: `8080`
- **职责**: 统一入口、路由转发（StripPrefix=1）、CORS 跨域、JWT 鉴权过滤
- **路由规则**:

| 前缀 | 服务 | 说明 |
|------|------|------|
| `/us/**` | ym-user | 用户服务 |
| `/it/**` | ym-item | 商品服务 |
| `/ca/**` | ym-pay | 购物车 |
| `/py/**` | ym-pay | 订单/支付 |
| `/pm/**` | ym-promotion | 优惠券 |
| `/rm/**` | ym-remark | 评论 |
| `/ai/**` | yunmeng-ai | AI 助手 |

### 2. ym-auth — 认证服务
- **职责**: JWT 令牌签发与认证授权
- **技术**: jjwt 0.11.5

### 3. ym-user — 用户服务
- **端口**: `8081`
- **核心功能**:
  - 用户注册/登录（手机号+密码/验证码）
  - 登录态管理（JWT Token）
  - 收货地址 CRUD（默认地址设置）
  - 个人资料管理（昵称、头像、性别）
  - 商家注册（角色升级）
  - 管理员后台（用户管理、角色管理）
  - 系统通知
- **核心类**:
  - `UserController` — 用户登录/注册/资料
  - `UserAddressController` — 收货地址 CRUD
  - `UserProfileController` — 个人资料
  - `AdminController` — 管理员后台
  - `SystemNoticeController` — 系统通知

### 4. ym-item — 商品服务
- **端口**: `8082`
- **核心功能**:
  - 📂 **分类管理**：支持多级分类树，管理员 CRUD
  - 📦 **商品管理**：商品 CRUD、上下架、批量定时审核（AI + 人工）
  - 📋 **SKU 管理**：多规格 SKU 管理（价格、库存、图片）
  - 🏷️ **品牌管理**：品牌 CRUD
  - 📐 **规格模板**：规格名+规格值模板管理
  - 🏪 **商家管理**：注册入驻、分步设置（logo/描述/头像）、店铺关注
  - ⭐ **商品收藏**：收藏/取消收藏/查询收藏列表
  - 🕒 **浏览历史**：记录/查询/清空浏览历史
  - 📊 **商品搜索**：Elasticsearch 全文搜索同步
  - 🔄 **AI 自动审核**：定时任务调用 yunmeng-ai 审核商品文字
- **核心类**:
  - `ItemController` — 商品列表/详情查询
  - `MerchantItemController` — 商家端商品管理
  - `ItemAdminController` — 管理员商品管理
  - `CategoryController` / `CategoryAdminController` — 分类查询/管理
  - `ShopController` / `ShopAdminController` — 商家注册/管理
  - `FavoriteController` — 商品收藏
  - `HistoryController` — 浏览历史
  - `ShopFollowController` — 店铺关注
  - `BrandController` — 品牌管理
  - `SpecTemplateController` / `SpecValueController` — 规格管理
  - `AiAutoReviewJob` — AI 自动审核定时任务
  - `ItemSyncJobHandler` — 商品同步 ES 任务
  - `OssUploadService` — OSS 图片上传

### 5. ym-pay — 支付/订单/购物车服务
- **核心功能**:
  - 🛒 **购物车**：添加/修改数量/删除/列表
  - 📝 **订单**：创建订单、订单列表、订单详情、取消订单、状态流转
  - 📋 **订单项**：查询订单内商品明细
  - 💰 **支付**：支付单创建、支付记录查询（占位实现）
  - ⏰ **XXL-JOB 定时任务**：订单超时自动取消
- **核心类**:
  - `CartController` — 购物车 CRUD
  - `OrderController` — 订单创建/查询/取消
  - `OrderItemController` — 订单商品列表
  - `PayController` — 支付管理
  - `OrderJobHandler` — 订单超时处理

### 6. ym-promotion — 优惠券服务
- **核心功能**:
  - 🎫 **优惠券模板**：创建/更新/删除/查询优惠券
  - 📢 **发放管理**：开始/暂停发放
  - 📥 **用户领取**：用户主动领取优惠券
  - 🔢 **兑换码**：兑换码生成/查询/兑换优惠券
  - 🎯 **使用范围**：优惠券适用范围管理
- **核心类**:
  - `CouponController` — 优惠券 CRUD
  - `UserCouponController` — 用户领取/查询
  - `ExchangeCodeController` — 兑换码管理
  - `CouponJobHandler` — 优惠券定时任务

### 7. ym-remark — 评论与点赞服务
- **核心功能**:
  - 💬 **评论**：发表评论/回复（支持商品评论 + 店铺评论）
  - 👍 **点赞**：评论点赞/取消点赞/查询点赞状态
  - 🔄 **定时同步**：点赞数同步到数据库
- **核心类**:
  - `CommentController` — 评论发表/查询
  - `LikedRecordController` — 点赞管理
  - `LikedTimesSyncJobHandler` — 点赞数同步任务

### 8. yunmeng-ai — AI 助手服务
- **核心功能**:
  - 🤖 **智能对话**：基于 DeepSeek 大模型的多轮对话
  - 🔍 **意图识别**：自动解析用户意图（查订单/查商品/售后等）
  - 📋 **AI 商品审核**：AI 辅助审核商品文字内容（敏感词/违规检测）
  - 💬 **会话管理**：支持多会话、对话历史
- **核心类**:
  - `AiChatController` — AI 对话接口
  - `AiReviewController` — AI 审核接口
  - `AiChatService` — 对话服务
  - `DeepSeekService` — DeepSeek API 调用
  - `IntentParser` — 意图解析器
  - `AiReviewService` — 审核服务
  - `ReplyBuilder` — 回复构建器

### 9. ym-api — Feign 接口定义模块
- **职责**: 定义跨微服务调用的 Feign 接口和 DTO 传输对象
- **核心文件**:
  - `CartFeign` — 购物车 Feign
  - `ItemFeign` — 商品 Feign
  - `OrderFeign` — 订单 Feign
  - `PromotionFeign` — 优惠券 Feign
  - `ShopFeign` — 商家 Feign
  - `UserFeign` — 用户 Feign

### 10. ym-common — 公共模块
- **职责**: 提供全局通用能力
- **核心内容**:
  - `UserContext` — 用户上下文（ThreadLocal 传递）
  - `UserInfoInterceptor` — 用户信息拦截器
  - `GlobalExceptionHandler` — 全局异常处理
  - `BizException` — 业务异常
  - `ResultCode` — 统一返回码枚举
  - `CommonAutoConfiguration` — Spring Boot 自动配置
  - 工具类集合（ArrayUtils、BeanUtils、BooleanUtils 等）

---

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.2.5 |
| 微服务 | Spring Cloud | 2023.0.1 |
| 微服务 | Spring Cloud Alibaba | 2023.0.1.0 |
| 网关 | Spring Cloud Gateway | — |
| 注册/配置中心 | Nacos | — |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | — |
| 缓存 | Redis + Redisson | 3.25.0 |
| 消息队列 | RabbitMQ | — |
| 定时任务 | XXL-JOB | 2.4.1 |
| 搜索引擎 | Elasticsearch | — |
| 对象存储 | Aliyun OSS | 3.17.4 |
| 认证 | jjwt (JWT) | 0.11.5 |
| API 文档 | Knife4j + springdoc | 4.4.0 / 2.3.0 |
| 服务调用 | OpenFeign + LoadBalancer | — |
| AI 模型 | DeepSeek | — |
| JSON | Fastjson2 | 2.0.25 |
| 日志 | Logstash Logback | 7.4 |

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x
- RabbitMQ 3.x
- XXL-JOB 2.4.x
- Elasticsearch 7.x+（可选，用于商品搜索）

### 本地运行

```bash
# 1. 克隆仓库
git clone https://github.com/liyunxiang041012-max/yunmeng-mall.git
cd yunmeng-mall

# 2. 编译打包
mvn clean install -DskipTests

# 3. 按顺序启动服务
# ① Nacos（需预先启动）
# ② 基础设施（MySQL / Redis / RabbitMQ / XXL-JOB）

# ③ 启动各微服务
cd ym-gateway && mvn spring-boot:run  # 网关 :8080
cd ym-user && mvn spring-boot:run     # 用户 :8081
cd ym-item && mvn spring-boot:run     # 商品 :8082
cd ym-pay && mvn spring-boot:run      # 支付/订单
cd ym-promotion && mvn spring-boot:run # 优惠券
cd ym-remark && mvn spring-boot:run   # 评论
cd yunmeng-ai && mvn spring-boot:run  # AI
cd ym-auth && mvn spring-boot:run     # 认证
```

### 网关路由表

所有请求统一通过 `http://localhost:8080` 进入，前缀自动 StripPrefix=1 后转发：

```
/us/**  →  ym-user       用户登录/注册/地址/资料
/it/**  →  ym-item       商品/分类/商家/收藏/历史
/ca/**  →  ym-pay        购物车
/py/**  →  ym-pay        订单/支付
/pm/**  →  ym-promotion  优惠券
/rm/**  →  ym-remark     评论/点赞
/ai/**  →  yunmeng-ai    AI助手
```

### 鉴权说明

- **白名单**（无需登录）：
  - `POST /us/user/register` 用户注册
  - `POST /us/user/login` 用户登录
  - `POST /us/user/sendCode` 发送验证码
  - `POST /us/shop/register` 商家注册
- 其他接口需携带 `Authorization: Bearer <token>`
- 统一响应格式：

```json
{
  "code": 200,
  "msg": "ok",
  "data": { ... }
}
```

---

## 📖 核心业务流

### 用户注册/登录流程

```
用户端 → /us/user/sendCode (发送验证码)
      → /us/user/register (注册)
      → /us/user/login (登录，获取JWT Token)
      → 携带Token访问其他接口
```

### 商品管理流程

```
商家/管理员 → 创建分类 → 上传图片(OSS) → 创建商品(选择分类/SKU)
           → AI自动审核(文字内容) + 人工审核(图片)
           → 上架 → ES同步 → 前端展示
                                    ↓
                              用户浏览/搜索
```

### 下单支付流程

```
用户 → 添加购物车 → 创建订单 → 在线支付(占位)
                                  ↓
                              订单超时检测(XXL-JOB) → 自动取消
```

---

## 📁 项目目录结构

```
yunmeng-mall/
├── ym-gateway/          # 网关服务 (Spring Cloud Gateway)
├── ym-auth/             # 认证服务 (JWT)
├── ym-user/             # 用户服务 (注册/登录/地址/资料/管理)
├── ym-item/             # 商品服务 (商品/分类/商家/收藏/ES同步)
├── ym-pay/              # 支付服务 (购物车/订单/支付)
├── ym-promotion/        # 优惠券服务 (创建/发放/领取/兑换)
├── ym-remark/           # 评论服务 (评论/回复/点赞)
├── yunmeng-ai/          # AI助手 (DeepSeek对话/智能审核)
├── ym-api/              # Feign接口定义 + DTO传输对象
├── ym-common/           # 公共模块 (工具类/异常/拦截器)
├── uploads/             # 上传文件目录
├── API接口文档.md        # 完整 API 接口文档
├── pom.xml              # 父 POM (依赖管理)
└── README.md            # 本文件
```

---

## 📚 相关文档

| 文档 | 说明 |
|------|------|
| [API接口文档.md](./API接口文档.md) | 完整 API 接口清单 |
| [商家后台-后端接口文档-最终版.md](./商家后台-后端接口文档-最终版.md) | 商家后台接口 |
| [管理员后台-完整接口文档.md](./管理员后台-完整接口文档.md) | 管理员后台接口 |
| [订单接口文档.md](./订单接口文档.md) | 订单相关接口 |
| [优惠券与评论服务接口文档.md](./优惠券与评论服务接口文档.md) | 优惠券+评论接口 |
| [AI辅助商品审核-前端对接文档.md](./AI辅助商品审核-前端对接文档.md) | AI审核对接 |
| [前端对接说明-AI助手.md](./前端对接说明-AI助手.md) | AI助手对接 |
| [商品审核流程-前端对接文档.md](./商品审核流程-前端对接文档.md) | 审核流程 |

---

## 🔧 配置说明

### 核心中间件连接配置（通过 Nacos 配置中心管理）

- **MySQL**: 各服务独立数据库，配置在 Nacos 或本地 `application.yaml` 中的 `spring.datasource`
- **Redis**: `spring.data.redis`
- **Nacos**: `spring.cloud.nacos.discovery.server-addr`
- **RabbitMQ**: `spring.rabbitmq`
- **XXL-JOB**: `xxl.job.admin.addresses` + `xxl.job.executor`
- **Elasticsearch**: `spring.elasticsearch`
- **DeepSeek API**: `yunmeng-ai` 模块 `DeepSeekConfig`
- **Aliyun OSS**: `oss.endpoint` / `oss.access-key-id` / `oss.access-key-secret`

### 文件上传限制

- 单文件最大: 5MB
- 请求最大: 10MB
- 配置文件: `spring.servlet.multipart.max-file-size` / `max-request-size`

---

## 🧑‍💻 开发规范

- **分层架构**: Controller → Service → Mapper (MyBatis-Plus)
- **统一响应**: 所有接口返回 `Result<T>` 结构
- **用户上下文**: 使用 `UserContext.getUserId()` 获取当前登录用户
- **微服务调用**: 通过 Feign 接口（ym-api 模块定义）
- **角色校验**: 商家/管理员操作需校验 role 字段
- **枚举映射**: MyBatis-Plus 使用 `@EnumValue` 注解

---

## 🌐 GitHub

🔗 [https://github.com/liyunxiang041012-max/yunmeng-mall](https://github.com/liyunxiang041012-max/yunmeng-mall)
