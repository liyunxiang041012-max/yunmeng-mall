# 商品图片上传 OSS 接口文档

## 接口概述

商家在添加/编辑商品时，先通过此接口上传商品图片到阿里云 OSS，拿到返回的图片 URL 后再传给新增/编辑商品接口。

---

## 接口详情

| 项目 | 内容 |
|------|------|
| **接口路径** | `POST /it/shop/item/upload/image` |
| **网关路由** | `/it/**` → `ym-item`（StripPrefix=1） |
| **后端路径** | `/shop/item/upload/image` |
| **请求方式** | `POST` |
| **Content-Type** | `multipart/form-data` |
| **鉴权** | 需要登录，仅商家角色（role=1）可访问 |

---

## 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `file` | MultipartFile | 是 | 商品图片文件 |

### 限制

| 限制项 | 值 |
|--------|-----|
| 最大文件大小 | 5MB |
| 允许格式 | jpg / png / gif / webp |

---

## 响应格式

### 成功响应

```json
{
  "code": 0,
  "data": "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/item/2026/06/24/xxx.jpg"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 0 表示成功 |
| `data` | String | OSS 图片访问 URL |

### 失败响应

```json
// 未登录
{ "code": 401, "msg": "请先登录" }

// 非商家角色
{ "code": 1, "msg": "仅商家可访问此接口" }

// 文件为空
{ "code": 1, "msg": "上传文件不能为空" }

// 文件过大
{ "code": 1, "msg": "文件大小不能超过5MB" }

// 格式不支持
{ "code": 1, "msg": "只支持 jpg、png、gif、webp 格式的图片" }
```

---

## 前端调用示例

```javascript
// 1. 上传图片
const formData = new FormData();
formData.append('file', file); // file 为 input[type=file] 选中的文件

const res = await fetch('/it/shop/item/upload/image', {
  method: 'POST',
  body: formData,
  headers: {
    'Authorization': `Bearer ${token}`  // 携带登录 token
  }
});
const { data: imageUrl } = await res.json(); // 拿到 OSS 图片 URL

// 2. 创建商品时传入 imageUrl
await fetch('/it/shop/item', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: '商品名称',
    image: imageUrl,    // ← 上传拿到的 OSS URL
    categoryId: 1,
    brandId: 1,
    price: 9900,        // 分，表示 99.00 元
    stock: 100
  })
});
```

---

## OSS 存储说明

| 项目 | 说明 |
|------|------|
| **存储目录** | `item/yyyy/MM/dd/uuid.ext` |
| **示例路径** | `item/2026/06/24/a1b2c3d4e5f6.jpg` |
| **配置来源** | Nacos → `shared-oss.yaml`（`aliyun.oss` 前缀） |

---

## 后端实现参考

| 文件 | 说明 |
|------|------|
| `ym-item/.../controller/MerchantItemController.java` | 上传接口 `uploadItemImage()` |
| `ym-item/.../service/impl/OssUploadService.java` | OSS 上传通用方法 `uploadImage(file, dir)` |
| `ym-item/.../config/OssProperties.java` | OSS 配置属性类 |
