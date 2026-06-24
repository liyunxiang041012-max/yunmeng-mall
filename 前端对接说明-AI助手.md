# 云梦 AI 助手 — 前端对接文档

## 一、概述

| 项目 | 值 |
|------|-----|
| 服务名 | `yunmeng-ai` |
| 网关前缀 | `/ai` |
| 认证方式 | `Authorization: Bearer <token>` |
| 会话有效期 | 24 小时（Redis） |
| AI 引擎 | DeepSeek V4 Pro + 规则降级 |

---

## 二、接口清单

### 2.1 发送消息

```
POST /ai/chat
```

**请求体** (`application/json`)：

```json
{
  "message": "帮我查最近订单",
  "sessionId": "ai_1717000000000_abc123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| message | String | ✅ | 用户输入的自然语言 |
| sessionId | String | ❌ | 会话 ID，**首次传空**，后端创建并返回 |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reply": "您最近有 3 笔订单：\n1. 订单 #OD20260531001 — 已发货（3599元）\n2. 订单 #OD20260525002 — 已完成（99元）",
    "sessionId": "ai_1717000000000_abc123"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data.reply | String | AI 回复文本（含 `\n` 换行） |
| data.sessionId | String | 会话 ID，**前端需缓存**用于后续对话 |

**错误响应**：

```json
{
  "code": 500,
  "message": "AI 服务暂时不可用，请稍后重试"
}
```

---

### 2.2 获取对话历史

```
GET /ai/history?sessionId=ai_1717000000000_abc123
```

**参数**：`sessionId` — 会话 ID（必填）

**响应**：

```json
{
  "code": 200,
  "data": {
    "sessionId": "ai_1717000000000_abc123",
    "messages": [
      { "role": "ai",   "text": "你好！我是云梦 AI 助手...", "time": "2026-05-31 12:30:00" },
      { "role": "user", "text": "帮我查订单",                 "time": "2026-05-31 12:31:00" },
      { "role": "ai",   "text": "您最近有 3 笔订单...",       "time": "2026-05-31 12:31:02" }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data.sessionId | String | 会话 ID |
| data.messages | Array | 消息列表，按时间正序排列 |
| messages[].role | String | `"user"` 用户 / `"ai"` AI |
| messages[].text | String | 消息内容 |
| messages[].time | String | 消息时间 `yyyy-MM-dd HH:mm:ss` |

**会话过期时**：

```json
{ "code": 500, "message": "会话不存在或已过期" }
```

---

### 2.3 清除对话历史

```
DELETE /ai/history/clear?sessionId=ai_1717000000000_abc123
```

**响应**：

```json
{ "code": 200, "message": "success" }
```

---

## 三、会话流程

```
用户打开 AI 页面
  │
  ├─ 检查本地是否有 sessionId
  │   ├─ 有 → GET /ai/history?sessionId=xxx  恢复对话
  │   └─ 没有 → 直接开始聊天
  │
  └─ 用户发送消息
        └─ POST /ai/chat  { message, sessionId }
              │
              ├─ 首次：sessionId 留空，响应里拿新的存起来
              └─ 后续：带上已缓存的 sessionId
```

> 前端负责缓存 sessionId（localStorage / Vuex），24 小时内有效。

---

## 四、AI 能力范围

| 意图 | 触发词示例 | 调用的后端数据 |
|------|-----------|--------------|
| 查订单 | "我的订单"、"物流到哪了"、"发货了吗" | 订单列表 |
| 查优惠券 | "我有什么券"、"有什么优惠"、"满减" | 可用优惠券 |
| 查购物车 | "购物车有什么"、"帮我看看购物车" | 购物车内容 |
| 推荐商品 | "推荐一款耳机"、"帮我找T恤" | 商品搜索 |
| 闲聊 | "你好"、"谢谢" | 无数据，DeepSeek 直接回复 |

> DeepSeek 调用失败时会自动降级到固定模板回复，不影响基本使用。

---

## 五、前端注意事项

1. **sessionId 缓存**：拿到后存 `localStorage`，后续请求都带上
2. **换行渲染**：`reply` 含 `\n`，前端用 `white-space: pre-wrap` 或 `<br>` 渲染
3. **首次消息**：第一轮 `sessionId` 传 `null` 或不传，接口返回后保存
4. **历史恢复**：页面进入时如果有 sessionId，先调 `GET /history` 恢复对话
5. **清除对话**：用户点击"新对话"时调 `DELETE /history/clear`，清除本地 sessionId
6. **超时处理**：网络超时或 5xx 时，提示"AI 服务繁忙，请稍后重试"
