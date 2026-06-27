# 优惠券 ID JS 精度丢失 - 前后端修复方案

> 问题：雪花 ID 19 位（如 `2069989063945297922`），JS `Number` 最大安全整数 16 位，JSON.parse 时直接截断末尾，导致前后端 ID 不一致。

---

## 后端（已改）

**ym-promotion `UserCouponVO.java`** — id 字段加了 `@JsonSerialize`：

```java
@JsonSerialize(using = ToStringSerializer.class)
private Long id;
```

重启 `ym-promotion` 后，`GET /pm/user-coupons/page` 返回：

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": "2069989063945297922",    ← 字符串，不丢精度
        "name": "周末闪购无门槛减5元"
      }
    ]
  }
}
```

---

## 前端（需改一处）

**Cart.vue** 创建订单时，`userCouponId` 传字符串：

```js
// 改前（丢精度）：
userCouponId: appliedCoupon.value?.id

// 改后（正确）：
userCouponId: String(appliedCoupon.value?.id)
```

---

## 端到端验证

```
1. 后端返回   "id": "2069989063945297922"    (JSON 字符串)
2. JS 拿到    "2069989063945297922"           (String，不变形)
3. 前端传参   userCouponId: "2069989063945297922"
4. 后端收到   "2069989063945297922"           (String → parseLong)
5. 核销成功   DB 查到了  ← 和数据库一致
```
