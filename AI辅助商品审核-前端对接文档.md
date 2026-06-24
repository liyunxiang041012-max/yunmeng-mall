# AI 辅助商品审核 - 前端对接文档

---

## 接口

```
POST /ai/review/item
Content-Type: application/json
```

**请求体：**

```json
{
  "name": "iPhone 15 Pro Max",
  "price": 599900,
  "stock": 100
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 商品名称 |
| price | Long | 价格（分） |
| stock | Integer | 库存 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "suggestion": "approve",
    "reason": "商品名称清晰，价格在合理范围内，建议通过审核。"
  }
}
```

| 字段 | 值 | 含义 |
|------|-----|------|
| suggestion | `approve` | 建议通过 |
| suggestion | `reject` | 建议驳回（有明确违规） |
| suggestion | `review` | 需人工判断 |

---

## 前端使用流程

```
管理员打开待审核商品 → 点击"AI智能审核"按钮
  → POST /ai/review/item  传入商品信息
  → 展示 AI 建议（通过/驳回/需判断）+ 审核理由
  → 管理员决定 approve 或 reject
```

## 示例

```javascript
const { data } = await fetch('/ai/review/item', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ name: item.name, price: item.price, stock: item.stock })
}).then(r => r.json());

// data.suggestion = "approve" | "reject" | "review"
// data.reason = "AI的审核意见文本"
```

---

## 数据库

```sql
ALTER TABLE item ADD COLUMN ai_review TEXT NULL COMMENT 'AI审核结果' AFTER audit_status;
```

---

## 后端文件

| 文件 | 模块 | 说明 |
|------|------|------|
| `AiReviewService.java` | yunmeng-ai | 调 DeepSeek 审核商品 |
| `AiReviewController.java` | yunmeng-ai | 接口 |
| `ItemReviewRequest.java` | yunmeng-ai | 请求 DTO |
| `ItemReviewResponse.java` | yunmeng-ai | 响应 DTO |

DeepSeek 不可用时自动降级返回 `suggestion: "review"` + 提示人工审核。
