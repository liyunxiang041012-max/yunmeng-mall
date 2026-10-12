package com.liyun.item.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.liyun.item.config.OssProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务
 * 参考用户头像上传模式，统一使用 OSS 存储商家的店铺头像
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssUploadService {

    private final OssProperties ossProperties;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );
            log.info("阿里云 OSS 客户端初始化成功，endpoint: {}, bucket: {}",
                    ossProperties.getEndpoint(), ossProperties.getBucketName());
        } catch (Exception e) {
            log.error("阿里云 OSS 客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云 OSS 客户端已关闭");
        }
    }

    /**
     * 上传图片到 OSS
     *
     * @param file 上传的文件
     * @param dir  存储目录，如 "shop/avatar"
     * @return 图片访问 URL
     */
    public String uploadImage(MultipartFile file, String dir) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String objectName = buildObjectName(dir, extension);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    objectName,
                    inputStream
            );

            // 设置公共读权限，让前端可以直接访问
            // 如果使用 CDN + 私有Bucket，请改用签名URL方式
            // putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

            PutObjectResult result = ossClient.putObject(putObjectRequest);
            log.info("OSS 上传成功: {}, ETag: {}", objectName, result.getETag());

            return buildAccessUrl(objectName);

        } catch (IOException e) {
            log.error("OSS 上传失败，文件读取异常", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("OSS 上传失败", e);
            throw new RuntimeException("OSS上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传店铺头像
     */
    public String uploadShopAvatar(MultipartFile file) {
        return uploadImage(file, ossProperties.getDirPrefix().getItem());
    }

    /**
     * 上传用户头像 (供 ym-user 等服务扩展使用)
     */
    public String uploadUserAvatar(MultipartFile file) {
        return uploadImage(file, ossProperties.getDirPrefix().getUserHeader());
    }

    // ==================== 私有方法 ====================

    /**
     * 构建 OSS 对象名 (路径 + 文件名)
     * 格式: prefix/yyyy/MM/dd/uuid.ext
     */
    private String buildObjectName(String dir, String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 确保路径以 / 结尾
        String prefix = dir.endsWith("/") ? dir : dir + "/";
        return prefix + datePath + "/" + fileName;
    }

    /**
     * 构建访问 URL
     * 优先使用 CDN 域名，否则使用 OSS 默认域名
     */
    private String buildAccessUrl(String objectName) {
        String domain = ossProperties.getCdnDomain();
        if (domain != null && !domain.isBlank()) {
            return domain.endsWith("/") ? domain + objectName : domain + "/" + objectName;
        }
        // OSS 默认域名: https://bucket-name.endpoint/objectName
        return String.format("https://%s.%s/%s",
                ossProperties.getBucketName(),
                ossProperties.getEndpoint(),
                objectName);
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 最大 5MB
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件大小不能超过5MB");
        }

        // 只允许图片格式
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new RuntimeException("只支持 jpg、png、gif、webp 格式的图片");
        }
    }

    private boolean isAllowedImageType(String contentType) {
        return contentType.equalsIgnoreCase("image/jpeg") ||
                contentType.equalsIgnoreCase("image/png") ||
                contentType.equalsIgnoreCase("image/gif") ||
                contentType.equalsIgnoreCase("image/webp");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        // 统一 jpeg 为 jpg
        if (".jpeg".equals(ext)) {
            return ".jpg";
        }
        return ext;
    }
}
