# 商品多规格（SKU）前后端对接文档

> ym-item 服务重启后生效。已基于 `item_sku` 表实现完毕。

---

## 一、接口变更清单

| 方法 | 路径 | 说明 | 变更 |
|------|------|------|:--:|
| `POST` | `/it/shop/item` | 新增商品 |  支持 specs+skus |
| `PUT` | `/it/shop/item/{itemId}` | 编辑商品 |  支持 specs+skus |
| `GET` | `/it/shop/item/page` | 商家商品列表 |  返回 specs+skus |
| `GET` | `/it/items/{id}` | 商品详情 |  返回 specs+skus |

---

## 二、新增商品 `POST /it/shop/item`

### 单规格（兼容现有，不变）

```json
{
  "name": "纯棉T恤",
  "price": 9900,
  "stock": 100,
  "image": "https://xxx.oss.com/xxx.jpg",
  "categoryId": 3,
  "brandId": 1
}
```

### 多规格（新增）

```json
{
  "name": "纯棉T恤",
  "image": "https://xxx.oss.com/xxx.jpg",
  "categoryId": 3,
  "brandId": 1,
  "specs": [
    { "specName": "颜色", "values": ["白色", "黑色"] },
    { "specName": "尺码", "values": ["S", "M", "L"] }
  ],
  "skus": [
    { "specData": {"颜色":"白色","尺码":"S"}, "price": 9900, "stock": 200 },
    { "specData": {"颜色":"白色","尺码":"M"}, "price": 9900, "stock": 200 },
    { "specData": {"颜色":"黑色","尺码":"M"}, "price": 9900, "stock": 200 },
    { "specData": {"颜色":"黑色","尺码":"L"}, "price": 9900, "stock": 200 }
  ]
}
```

> **判断规则**：`specs` 且 `specs.length > 0` → 多规格，否则单规格。
>
> ⚠️ **多规格时不传顶层 `price` / `stock`**，后端自动算：
> - `price` = MIN(所有 sku.price)
> - `stock` = SUM(所有 sku.stock)

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 3, "name": "纯棉T恤", "price": 9900, "stock": 800 }
}
```

---

## 三、编辑商品 `PUT /it/shop/item/{itemId}`

入参与新增相同，支持 `specs` + `skus` 全量替换。编辑时旧 SKU 软删除，新 SKU 写入。

### 响应

```json
{ "code": 200, "message": "success" }
```

---

## 四、商家商品列表 `GET /it/shop/item/page`

### 请求

| 参数 | 类型 | 默认值 | 说明 |
|------|------|:--:|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页条数 |
| `status` | int | | 1=上架 0=下架（不传=全部） |
| `keyword` | string | | 商品名模糊搜索 |

### 响应

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 3,
        "name": "纯棉T恤",
        "image": "https://xxx.jpg",
        "price": 9900,
        "stock": 800,
        "sold": 5,
        "status": 1,
        "auditStatus": 1,
        "categoryId": 3,
        "brandId": 1,
        "createTime": "2026-06-24T10:00:00",
        "updateTime": "2026-06-24T10:00:00",
        "specs": [
          {
            "specName": "颜色",
            "values": [
              { "value": "白色", "stock": 400 },
              { "value": "黑色", "stock": 400 }
            ]
          },
          {
            "specName": "尺码",
            "values": [
              { "value": "S", "stock": 200 },
              { "value": "M", "stock": 400 },
              { "value": "L", "stock": 200 }
            ]
          }
        ],
        "skus": [
          {
            "id": 7,
            "itemId": 3,
            "skuName": "纯棉T恤 白色 S",
            "price": 9900,
            "stock": 200,
            "image": null,
            "specData": { "颜色": "白色", "尺码": "S" }
          }
        ]
      }
    ],
    "total": 15,
    "pages": 2
  }
}
```

---

## 五、商品详情 `GET /it/items/{id}`

### 响应

```json
{
  "code": 200,
  "data": {
    "id": 3,
    "name": "纯棉T恤",
    "price": 9900,
    "originalPrice": null,
    "mainImage": "https://xxx.jpg",
    "images": ["https://xxx.jpg"],
    "sold": 5,
    "brandName": "某品牌",
    "categoryName": "服饰",
    "description": "富文本描述...",
    "specs": [
      {
        "specName": "颜色",
        "values": [
          { "value": "白色", "stock": 400 },
          { "value": "黑色", "stock": 400 }
        ]
      }
    ],
    "skus": [
      {
        "id": 7,
        "itemId": 3,
        "skuName": "纯棉T恤 白色 S",
        "price": 9900,
        "stock": 200,
        "image": null,
        "specData": { "颜色": "白色", "尺码": "S" }
      }
    ]
  }
}
```

---

## 六、字段速查

### `specs[]` — 规格组

| 字段 | 类型 | 说明 |
|------|------|------|
| `specName` | string | 规格名，如"颜色"、"尺码" |
| `values[].value` | string | 规格值 |
| `values[].stock` | int | 该规格值下所有 SKU 库存之和 |

### `skus[]` — SKU 明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | SKU ID |
| `itemId` | long | 关联商品 ID |
| `skuName` | string | 自动生成，格式 "商品名 白色 S" |
| `price` | int | 价格，**单位分** |
| `stock` | int | 库存 |
| `image` | string | SKU 独立图片，可 null |
| `specData` | object | 规格键值对 `{"颜色":"白色","尺码":"S"}` |

### 全局约定

| 规则 | 说明 |
|------|------|
| Result 包装 | 所有响应格式 `{ "code": 200, "data": {...} }` |
| 分页字段 | 用 `list`，不是 `records` |
| 金额单位 | **分**，前端 `/100` 显示元 |
| 单规格兼容 | `skus` 含 1 条记录，`specs` 为空数组 `[]` |

---

## 七、前端取值示例

```js
// 列表 / 详情
const res = response.data
const item = res.list ? res.list[0] : res
const priceYuan = (item.price / 100).toFixed(2)
const skuList = item.skus || []
const specGroups = item.specs || []

// SKU 匹配选中
const selected = { "颜色": "白色", "尺码": "S" }
const currentSku = skuList.find(sku =>
  Object.entries(selected).every(([k, v]) => sku.specData[k] === v)
)
const currentPrice = currentSku?.price ?? item.price
const currentStock = currentSku?.stock ?? 0

// 库存判断
const valInfo = specGroups
  .find(g => g.specName === "颜色")
  ?.values.find(v => v.value === "白色")
const hasStock = (valInfo?.stock ?? 0) > 0
```
