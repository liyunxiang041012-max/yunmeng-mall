package com.liyun.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 * 提供文件上传、下载、删除等操作
 *
 * @author yunmeng
 * @date 2026-05-27
 */
@Data
public class OssUtils {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    private OSS ossClient;

    /**
     * 构造函数
     *
     * @param endpoint        OSS访问域名
     * @param accessKeyId     访问密钥ID
     * @param accessKeySecret 访问密钥Secret
     * @param bucketName      存储空间名称
     */
    public OssUtils(String endpoint, String accessKeyId, String accessKeySecret, String bucketName) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
        init();
    }

    /**
     * 初始化OSS客户端
     */
    private void init() {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 上传文件到OSS
     *
     * @param file 文件
     * @return 文件访问URL
     */
    public String upload(MultipartFile file) {
        return upload(file, null);
    }

    /**
     * 上传文件到OSS（指定目录前缀）
     *
     * @param file        文件
     * @param dirPrefix   目录前缀（如：item、user-header）
     * @return 文件访问URL
     */
    public String upload(MultipartFile file, String dirPrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            // 生成文件路径
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 按日期生成目录: dirPrefix/yyyy/MM/dd/uuid.extension
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName;
            if (StringUtils.isNotBlank(dirPrefix)) {
                fileName = dirPrefix + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
            } else {
                fileName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
            }

            return upload(inputStream, fileName, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 上传文件到OSS
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名(包含路径)
     * @param contentType 文件类型
     * @param size        文件大小
     * @return 文件访问URL
     */
    public String upload(InputStream inputStream, String fileName, String contentType, long size) {
        try {
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            if (contentType != null) {
                metadata.setContentType(contentType);
            }

            // 上传文件
            ossClient.putObject(bucketName, fileName, inputStream, metadata);

            // 返回文件访问URL
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + fileName, e);
        }
    }

    /**
     * 上传字节数组到OSS
     *
     * @param bytes       字节数组
     * @param fileName    文件名(包含路径)
     * @param contentType 文件类型
     * @return 文件访问URL
     */
    public String upload(byte[] bytes, String fileName, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return upload(inputStream, fileName, contentType, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + fileName, e);
        }
    }

    /**
     * 上传文件到指定目录
     *
     * @param file        文件
     * @param dirPrefix   目录前缀（如：item、user-header）
     * @param customName  自定义文件名（不含路径，可为null，为null时自动生成）
     * @return 文件访问URL
     */
    public String uploadToDir(MultipartFile file, String dirPrefix, String customName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName;
            if (StringUtils.isNotBlank(customName)) {
                // 使用自定义文件名
                fileName = dirPrefix + "/" + customName + extension;
            } else {
                // 生成UUID文件名
                String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                fileName = dirPrefix + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
            }

            return upload(inputStream, fileName, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除OSS中的文件
     *
     * @param fileUrl 文件完整URL
     */
    public void delete(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return;
        }

        try {
            // 从URL中提取文件路径
            String key = extractObjectKey(fileUrl);
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + fileUrl, e);
        }
    }

    /**
     * 获取文件签名URL(用于私有bucket)
     *
     * @param fileName 文件名(包含路径)
     * @param expires  过期时间(毫秒)
     * @return 签名URL
     */
    public String getPresignedUrl(String fileName, long expires) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expires);
            URL url = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("获取签名URL失败: " + fileName, e);
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param fileName 文件名(包含路径)
     * @return 是否存在
     */
    public boolean doesObjectExist(String fileName) {
        try {
            return ossClient.doesObjectExist(bucketName, fileName);
        } catch (Exception e) {
            throw new RuntimeException("检查文件存在性失败: " + fileName, e);
        }
    }

    /**
     * 从完整URL中提取Object Key
     *
     * @param fileUrl 文件URL
     * @return Object Key
     */
    private String extractObjectKey(String fileUrl) {
        // https://bucketName.endpoint/key
        String prefix = "https://" + bucketName + "." + endpoint + "/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        // 如果已经是key格式，直接返回
        return fileUrl;
    }

    /**
     * 关闭OSS客户端
     */
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
