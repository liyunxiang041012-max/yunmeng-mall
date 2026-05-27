# 云梦商城 AI开发指南（精简版）

> 核心规范，每次开发前必读

## 🎯 核心开发规范（必须遵守）

### 1. Controller层
- **只做**：接收参数 → 调用Service → 返回Result
- **禁止**：业务逻辑、try-catch、参数校验（除@Validated）
- **注入**：使用 `@RequiredArgsConstructor`

```java
@RestController
@RequiredArgsConstructor
public class FileController {
    private final IUserProfileService userProfileService;
    
    @PostMapping("/user-header/upload")
    public Result<String> uploadUserHeader(@RequestParam("file") MultipartFile file) {
        return Result.success(userProfileService.uploadAvatar(file));
    }
}
```

### 2. Service层
- 业务逻辑全部在Service
- 使用 `BizException` 抛异常
- 使用构造器注入

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements IUserProfileService {
    
    private final OssUtils ossUtils;
    
    @Override
    public String uploadAvatar(MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        
        String avatarUrl = ossUtils.upload(file, OssDirConstants.USER_HEADER);
        
        UserProfile userProfile = lambdaQuery().eq(UserProfile::getId, userId).one();
        if (userProfile == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        
        userProfile.setAvatar(avatarUrl);
        userProfile.setUpdateTime(DateUtils.now());
        updateById(userProfile);
        
        return avatarUrl;
    }
}
```

### 3. Result使用

```java
// ✅ 正确
Result.success();
Result.success(data);
Result.fail(ResultCode.USER_NOT_EXIST);
Result.fail("错误消息");

// ❌ 错误
Result.error("错误");  // 不存在
```

### 4. 异常处理
- Controller不写try-catch
- 使用 `BizException` + `ResultCode`
- 全局异常处理器自动处理

```java
throw new BizException(ResultCode.UNAUTHORIZED);
throw new BizException(ResultCode.USER_NOT_EXIST);
```

## 🔧 常用工具类

| 工具类 | 用途 |
|--------|------|
| `Result` | 统一返回 |
| `BizException` | 业务异常 |
| `ResultCode` | 错误码 |
| `UserContext` | 获取当前用户 |
| `OssUtils` | OSS上传 |
| `OssDirConstants` | OSS目录常量 |

## 📝 OSS使用

```java
@Service
@RequiredArgsConstructor
public class XxxService {
    private final OssUtils ossUtils;
    
    public String uploadFile(MultipartFile file) {
        return ossUtils.upload(file, OssDirConstants.ITEM);
    }
}
```

**目录常量**：
- `ITEM` - 商品图片
- `USER_HEADER` - 用户头像
- `ITEM_DETAIL` - 商品详情
- `SHOP_LOGO` - 店铺Logo

## ⚠️ 常见错误

```java
// ❌ Controller写业务逻辑
@PostMapping("/upload")
public Result upload(MultipartFile file) {
    if (file == null) {
        return Result.error("文件为空");  // ❌ 没有error方法
    }
    try {
        String url = ossUtils.upload(file);  // ❌ 不应直接调用工具类
        return Result.success(url);
    } catch (Exception e) {  // ❌ 不应catch
        return Result.error(e.getMessage());
    }
}

// ✅ 正确写法
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    return Result.success(userProfileService.uploadAvatar(file));
}
```

## 📋 代码检查清单

- [ ] Controller只调用Service？
- [ ] 使用 `@RequiredArgsConstructor`？
- [ ] Controller没有try-catch？
- [ ] 使用 `Result.success()` / `Result.fail()`？
- [ ] 异常使用 `BizException`？

---
**完整文档**: 查看 `AI开发指南.md`  
**最后更新**: 2026-05-27
