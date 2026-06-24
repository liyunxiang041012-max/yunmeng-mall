# 问题：yunmeng-ai 通过 Feign 调用 ym-pay 订单查询失败

## 现象

`yunmeng-ai` 调用 `orderFeign.getUserOrders()` 没有任何异常日志，但用户收不到订单数据。

```
AI 意图识别: message=帮我查最近订单, intent=ORDER
(无异常)
→ 查不到
```

## 根因

**Feign 调用 `ym-pay` 时没有透传 `user-id` Header，`ym-pay` 的 `OrderController` 返回了 `{"code":401, "msg":"用户未登录"}`**，但因为 Feign 返回类型是 `Map<String, Object>` + HTTP 状态码 200，所以不抛异常，静默失败。

```java
// ym-pay OrderController.list()
public Result<List<Order>> list() {
    Long userId = UserContext.getUserId();
    if (userId == null) {
        return Result.fail(401, "用户未登录");  // ← 返回了这个
    }
    ...
}
```

## 原因链条

```
yunmeng-ai 请求线程
    ↓
UserInfoInterceptor.preHandle()  →  UserContext.setUserId(1L)  ✅
    ↓
AiChatService.handleMessage()
    ↓
orderFeign.getUserOrders()
    ↓
FeignConfig.RequestInterceptor.apply()  ← 需要在此透传 user-id Header
    │
    ├─ 旧写法：RequestContextHolder.getRequestAttributes() → request.getHeader("user-id")
    │   ↓
    │  RequestContextHolder 是 ThreadLocal，Feign 拦截器执行时可能为 null ❌
    │
    └─ 新写法：UserContext.getUserId()  →  template.header("user-id", ...)
        ↓
       UserContext 也是 ThreadLocal，但同线程一定可用 ✅
    ↓
→ ym-pay OrderController
    ↓
UserInfoInterceptor.preHandle() → request.getHeader("user-id") 读到值 ✅
    ↓
UserContext.getUserId() ≠ null  →  正常返回订单数据
```

## 修复内容

**文件：`yunmeng-ai/.../config/FeignConfig.java`**

将透传逻辑从"从 `HttpServletRequest` Header 读取"改为"从 `UserContext` 直接取值"：

```java
// 旧：依赖 RequestContextHolder（不可靠）
ServletRequestAttributes attributes = RequestContextHolder.getRequestAttributes();
String userId = request.getHeader("user-id");

// 新：直接用 UserContext（同线程一定可用）
Long userId = UserContext.getUserId();
template.header("user-id", String.valueOf(userId));
```

**文件：`yunmeng-ai/.../AiApplication.java`**

`@EnableFeignClients` 加了 `defaultConfiguration = FeignConfig.class`，让 `ym-api` 中定义的 `OrderFeign`、`CartFeign` 也自动应用 Header 透传拦截器。

```java
@EnableFeignClients(
    basePackages = {"com.liyun.api.client", "com.liyun.ai.client"}, 
    defaultConfiguration = FeignConfig.class
)
```

## 涉及的 Feign 接口（都在 `ym-api` 模块）

| 接口 | 服务 | 是否需要 user-id |
|------|------|:---:|
| `OrderFeign.getUserOrders()` | ym-pay | ✅ |
| `CartFeign.getUserCart()` | ym-pay | ❌ (Controller 不校验) |
| `PromotionFeign.pageQueryUserCoupons()` | ym-promotion | 未知 |
| `ItemFeign.searchItems()` | ym-item | ❌ |
| `ShopFeign` | ym-user | ❌ |
