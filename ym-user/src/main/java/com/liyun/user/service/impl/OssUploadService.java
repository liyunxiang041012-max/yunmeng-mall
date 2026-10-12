package com.liyun.user.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.liyun.user.config.OssProperties;
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
 * 阿里云 OSS 文件上传服务（用户头像）
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
            log.info("OSS 客户端初始化成功, bucket: {}", ossProperties.getBucketName());
        } catch (Exception e) {
            log.error("OSS 客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 上传用户头像
     */
    public String uploadAvatar(MultipartFile file) {
        validateFile(file);

        String ext = getExtension(file.getOriginalFilename());
        String objectName = buildObjectName(ossProperties.getDirPrefix().getUserHeader(), ext);

        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(new PutObjectRequest(ossProperties.getBucketName(), objectName, in));
            log.info("头像上传成功: {}", objectName);
            return buildUrl(objectName);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new RuntimeException("头像上传失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("上传文件不能为空");
        if (file.getSize() > 5 * 1024 * 1024) throw new RuntimeException("文件大小不能超过5MB");
        String ct = file.getContentType();
        if (ct == null || (!ct.equalsIgnoreCase("image/jpeg") && !ct.equalsIgnoreCase("image/png")
                && !ct.equalsIgnoreCase("image/gif") && !ct.equalsIgnoreCase("image/webp"))) {
            throw new RuntimeException("只支持jpg/png/gif/webp");
        }
    }

    private String buildObjectName(String dir, String ext) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        String prefix = dir.endsWith("/") ? dir : dir + "/";
        return prefix + datePath + "/" + name;
    }

    private String buildUrl(String objectName) {
        String cdn = ossProperties.getCdnDomain();
        if (cdn != null && !cdn.isBlank()) {
            return (cdn.endsWith("/") ? cdn : cdn + "/") + objectName;
        }
        return String.format("https://%s.%s/%s", ossProperties.getBucketName(), ossProperties.getEndpoint(), objectName);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        return ".jpeg".equals(ext) ? ".jpg" : ext;
    }
}
