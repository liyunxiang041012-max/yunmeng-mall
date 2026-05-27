# 阿里云OSS工具类使用说明

## 1. 在需要使用OSS的模块中添加依赖

在对应模块的 `pom.xml` 中添加对 `ym-common` 的依赖：

```xml
<dependency>
    <groupId>com.liyun</groupId>
    <artifactId>ym-common</artifactId>
    <version>1.0</version>
</dependency>
```

## 2. 在配置文件中添加OSS配置

### 方式一：使用Nacos配置中心（推荐）

在Nacos中创建配置文件：
- **Data ID**: `shared-oss.yaml`
- **Group**: `DEFAULT_GROUP`
- **配置格式**: `YAML`
- **配置内容**: 参考项目根目录下的 `shared-oss.yaml` 文件

在各模块的 `bootstrap.yaml` 中引入共享配置：
```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: shared-oss.yaml
            group: DEFAULT_GROUP
            refresh: true
```

### 方式二：本地配置文件

在 `application.yaml` 或 `application.properties` 中添加以下配置：

```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com  # OSS访问域名
    access-key-id: LTAI5t6DtyFWH6H8uwBZC6aU    # 访问密钥ID
    access-key-secret: JxbhyKVeaaKl0m8RzAPrHFv7sQmo1o  # 访问密钥Secret
    bucket-name: yunmengmall           # 存储空间名称
```

## 3. 在代码中使用

### 3.1 注入OssUtils

```java
@Service
public class FileService {
    
    @Autowired
    private OssUtils ossUtils;
    
    /**
     * 上传商品图片
     */
    public String uploadItemImage(MultipartFile file) {
        // 上传到 item 目录
        String fileUrl = ossUtils.upload(file, OssDirConstants.ITEM);
        return fileUrl;
    }
    
    /**
     * 上传用户头像
     */
    public String uploadUserHeader(MultipartFile file) {
        // 上传到 user-header 目录
        String fileUrl = ossUtils.upload(file, OssDirConstants.USER_HEADER);
        return fileUrl;
    }
    
    /**
     * 上传文件到指定目录（自定义文件名）
     */
    public String uploadWithCustomName(MultipartFile file) {
        // 上传到 user-header 目录，使用自定义文件名
        String customName = "user_123_header"; // 不含扩展名
        String fileUrl = ossUtils.uploadToDir(file, OssDirConstants.USER_HEADER, customName);
        return fileUrl;
    }
    
    /**
     * 上传字节数组
     */
    public String uploadBytes(byte[] bytes, String fileName, String contentType) {
        return ossUtils.upload(bytes, fileName, contentType);
    }
    
    /**
     * 删除文件
     */
    public void deleteFile(String fileUrl) {
        ossUtils.delete(fileUrl);
    }
    
    /**
     * 获取签名URL(用于私有bucket)
     */
    public String getPresignedUrl(String fileName) {
        // 过期时间: 1小时(3600000毫秒)
        return ossUtils.getPresignedUrl(fileName, 3600000);
    }
}
```

### 3.2 文件上传示例(Controller层)

```java
@RestController
@RequestMapping("/file")
public class FileController {
    
    @Autowired
    private OssUtils ossUtils;
    
    /**
     * 上传商品图片
     */
    @PostMapping("/item/upload")
    public Result<String> uploadItem(@RequestParam("file") MultipartFile file) {
        try {
            // 上传到 item 目录
            String fileUrl = ossUtils.upload(file, OssDirConstants.ITEM);
            return Result.success(fileUrl);
        } catch (Exception e) {
            return Result.error("商品图片上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传用户头像
     */
    @PostMapping("/user-header/upload")
    public Result<String> uploadUserHeader(@RequestParam("file") MultipartFile file) {
        try {
            // 上传到 user-header 目录
            String fileUrl = ossUtils.upload(file, OssDirConstants.USER_HEADER);
            return Result.success(fileUrl);
        } catch (Exception e) {
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传商品图片（指定子目录）
     */
    @PostMapping("/item/detail/upload")
    public Result<String> uploadItemDetail(@RequestParam("file") MultipartFile file) {
        try {
            // 上传到 item/detail 目录
            String fileUrl = ossUtils.upload(file, OssDirConstants.ITEM_DETAIL);
            return Result.success(fileUrl);
        } catch (Exception e) {
            return Result.error("商品详情图片上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("fileUrl") String fileUrl) {
        try {
            ossUtils.delete(fileUrl);
            return Result.success();
        } catch (Exception e) {
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }
}
```

## 4. 主要功能说明

### 4.1 文件上传
- 支持 `MultipartFile` 直接上传
- 支持 `MultipartFile` 指定目录前缀上传
- 支持 `MultipartFile` 指定目录和自定义文件名上传
- 支持 `InputStream` 上传
- 支持 `byte[]` 上传
- 自动生成按日期组织的文件路径: `dirPrefix/yyyy/MM/dd/uuid.extension`

### 4.2 目录前缀管理
项目提供了 `OssDirConstants` 常量类来管理常用的目录前缀：
- `OssDirConstants.ITEM` - 商品图片目录 (item)
- `OssDirConstants.USER_HEADER` - 用户头像目录 (user-header)
- `OssDirConstants.ITEM_DETAIL` - 商品详情目录 (item/detail)
- `OssDirConstants.SHOP_LOGO` - 店铺Logo目录 (shop/logo)

你也可以直接使用字符串作为目录前缀，例如：
```java
ossUtils.upload(file, "item");
ossUtils.upload(file, "user-header");
```

### 4.3 文件删除
- 支持通过完整URL删除文件
- 自动从URL中提取文件路径

### 4.4 签名URL
- 用于私有Bucket的文件访问
- 可设置过期时间

### 4.5 文件存在性检查
- 判断文件是否已存在于OSS中

## 5. 注意事项

1. **安全性**: 不要将 `accessKeyId` 和 `accessKeySecret` 硬编码在代码中，应该使用配置中心或环境变量
2. **资源释放**: `OssUtils` 实现了自动初始化，在应用关闭时会自动释放资源
3. **异常处理**: 所有操作都会抛出 `RuntimeException`，请在业务层做好异常处理
4. **文件大小**: 注意Spring Boot默认的文件上传大小限制，如需上传大文件需要调整配置

## 6. Nacos配置示例

### 6.1 创建Nacos配置文件

在Nacos控制台创建配置：
- **Data ID**: `shared-oss.yaml`
- **Group**: `DEFAULT_GROUP`
- **描述**: 阿里云OSS共享配置

### 6.2 配置文件内容

```yaml
# 阿里云OSS配置
aliyun:
  oss:
    # OSS访问域名（根据实际区域修改）
    endpoint: oss-cn-hangzhou.aliyuncs.com
    
    # 访问密钥（建议使用环境变量）
    access-key-id: ${OSS_ACCESS_KEY_ID:your-access-key-id}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET:your-access-key-secret}
    
    # 存储空间名称
    bucket-name: ${OSS_BUCKET_NAME:your-bucket-name}

# 文件上传配置
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

### 6.3 多环境配置示例

**开发环境 (dev)**:
```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: dev-access-key-id
    access-key-secret: dev-access-key-secret
    bucket-name: dev-bucket-name
```

**测试环境 (test)**:
```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: test-access-key-id
    access-key-secret: test-access-key-secret
    bucket-name: test-bucket-name
```

**生产环境 (prod)**:
```yaml
aliyun:
  oss:
    # 生产环境建议使用内网endpoint
    endpoint: oss-cn-hangzhou-internal.aliyuncs.com
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    bucket-name: prod-bucket-name
```

### 6.4 在各模块中引入配置

在 `ym-item`、`ym-user` 等模块的 `bootstrap.yaml` 中添加：

```yaml
spring:
  application:
    name: ym-item
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos
      config:
        file-extension: yaml
        # 引入共享配置
        shared-configs:
          - data-id: shared-oss.yaml
            group: DEFAULT_GROUP
            refresh: true
```
