# 后端接口实现 — 前端对接文档

> **日期**：2026-06-23  
> **网关规则**：`StripPrefix=1`，前端调 `/us/user/xxx`，网关去 `/us/` 后路由到 ym-user

---

## 一、新增 API（3个）

### 1. `PUT /us/user/profile` — 完善/更新个人资料

**鉴权**：需要登录 token

#### 请求

```json
{
  "birthday": "1995-06-15",
  "gender": "male",
  "province": "广东省"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| birthday | String | 否 | yyyy-MM-dd |
| gender | String | 否 | `"male"` / `"female"` |
| province | String | 否 | 省份名，如"广东省" |

> 所有字段均可选，只传要改的，不传保持原值。

#### 性别枚举映射

`PUT /us/user/profile` 接收和 `GET /us/user/admin/overview` 返回的性别字段，均按此映射：

| 前端值 | DB值 | 含义 |
|--------|------|------|
| `"male"` | 1 | 男 |
| `"female"` | 2 | 女 |
| 未填 | 0 | 未知 |

#### 响应

```json
{"code": 200, "message": "操作成功", "data": null, "timestamp": 1719123456789}
```

---

### 2. `GET /us/user/admin/overview` — 仪表盘概览

**鉴权**：需要管理员 token

#### 响应

```json
{
  "code": 200,
  "data": {
    "stats": {
      "gmv":        {"value": "¥—",  "trend": 0.0, "delta": "—"},
      "orders":     {"value": "—",   "trend": 0.0, "delta": "—"},
      "users":      {"value": "26",  "trend": 0.0, "delta": "26"},
      "conversion": {"value": "—",   "trend": 0.0, "delta": "—"}
    },
    "userPortrait": {
      "gender": {"male": 58, "female": 42},
      "ages": [
        {"label": "18-24", "percent": 28, "color": "#D9A53C"},
        {"label": "25-34", "percent": 42, "color": "#1A1712"},
        {"label": "35-44", "percent": 18, "color": "#8C6308"},
        {"label": "45-54", "percent": 8,  "color": "#9B9484"},
        {"label": "55+",   "percent": 4,  "color": "#C0B8A8"}
      ],
      "regions": [
        {"name": "广东", "percent": 22},
        {"name": "浙江", "percent": 18}
      ]
    },
    "consumeLevels": [
      {"label": "高消费",   "percent": 55, "color": "#D9A53C"},
      {"label": "中等消费", "percent": 25, "color": "#1A1712"},
      {"label": "低消费",   "percent": 20, "color": "#8C6308"}
    ],
    "recentOrders": [
      {"id": 1, "orderNo": "—", "user": "—", "product": "—",
       "amount": "—", "status": "—", "statusClass": "pending", "time": "—"}
    ],
    "topProducts": [
      {"name": "—", "sales": "—", "revenue": "—", "color": "#FDE8C8"}
    ]
  }
}
```

#### 数据来源说明

| 字段 | 来源 | 说明 |
|------|------|------|
| `stats.users` | ✅ 真实 DB | `value`=用户总数，`delta`=启用数 |
| `userPortrait.gender` | ✅ 真实 DB | 从 user_profile 表统计 |
| `userPortrait.regions` | ✅ 真实 DB | 从 user_profile.region 解析省名统计 TOP5 |
| `userPortrait.ages` | ⚠️ 固定值 | 后期可接真实生日数据 |
| `stats.gmv` | ⚠️ 占位 | 需 ym-pay 管理后台汇总接口 |
| `stats.orders` | ⚠️ 占位 | 需 ym-pay 管理后台汇总接口 |
| `recentOrders` | ⚠️ 占位 | 共5条占位记录，需 ym-pay 接口 |
| `topProducts` | ⚠️ 占位 | 共5条占位记录，需 ym-item 接口 |
| `consumeLevels` | ⚠️ 固定值 | 后期按消费金额分档 |

> **前端建议**：`value` 为 `"—"` 时显示占位符，`trend=0` 时不显示趋势箭头。

---

### 3. `GET /us/user/admin/revenue?period=7` — 收入趋势

**鉴权**：需要管理员 token

#### 参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| period | int | 7 | 7 / 30 / 90 |

#### 响应

```json
{
  "code": 200,
  "data": {
    "values": [4200, 3800, 5100, 4600, 6800, 7200, 12850]
  }
}
```

- `period=7` → 7 个数值
- `period=30` → 30 个数值
- `period=90` → 12 个数值

> ⚠️ 当前为模拟数据（固定种子，同参数返回值不变）。后续接 ym-pay 真实数据。

---

## 二、已有 API 修复/改进

### 全局异常处理修复

**问题**：`GlobalExceptionHandler` 在 `ym-common` 中，但各模块的 `@SpringBootApplication` 默认只扫自己的包（如 `com.liyun.user`），不会扫 `com.liyun.common`，导致所有业务异常直接暴露为 500，前端收不到 `message`。

**修复**：在 `ym-common` 添加 Spring Boot 自动配置：

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
→ com.liyun.common.config.CommonAutoConfiguration
→ @Import(GlobalExceptionHandler.class)
```

**效果**：依赖 `ym-common` 的所有模块自动加载异常处理器，异常返回标准 JSON。

### 网关 401 统一返回 JSON

**问题**：`AuthGlobalFilter` 的 `unauthorized()` 只设状态码 401，无 body。

**修复**：返回标准 JSON body：
```json
{"code": 401, "message": "未登录或Token过期", "timestamp": 1719123456789}
```

### 网关白名单补全

新增 `/us/user/admin/login` 到白名单（管理员登录无需 token）。

**当前完整白名单**：
```
/us/user/register      /us/user/login          /us/user/shop/login
/us/user/admin/login   /us/user/sendCode       /it/shop/register
```

---

## 三、统一错误响应格式

所有接口失败时返回以下格式，`message` 是可直接展示的中文提示：

```json
{
  "code": 500,
  "message": "该账号不是管理员",
  "timestamp": 1719123456789
}
```

| 场景 | code | message |
|------|------|--------|
| 账号不存在 | 1002 | 用户不存在 |
| 密码错误 | 500 | 密码错误 |
| 不是管理员 | 500 | 该账号不是管理员 |
| 账号已禁用 | 500 | 该账号已被禁用 |
| 未登录 | 401 | 未登录或Token过期 |
| 权限不足 | 500 | 仅管理员可访问此接口 |

> `data` 字段为 null 时不返回（`@JsonInclude(NON_NULL)`）。

---

## 四、涉及文件清单

| 模块 | 文件 | 操作 |
|------|------|------|
| ym-common | `config/CommonAutoConfiguration.java` | 🆕 自动配置 |
| ym-common | `resources/META-INF/spring/...AutoConfiguration.imports` | 🆕 自动注册 |
| ym-gateway | `filter/AuthGlobalFilter.java` | ✏️ 白名单+JSON 401 |
| ym-user | `enums/GenderEnum.java` | 🆕 性别枚举 |
| ym-user | `dto/UpdateProfileDTO.java` | 🆕 |
| ym-user | `service/IUserService.java` | ✏️ +updateProfile |
| ym-user | `service/IAdminService.java` | ✏️ +getOverview +getRevenue |
| ym-user | `service/impl/UserServiceImpl.java` | ✏️ +updateProfile |
| ym-user | `service/impl/AdminServiceImpl.java` | ✏️ +getOverview +getRevenue |
| ym-user | `controller/UserController.java` | ✏️ +PUT /profile |
| ym-user | `controller/AdminController.java` | ✏️ +GET /overview +GET /revenue |

---

## 五、编译部署

```powershell
cd E:\cloud\yunmeng-mall-main\yunmeng-mall-main

# 1. 编译 ym-common（全局异常处理器自动配置生效）
mvn clean install -pl ym-common -am -DskipTests

# 2. 编译 ym-user
mvn clean install -pl ym-user -am -DskipTests

# 3. 编译 gateway
mvn clean install -pl ym-gateway -am -DskipTests

# 4. 重启 ym-user + ym-gateway
```
