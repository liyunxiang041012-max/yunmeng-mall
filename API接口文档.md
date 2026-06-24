# 云梦商城 — 完整 API 接口文档

## 网关路由与鉴权

所有前端请求通过 `http://localhost:8080` 进入网关，由 `StripPrefix=1` 去掉第一段前缀后转发到对应微服务。

| 网关前缀 | 微服务 | 说明 |
|----------|--------|------|
| `/us/**` | ym-user | 用户服务 |
| `/it/**` | ym-item | 商品/商家服务 |
| `/ca/**` | ym-pay | 购物车服务 |
| `/py/**` | ym-pay | 订单/支付服务 |
| `/ai/**` | yunmeng-ai | AI 助手服务 |

**鉴权白名单**（无需登录）：

| 接口 | 说明 |
|------|------|
| `POST /us/user/register` | 用户注册 |
| `POST /us/user/login` | 用户登录 |
| `POST /us/user/sendCode` | 发送短信验证码 |
| `POST /us/shop/register` | 商家注册 |

其他接口需在请求头携带 `Authorization: Bearer <token>`。

**响应格式**：所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 200,
  "msg": "ok",
  "data": { ... }
}
```

> 前端 `utils/request.js` 配置了响应拦截器 `res => res.data`，自动解包了一层 axios response。所以前端代码中 `res.data` 取到的是上面这个 Result JSON。

---

## 一、用户服务 ym-user（网关前缀 `/us`）

### 1.1 用户登录

```
POST /us/user/login
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| phone | String | 是 | 手机号 |
| password | String | 是 | 密码 |

```json
// 请求
{ "phone": "13800138000", "password": "123456" }

// 返回
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOi...",
    "userId": 1,
    "nickname": "张三",
    "role": 0
  }
}
```

---

### 1.2 发送短信验证码

```
POST /us/user/sendCode
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| phone | String | 是 | 手机号 |

```json
// 请求
{ "phone": "13800138000" }

// 返回
{ "code": 200, "data": null }
```

---

### 1.3 用户注册

```
POST /us/user/register
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| phone | String | 是 | 手机号 |
| code | String | 是 | 短信验证码 |
| password | String | 是 | 密码 |
| nickname | String | 是 | 昵称 |

```json
// 请求
{
  "phone": "13800138000",
  "code": "123456",
  "password": "123456",
  "nickname": "张三"
}

// 返回
{ "code": 200, "data": null }
```

---

### 1.4 商家注册（内部 Feign 调用）

```
POST /us/user/shop/register
```

> 此接口由 ym-item 的 `ShopServiceImpl.addShop()` 通过 Feign 调用，不直接暴露给前端。

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| shopName | String | 是 | 店铺名称 |
| phone | String | 是 | 手机号 |
| code | String | 是 | 短信验证码 |
| password | String | 是 | 密码 |
| nickname | String | 是 | 昵称 |

返回 `Long` 类型的 userId。

---

### 1.5 获取用户详情

```
GET /us/user/detail
```

需登录。返回当前登录用户的详细信息。

```json
// 返回
{
  "code": 200,
  "data": {
    "userId": 1,
    "nickname": "张三",
    "phone": "138****8000",
    "role": 0,
    "avatar": "/images/avatar/default.png"
  }
}
```

---

### 1.6 退出登录

```
POST /us/user/logout
```

---

### 1.7 收货地址 — 查询列表

```
GET /us/address/list
```

```json
// 返回
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "receiverName": "张三",
      "phone": "13800138000",
      "province": "广东省",
      "city": "深圳市",
      "district": "南山区",
      "detail": "科技园路1号",
      "isDefault": true
    }
  ]
}
```

---

### 1.8 收货地址 — 新增

```
POST /us/address/add
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| receiverName | String | 是 | 收件人姓名 |
| phone | String | 是 | 联系电话 |
| province | String | 是 | 省 |
| city | String | 是 | 市 |
| district | String | 是 | 区 |
| detail | String | 是 | 详细地址 |
| isDefault | Boolean | 否 | 是否默认地址 |

---

### 1.9 收货地址 — 修改

```
PUT /us/address/update/{id}
```

参数同上。

---

### 1.10 收货地址 — 删除

```
DELETE /us/address/delete/{id}
```

---

### 1.11 收货地址 — 设为默认

```
PUT /us/address/setDefault/{id}
```

---

## 二、商品服务 ym-item（网关前缀 `/it`）

### 2.1 商品分类 — 顶级分类

```
GET /it/categories/top
```

```json
// 返回
{
  "code": 200,
  "data": [
    { "id": 1, "name": "电子产品", "parentId": 0, "sort": 1, "status": 1 }
  ]
}
```

---

### 2.2 商品 — 分页查询

```
GET /it/items/page
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 10 |
| categoryId | Long | 否 | 分类ID |
| keyword | String | 否 | 搜索关键词 |
| sort | String | 否 | 排序方式 |

```json
// 返回
{
  "code": 200,
  "data": {
    "total": 100,
    "pages": 10,
    "list": [
      {
        "id": 1001,
        "name": "极简无线降噪耳机",
        "mainImage": "/images/item/101/main.jpg",
        "price": 8900,
        "sales": 1234
      }
    ]
  }
}
```

---

### 2.3 商品 — 详情

```
GET /it/items/{id}
```

```json
// 返回
{
  "code": 200,
  "data": {
    "id": 1001,
    "name": "极简无线降噪耳机",
    "mainImage": "/images/item/101/main.jpg",
    "price": 8900,
    "description": "高品质降噪耳机",
    "skus": [
      { "id": 10001, "name": "星空黑 / 128G", "price": 8900, "stock": 100 }
    ],
    "images": ["/images/item/101/detail1.jpg"],
    "shopId": 10,
    "shopName": "数码旗舰店"
  }
}
```

---

### 2.4 商品 — 基本信息（Feign 内部调用）

```
GET /it/items/info/{itemId}
```

---

### 2.5 商品 — 批量获取基本信息（Feign 内部调用）

```
POST /it/items/batch-info
Body: [1001, 1002, 1003]
```

---

### 2.6 商品 — 同步到 ES

```
GET /it/items/sync
```

---

### 2.7 SKU — 查询

```
GET /it/sku/info/{skuId}
```

返回 `SkuInfoDTO`：`{ id, itemId, name, price, image, stock, shopId }`

---

### 2.8 SKU — 批量查询（Feign 内部调用）

```
POST /it/sku/batch-info
Body: [10001, 10002]
```

---

### 2.9 商家 — 注册

```
POST /it/shop/register
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| shopName | String | 是 | 店铺名称 |
| phone | String | 是 | 手机号 |
| code | String | 是 | 短信验证码 |
| password | String | 是 | 登录密码 |
| nickname | String | 是 | 昵称 |
| logo | String | 否 | 店铺 logo URL |
| description | String | 否 | 店铺描述 |

**详细文档** → [商家注册接口文档.md](商家注册接口文档.md)

---

### 2.10 商家 — 查询（购物车展示用）

```
GET /it/shop/cart/{id}
```

返回 `ShopCartVO`：`{ id, name, logo }`

---

### 2.11 商家 — 批量查询（购物车展示用）

```
POST /it/shop/batch-info
Body: [10, 11]
```

---

### 2.12 商家 — 详细信息

```
GET /it/shop/info/{shopId}
```

返回 `ShopInfoDTO`：`{ id, name, logo, description, rating, sales }`

---

### 2.13 商家 — 批量详细信息（Feign 内部调用）

```
POST /it/shop/batch-detail
Body: [10, 11]
```

---

### 2.14 商品收藏 — 切换收藏

```
POST /it/favorites/toggle/{itemId}
```

需登录。返回 `true`（已收藏）或 `false`（已取消）。

---

### 2.15 商品收藏 — 检查收藏状态

```
GET /it/favorites/check/{itemId}
```

返回 `true` 或 `false`。

---

### 2.16 商品收藏 — 我的收藏列表

```
GET /it/favorites/my?page=1&size=10
```

```json
// 返回
{
  "code": 200,
  "data": {
    "total": 5,
    "list": [
      {
        "itemId": 1001,
        "name": "极简无线降噪耳机",
        "image": "/images/item/101/main.jpg",
        "price": 8900,
        "favoriteTime": "2026-06-17 10:00:00"
      }
    ]
  }
}
```

---

### 2.17 浏览历史 — 我的历史

```
GET /it/history/my?page=1&size=10
```

---

### 2.18 浏览历史 — 删除单条

```
DELETE /it/history/{id}
```

---

### 2.19 浏览历史 — 清空全部

```
DELETE /it/history/clear
```

---

## 三、购物车 ym-pay（网关前缀 `/ca`）

### 3.1 添加商品到购物车

```
POST /ca/cart/add
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| skuId | Long | 是 | SKU ID |
| quantity | Integer | 是 | 数量 |

---

### 3.2 查询购物车列表

```
GET /ca/cart/list
```

```json
// 返回
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "skuId": 10001,
      "name": "极简无线降噪耳机",
      "image": "/images/item/101/main.jpg",
      "shopName": "数码旗舰店",
      "skuName": "星空黑 / 128G",
      "price": 8900,
      "quantity": 2,
      "shopId": 10
    }
  ]
}
```

---

### 3.3 更新购物车数量

```
PUT /ca/cart/update
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| id | Long | 是 | 购物车项ID |
| quantity | Integer | 是 | 新数量 |

---

### 3.4 删除购物车商品

```
DELETE /ca/cart/delete
```

```json
{ "ids": [1, 2, 3] }
```

---

## 四、订单与支付 ym-pay（网关前缀 `/py`）

### 4.1 创建订单

```
POST /py/order/create
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| items | Array | 是 | 订单商品列表 |
| items[].skuId | Long | 是 | SKU ID |
| items[].quantity | Integer | 是 | 购买数量 |

```json
// 请求
{
  "items": [
    { "skuId": 10001, "quantity": 2 }
  ]
}

// 返回
{ "code": 200, "data": "20260617001" }
```

> `data` 直接是订单ID字符串。

---

### 4.2 查询订单列表

```
GET /py/order/list
```

```json
// 返回
{
  "code": 200,
  "data": [
    {
      "id": "20260617001",
      "totalAmount": 17800,
      "payAmount": 17800,
      "discountAmount": 0,
      "status": 1,
      "createTime": "2026-06-17 14:30:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 订单号 |
| totalAmount | Long | 总金额（分） |
| payAmount | Long | 实付金额（分） |
| status | Integer | 1=待支付, 2=已支付, 3=已发货, 4=已取消 |
| createTime | String | 创建时间 |

---

### 4.3 查询订单详情

```
GET /py/order/detail/{orderId}
```

返回字段同订单列表中的单条记录。

---

### 4.4 取消订单

```
PUT /py/order/cancel/{orderId}
```

---

### 4.5 更新订单状态

```
PUT /py/order/status
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| orderId | String | 是 | 订单号 |
| status | Integer | 是 | 新状态 |

---

### 4.6 查询订单商品列表

```
GET /py/order-item/list/{orderId}
```

```json
// 返回
{
  "code": 200,
  "data": [
    {
      "id": 5001,
      "orderId": "20260617001",
      "skuId": 10001,
      "name": "极简无线降噪耳机",
      "image": "/images/item/101/main.jpg",
      "price": 8900,
      "quantity": 2
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 商品名称（前端用 `item.name`） |
| image | String | 商品图片（前端用 `item.image`） |
| price | Long | 单价（分，前端除以100转元） |
| quantity | Integer | 数量 |

---

### 4.7 创建支付单

```
POST /py/pay/create
```

（当前为占位实现）

---

### 4.8 查询支付记录列表

```
GET /py/pay/list
```

---

### 4.9 查询支付详情

```
GET /py/pay/detail/{id}
```

---

### 4.10 取消支付

```
PUT /py/pay/cancel/{id}
```

---

## 五、优惠券 ym-promotion

> 优惠券服务目前未在网关配置路由前缀，由前端直接调用或通过内部 Feign 访问。

### 5.1 新增优惠券

```
POST /coupons
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| name | String | 是 | 优惠券名称 |
| type | Integer | 是 | 类型：1=满减, 2=折扣 |
| discountValue | Integer | 是 | 优惠值（满减为金额分，折扣为百分比） |
| minAmount | Integer | 是 | 最低消费金额（分） |
| totalNum | Integer | 是 | 发放总量 |
| startTime | String | 是 | 生效时间 |
| endTime | String | 是 | 失效时间 |

---

### 5.2 分页查询优惠券

```
GET /coupons/page?page=1&size=10
```

---

### 5.3 根据ID查优惠券

```
GET /coupons/{id}
```

---

### 5.4 更新优惠券

```
PUT /coupons/{id}
```

---

### 5.5 删除优惠券

```
DELETE /coupons/{id}
```

---

### 5.6 开始发放优惠券

```
PUT /coupons/{id}/issue
```

---

### 5.7 暂停发放优惠券

```
PUT /coupons/{id}/pause
```

---

### 5.8 查询发放中的优惠券列表

```
GET /coupons/list
```

---

### 5.9 用户领取优惠券

```
POST /user-coupons/{couponId}/receive
```

---

### 5.10 兑换优惠券

```
POST /user-coupons/{code}/exchange
```

---

### 5.11 用户优惠券分页查询

```
GET /user-coupons/page
```

---

### 5.12 兑换码分页查询

```
GET /codes/page
```

---

## 六、评论与点赞 ym-remark

> ym-remark 服务目前未在网关配置路由前缀。

### 6.1 发表评论/回复

```
POST /comments
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| bizId | Long | 是 | 业务ID（商品ID/店铺ID） |
| bizType | String | 是 | 业务类型：item / shop |
| content | String | 是 | 评论内容 |
| parentId | Long | 否 | 父评论ID（回复时需要） |
| rating | Integer | 否 | 评分（1-5，商品评论时使用） |

---

### 6.2 分页查询评论

```
GET /comments/page?bizId=1001&bizType=item&pageNo=1&pageSize=20
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| bizId | Long | 是 | 业务ID |
| bizType | String | 是 | item / shop |
| pageNo | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |

---

### 6.3 点赞/取消点赞

```
POST /likes
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| bizId | Long | 是 | 业务ID（评论ID） |
| bizType | String | 是 | 业务类型 |

---

### 6.4 查询点赞状态

```
GET /likes/list?bizIds=1,2,3
```

返回已点赞的 bizId 集合。

---

## 七、AI 助手 yunmeng-ai（网关前缀 `/ai`）

### 7.1 发送消息

```
POST /ai/chat
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| message | String | 是 | 用户消息 |
| sessionId | String | 否 | 会话ID，不传则创建新会话 |

```json
// 请求
{ "message": "帮我查一下我的订单", "sessionId": "abc123" }

// 返回
{
  "code": 200,
  "data": {
    "reply": "您目前有2个订单...",
    "sessionId": "abc123",
    "action": { "type": "show_order", "orderId": "20260617001" }
  }
}
```

---

### 7.2 获取对话历史

```
GET /ai/history?sessionId=abc123
```

---

### 7.3 清除对话历史

```
DELETE /ai/history?sessionId=abc123
<!-- 或 -->
DELETE /ai/history/clear?sessionId=abc123
```

---

## 附录：调用链路汇总

```
前端 → 网关(8080) → StripPrefix=1 → 微服务
                                  ├─ /us/** → ym-user
                                  │    ├─ UserController       用户登录/注册/详情
                                  │    └─ UserAddressController 收货地址CRUD
                                  │
                                  ├─ /it/** → ym-item
                                  │    ├─ CategoryController    商品分类
                                  │    ├─ ItemController        商品查询/详情
                                  │    ├─ ItemSkuController     SKU查询
                                  │    ├─ ShopController        商家注册/查询
                                  │    ├─ FavoriteController    收藏
                                  │    └─ HistoryController     浏览历史
                                  │
                                  ├─ /ca/** → ym-pay
                                  │    └─ CartController        购物车CRUD
                                  │
                                  ├─ /py/** → ym-pay
                                  │    ├─ OrderController       订单创建/查询/取消
                                  │    ├─ OrderItemController   订单商品列表
                                  │    └─ PayController         支付
                                  │
                                  └─ /ai/** → yunmeng-ai
                                       └─ AiChatController     AI对话
```
