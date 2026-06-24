# 云梦商城 — AI 智能特色与定时任务总结

> **yunmeng-ai 模块** 是云梦商城区别于常规电商项目的核心亮点，集成 **DeepSeek 大模型**，实现了 AI 智能客服 + AI 商品审核双重能力。同时系统基于 **XXL-JOB** 实现了完整的分布式定时任务体系。

---

## 一、AI 智能客服 (yunmeng-ai)

### 1.1 架构概览

```
用户消息 → IntentParser(意图识别) → 业务数据获取(Feign) 
        → 对话历史加载(Redis) → DeepSeek API(大模型生成)
        → 降级到 ReplyBuilder(规则模板) ← DeepSeek失败时
```

### 1.2 意图识别（IntentParser）

**位置**: `yunmeng-ai/.../service/IntentParser.java`

基于**关键词规则匹配**（轻量、零成本），从用户自然语言中识别 6 种意图：

| 意图 | 枚举值 | 触发关键词 | 说明 |
|------|--------|-----------|------|
| 查订单 | `ORDER` | 订单、买了什么、物流、发货、快递、包裹 | 自动调用订单 Feign 拉取数据 |
| 查优惠券 | `COUPON` | 优惠券、券、折扣、满减、红包 | 自动调用优惠券 Feign |
| 查购物车 | `CART` | 购物车、加了什么、结算、cart | 自动调用购物车 Feign |
| 商品推荐 | `RECOMMEND` | 推荐、帮我找、想买、搜、有什么好 | 调用商品搜索接口 |
| 查店铺 | `SHOP` | 店铺、商家、品牌、店家 | 引导浏览店铺 |
| 闲聊 | `CHAT` | 你好、谢谢、其他 | 兜底闲聊回复 |

### 1.3 业务数据集成

AI 对话不只是"聊天"，而是**真正理解用户意图后调用后端微服务**获取实时数据：

```
AiChatService.handleMessage()
  ├─ ORDER   → orderFeign.getUserOrders()    → 构建订单上下文(订单号/状态/金额)
  ├─ COUPON  → promotionFeign.pageQueryUserCoupons() → 构建优惠券上下文
  ├─ CART    → cartFeign.getUserCart()       → 构建购物车上下文
  └─ RECOMMEND → itemFeign.searchItems()     → 构建商品推荐上下文
```

**关键设计**：
- 订单/优惠券/购物车等涉及隐私的意图，会**校验用户登录态**，未登录友好提示
- 数据获取后构建结构化上下文（含金额转换：分→元），喂给 DeepSeek 生成自然语言回复

### 1.4 多轮对话 & 历史管理

- 基于 **Redis List** 存储对话历史，key 格式 `chat:history:{sessionId}`
- 每次对话保留最近 **10 条**消息上下文发给 DeepSeek
- 会话有效期 **24 小时**自动过期
- 角色映射：内部 `ai` → DeepSeek API 要求的 `assistant`
- API 接口：`GET /ai/history` 查询历史、`DELETE /ai/history/clear` 清除

### 1.5 DeepSeek 大模型集成

**位置**: `yunmeng-ai/.../service/DeepSeekService.java`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `deepseek.api-key` | — | DeepSeek API Key（必填） |
| `deepseek.base-url` | `https://api.deepseek.com` | API 地址 |
| `deepseek.model` | `deepseek-chat` | 模型名称 |
| `deepseek.temperature` | `0.7` | 生成随机性 |
| `deepseek.max-tokens` | `1000` | 最大输出长度 |
| `deepseek.timeout` | `30` | 超时秒数 |

**调用流程**：
```
1. 根据意图类型构建 System Prompt（含日期+业务数据）
2. 加载最近10条对话历史
3. POST /v1/chat/completions → DeepSeek API
4. 解析 choices[0].message.content 返回
5. 失败 → 降级到 ReplyBuilder 规则模板
```

**System Prompt 特点**：
- 动态注入当天日期（让 AI 知道"今天"）
- 动态注入业务数据上下文（订单/优惠券/购物车真实数据）
- 明确约束：不编造数据、不索取隐私、200字以内、中文友好

### 1.6 降级容错（ReplyBuilder）

**位置**: `yunmeng-ai/.../service/ReplyBuilder.java`

当 DeepSeek API 不可用时，自动降级到**规则模板**回复，保证服务不中断：

| 场景 | 降级行为 |
|------|---------|
| 查订单 | 解析 Feign 返回数据，格式化输出订单列表（金额分→元） |
| 查优惠券 | 格式化输出优惠券名称+有效期 |
| 查购物车 | 汇总购物车商品+合计金额 |
| 推荐商品 | 格式化输出搜索结果 |
| 闲聊 | 识别"你好/谢谢/再见"等，回复对应引导语 |

### 1.7 API 接口总览

| 接口 | 方法 | 说明 |
|------|------|------|
| `/ai/chat` | POST | 发送消息，返回 AI 回复 + sessionId |
| `/ai/history?sessionId=xxx` | GET | 获取对话历史 |
| `/ai/history/clear?sessionId=xxx` | DELETE | 清除对话历史 |

---

## 二、AI 商品审核 (yunmeng-ai)

### 2.1 两层审核机制

```
商家提交商品 → [第1层] 本地规则快速拦截（免费、毫秒级）
             → [第2层] DeepSeek AI 深度审核（需API调用）
             → 返回: approve(通过) / reject(驳回) / review(存疑)
```

**位置**: `yunmeng-ai/.../service/AiReviewService.java`

### 2.2 第一层：本地规则（零成本拦截）

在调用 AI 之前，先用 7 条硬规则做快速检查：

| 序号 | 规则 | 结果 |
|:--:|------|------|
| 1 | 商品名称为空 | `reject` — 商品名称为空 |
| 2 | 名称少于 2 个字符 | `reject` — 无法识别商品 |
| 3 | 纯数字名称（如"123"） | `reject` — 不是有效商品名 |
| 4 | 纯符号名称 | `reject` — 不是有效商品名 |
| 5 | 名称超过 100 字符 | `reject` — 请精简 |
| 6 | 价格 ≤ 0 | `reject` — 价格必须大于0 |
| 7 | 库存为负数 | `reject` — 库存不能为负 |

**优势**：不消耗 DeepSeek Token，可在毫秒级完成拦截。

### 2.3 第二层：DeepSeek AI 深度审核

本地规则通过后，调用 DeepSeek 进行内容深度审核：

**审核范围**（仅文字内容，图片由人工审核）：

| 违规类型 | 示例 |
|---------|------|
| 敏感内容 | 脏话、人身攻击、政治敏感、色情暗示 |
| 联系方式 | 手机号、微信号、QQ号、网址 |
| 违法信息 | 枪支、毒品、赌博、假币、盗版 |
| 无关内容 | 商品名与商城品类完全无关（如"今天天气真好"） |
| 测试数据 | 明显刷单/测试（如"测试商品123"） |

**三步判定**：

| 判定 | 含义 | 系统行为 |
|------|------|---------|
| `approve` | 建议通过 | 自动设置 `auditStatus=1`（已通过） |
| `reject` | 建议驳回 | 保持待审核，等待人工处理 |
| `review` | 存疑/不确定 | 保持待审核，等待人工处理 |

### 2.4 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/ai/review/item` | POST | AI 审核商品（名称+价格+库存） |

**请求体**：
```json
{ "name": "iPhone 15 Pro Max", "price": 899900, "stock": 100 }
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "suggestion": "approve",
    "reason": "商品名称清晰规范，无违规内容"
  }
}
```

---

## 三、XXL-JOB 分布式定时任务体系

整个系统基于 **XXL-JOB 2.4.1** 实现分布式定时任务调度，涵盖商品审核、订单处理、优惠券管理、数据同步等场景。

### 3.1 任务一览

| 任务名称 | 所属模块 | 功能 | 建议频率 |
|---------|---------|------|---------|
| `aiAutoReviewHandler` | ym-item | AI 批量审核待审商品 | 每 5 分钟 |
| `orderTimeoutCancel` | ym-pay | 扫描超时未支付订单并自动取消 | 每 1 分钟 |
| `couponBeginIssue` | ym-promotion | 定时开启优惠券发放 | 按需配置 |
| `couponEndIssue` | ym-promotion | 定时停止优惠券发放 | 按需配置 |
| `couponExpireCheck` | ym-promotion | 扫描过期用户优惠券 | 每天凌晨 |
| `likedTimesSync` | ym-remark | 点赞数 Redis → DB 同步 | 每 10 分钟 |
| `itemSyncToEsJob` | ym-item | 全量同步商品数据到 Elasticsearch | 每天凌晨 |

### 3.2 任务详解

#### ① AI 批量商品审核 `aiAutoReviewHandler`

**位置**: `ym-item/.../job/AiAutoReviewJob.java`

```
XXL-JOB 触发 → 查询 auditStatus=0 的待审商品
             → 逐条 POST /ai/review/item 调用 yunmeng-ai
             → approve → 自动设置 auditStatus=1
             → reject/review → 保留待人工处理
             → 返回统计: 通过X件 风险X件 异常X件 耗时Xms
```

**设计要点**：
- 逐条调用而非批量（避免单次失败影响全局）
- `suggestion=approve` 才自动通过，`reject`/`review` 保留人工处理
- 详细日志 + XXL-JOB 状态上报（`handleSuccess`/`handleFail`）

#### ② 订单超时取消 `orderTimeoutCancel`

**位置**: `ym-pay/.../handler/OrderJobHandler.java`

- 每分钟扫描一次，将超时未支付的订单自动标记为取消
- 释放被锁定的库存，恢复优惠券状态

#### ③ 优惠券定时管理（3个任务）

**位置**: `ym-promotion/.../handler/CouponJobHandler.java`

| 任务 | 功能 |
|------|------|
| `couponBeginIssue` | 到达发放时间自动开启优惠券领取 |
| `couponEndIssue` | 到达截止时间自动停止发放 |
| `couponExpireCheck` | 扫描已过期的用户优惠券，更新状态 |

#### ④ 点赞数同步 `likedTimesSync`

**位置**: `ym-remark/.../task/LikedTimesSyncJobHandler.java`

- 将 Redis 中缓存的点赞计数批量同步到 MySQL 数据库
- 支持分片执行，处理 `comment` 和 `product` 两种业务类型
- 替代了原来的 `@Scheduled` 实现，统一由 XXL-JOB 调度管理

#### ⑤ 商品 ES 同步 `itemSyncToEsJob`

**位置**: `ym-item/.../job/ItemSyncJobHandler.java`

- 全量将商品数据从 MySQL 同步到 Elasticsearch
- 确保搜索引擎数据与主数据库一致
- 记录耗时并通过 `XxlJobHelper` 上报执行结果

### 3.3 XXL-JOB 执行器配置

所有业务模块（ym-item、ym-pay、ym-promotion、ym-remark、yunmeng-ai）各自作为独立的 XXL-JOB 执行器注册到调度中心：

```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8088/xxl-job-admin
    executor:
      appname: ym-item-executor    # 各模块使用不同 appname
      port: 9999                    # 各模块使用不同端口
      address:                     # 需配置实际物理IP（防止虚拟网卡探测错误）
```

---

## 四、技术亮点总结

### AI 模块亮点

| 亮点 | 说明 |
|------|------|
| 🎯 **意图识别前置** | 先用关键词规则做意图分类，再决定是否调 AI，节省 Token |
| 📊 **业务数据注入** | AI 不只是闲聊，能真正查询用户的订单/优惠券/购物车 |
| 🔄 **双轨降级** | DeepSeek 失败 → 规则模板降级，保证服务高可用 |
| 🛡️ **两层审核** | 本地规则(免费毫秒级) + AI 深度(按需调用)，成本最优 |
| 💬 **多轮对话** | Redis 存储 24h 会话历史，支持上下文连续对话 |
| 🔐 **隐私保护** | 未登录不查业务数据，AI 约束不索取手机号等敏感信息 |
| ⏱️ **定时自动化** | XXL-JOB 每 5 分钟自动审核待审商品，零人工干预可批量通过 |

### 定时任务亮点

| 亮点 | 说明 |
|------|------|
| 🗂️ **统一调度** | 所有定时任务由 XXL-JOB 统一管理，取代了分散的 @Scheduled |
| 📈 **可观测** | 每次任务执行均有日志+耗时统计，成功/失败状态上报调度中心 |
| 🧩 **分片支持** | 点赞同步等任务支持分片执行，适配大数据量场景 |
| ⚡ **配置灵活** | 执行频率通过 XXL-JOB 调度中心页面配置，无需重启服务 |

---

## 五、系统架构图（AI + 定时任务视角）

```
┌─────────────────────────────────────────────────────────────┐
│                     XXL-JOB 调度中心 (:8088)                  │
│   aiAutoReviewHandler │ orderTimeoutCancel │ coupon* │ ...  │
└──────┬──────────────────┬──────────────────┬────────────────┘
       │ 每5分钟           │ 每分钟            │
       ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│  ym-item     │  │  ym-pay      │  │  ym-promotion    │
│  AiAutoReview│  │  OrderJob    │  │  CouponJob       │
│  Job         │  │  Handler     │  │  Handler         │
└──────┬───────┘  └──────────────┘  └──────────────────┘
       │ POST /ai/review/item
       ▼
┌──────────────────────────────────────────────────────────────┐
│                      yunmeng-ai 服务                          │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  │
│  │  AiChatService  │  │  AiReviewService                 │  │
│  │  ├─IntentParser │  │  ├─ 本地规则(7条，免费)           │  │
│  │  ├─Feign(订单/   │  │  ├─ DeepSeek AI 深度审核         │  │
│  │  │ 优惠券/购物车)│  │  └─ 返回 approve/reject/review   │  │
│  │  ├─Redis(历史)   │  └──────────────────────────────────┘  │
│  │  └─ReplyBuilder  │                                         │
│  └────────┬─────────┘                                         │
│           │                                                   │
│  ┌────────▼─────────────────────────────────────────────┐    │
│  │              DeepSeekService                          │    │
│  │  POST https://api.deepseek.com/v1/chat/completions    │    │
│  │  model: deepseek-chat | temperature: 0.7 | max:1000  │    │
│  └──────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

---

## 六、模块文件清单

```
yunmeng-ai/
├── config/
│   ├── DeepSeekConfig.java      # DeepSeek API 配置(api-key/model/temperature)
│   ├── RestTemplateConfig.java  # HTTP 客户端配置
│   ├── FeignConfig.java         # Feign 请求拦截器(用户身份透传)
│   ├── WebMvcConfig.java        # Web 配置
│   └── XxlJobConfig.java        # XXL-JOB 执行器配置
├── controller/
│   ├── AiChatController.java    # AI 智能客服接口
│   └── AiReviewController.java  # AI 商品审核接口
├── model/
│   ├── ChatRequest.java         # 聊天请求
│   ├── ChatResponse.java        # 聊天响应
│   ├── ChatMessage.java         # 单条消息
│   ├── ChatHistory.java         # 对话历史
│   ├── IntentType.java          # 意图枚举(ORDER/COUPON/CART/RECOMMEND/SHOP/CHAT)
│   ├── ItemReviewRequest.java   # 审核请求
│   └── ItemReviewResponse.java  # 审核响应(approve/reject/review)
├── service/
│   ├── AiChatService.java       # 对话核心逻辑(意图→数据→DeepSeek→降级)
│   ├── AiReviewService.java     # 审核核心逻辑(本地规则+AI)
│   ├── DeepSeekService.java     # DeepSeek API 调用封装
│   ├── IntentParser.java        # 关键词意图识别
│   └── ReplyBuilder.java        # 降级规则模板回复
└── AiApplication.java           # 启动类

相关定时任务（分布在各自模块）:
ym-item/job/AiAutoReviewJob.java    # AI 批量审核定时任务
ym-item/job/ItemSyncJobHandler.java  # ES 同步定时任务
ym-pay/handler/OrderJobHandler.java  # 订单超时取消定时任务
ym-promotion/handler/CouponJobHandler.java  # 优惠券定时任务(3个)
ym-remark/task/LikedTimesSyncJobHandler.java # 点赞同步定时任务
```
