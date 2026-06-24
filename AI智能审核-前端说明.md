# AI 智能审核 - 前端对接说明

---

## AI 做什么

每 5 分钟自动扫描所有新提交的商品，用 DeepSeek 分析商品名称（不含图片），给出审核建议。

---

## 两种结果

| AI 判断 | 后端动作 | 前端展示 |
|---------|---------|---------|
| **建议通过** (`approve`) | `auditStatus` 自动变为 `1` | 商品出现在「已通过」列表，不再需要人工操作 |
| **有风险** (`review`/`reject`) | **不动**，等管理员处理 | 商品留在「待审核」列表，提示管理员人工判断 |

---

## 管理员看到的

管理员打开「待审核」列表（`GET /it/shop/admin/item/page?auditStatus=0`）：

- `auditStatus=0`：AI 还没给通过的商品（可能是还没轮询到，可能是 AI 觉得有风险）
- `auditStatus=1`：AI 已自动通过，或管理员手动通过
- `auditStatus=2`：管理员手动驳回

**AI 觉得有风险的商品，auditStatus 仍然是 0**，等管理员来看。

---

## 前端不需要改什么

待审核列表、已通过列表的查询逻辑不用变，`auditStatus` 的值含义没变：

```
0 = 待审核（含 AI 还没审的 + AI 觉得有风险的）
1 = 已通过（AI 自动过的 + 管理员手动过的）
2 = 已驳回
```

唯一区别：部分 `auditStatus=1` 的商品是 AI 自动过的，不是人工点的。

---

## 管理员手动审核不变

管理员仍然可以随时手动点「通过」/「驳回」，和之前完全一样：

```
PUT /it/shop/admin/item/{id}/approve → auditStatus=1
PUT /it/shop/admin/item/{id}/reject  → auditStatus=2
```
