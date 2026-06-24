# My.vue 页面 — 后端接口对接说明

> **后端接口格式为准**。本文档说明后端实际返回格式，请前端据此调整解析逻辑。

---

## 📌 核心变更：`/my` 接口返回分页结构

收藏列表和浏览历史接口使用项目统一的 `PageDTO` 分页结构，**数据在 `data.list` 中**，而非 `data` 直接是数组。

### 统一响应外层

```json
{
  "code": 200,
  "message": "ok",
  "data": { ... }     // ← data 内容见下方各接口
}
```

---

## 1. GET /it/favorites/my — 我的收藏列表

### 请求

```
GET /it/favorites/my?page=1&size=20
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页条数 |

### 响应格式

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 34,
    "pages": 4,
    "list": [
      {
        "id": 1,
        "itemId": 101,
        "name": "极简无线降噪耳机",
        "mainImage": "/images/item/101/main.jpg",
        "price": 8900,
        "createTime": "2026-05-28 14:30:00"
      }
    ]
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| data.total | Long | 总记录数 |
| data.pages | Long | 总页数 |
| data.list | Array | **收藏列表（这才是你要遍历的数据）** |
| data.list[].id | Long | 收藏记录 ID |
| data.list[].itemId | Long | 商品 ID（用于取消收藏） |
| data.list[].name | String | 商品名称 |
| data.list[].mainImage | String | 商品主图（相对路径） |
| data.list[].price | Long | 商品价格，**单位：分** |
| data.list[].createTime | String | 收藏时间 `yyyy-MM-dd HH:mm:ss` |

### 前端代码调整

```javascript
// ❌ 之前（拿不到数据）
const res = await getMyFavorites()
this.favoriteItems = res.data  // undefined，因为 res.data 是 PageDTO 对象

// ✅ 改为
const res = await getMyFavorites({ page: 1, size: 20 })
this.favorites = res.data.list       // 收藏列表
this.total = res.data.total           // 可选：总条数
this.pages = res.data.pages           // 可选：总页数
```

---

## 2. GET /it/history/my — 我的浏览历史

### 请求

```
GET /it/history/my?page=1&size=20
Authorization: Bearer <token>
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页条数 |

### 响应格式

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 210,
    "pages": 21,
    "list": [
      {
        "id": 1,
        "itemId": 201,
        "name": "轻量碳纤维背包",
        "mainImage": "/images/item/201/main.jpg",
        "price": 12900,
        "createTime": "2026-05-28 10:15:00"
      }
    ]
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| data.total | Long | 总记录数 |
| data.pages | Long | 总页数 |
| data.list | Array | **浏览历史列表（这才是你要遍历的数据）** |
| data.list[].id | Long | 浏览记录 ID |
| data.list[].itemId | Long | 商品 ID |
| data.list[].name | String | 商品名称 |
| data.list[].mainImage | String | 商品主图（相对路径） |
| data.list[].price | Long | 商品价格，**单位：分** |
| data.list[].createTime | String | 浏览时间 `yyyy-MM-dd HH:mm:ss` |

### 前端代码调整

```javascript
// ❌ 之前（拿不到数据）
const res = await getBrowseHistory()
this.historyItems = res.data  // 拿不到数组

// ✅ 改为
const res = await getBrowseHistory({ page: 1, size: 20 })
this.historyItems = res.data.list        // 历史列表
this.footprint = res.data.total           // 足迹 = 总浏览条数
```

---

## 3. POST /it/favorites/toggle/{itemId} — 切换收藏

```
POST /it/favorites/toggle/101
Authorization: Bearer <token>
```

### 响应

```json
{
  "code": 200,
  "message": "ok",
  "data": true
}
```

| data 值 | 含义 |
|---------|------|
| true | 已收藏 |
| false | 已取消收藏 |

---

## 4. DELETE /it/history/{id} — 删除单条浏览记录

```
DELETE /it/history/1
Authorization: Bearer <token>
```

### 响应

```json
{
  "code": 200,
  "message": "ok"
}
```

---

## 5. DELETE /it/history/clear — 清空全部浏览记录

```
DELETE /it/history/clear
Authorization: Bearer <token>
```

### 响应

```json
{
  "code": 200,
  "message": "ok"
}
```

---

## ⚠️ 注意事项

1. **data.list 才是列表数据** — 这是前端拿不到数据的根本原因
2. **价格单位是分** — 展示时需 `/100` 转为元
3. **图片是相对路径** — 需要拼接 baseURL（如 `http://localhost:8080`）
4. **分页参数可选** — 不传默认 `page=1, size=10`，可通过传参加载更多
5. **足迹数量** — 直接取 `history.data.total` 即可

---

**文档更新时间**：2026-05-28  
**后端接口状态**：全部已实现 ✅
