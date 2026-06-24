package com.liyun.item.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传工具类
 */
@Slf4j
public class FileUploadUtils {

    // 默认上传目录
    private static final String UPLOAD_DIR = "uploads/shop/";
    
    // 允许的图片类型
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    
    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传店铺头像
     * @param file 上传的文件
     * @return 文件访问URL
     */
    public static String uploadShopAvatar(MultipartFile file) {
        // 验证文件
        validateFile(file);
        
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            
            // 构建完整路径
            String uploadPath = UPLOAD_DIR + System.currentTimeMillis() + "/";
            Path dirPath = Paths.get(uploadPath);
            
            // 创建目录
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            // 保存文件
            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());
            
            // 返回访问URL (实际项目中应该返回OSS地址或CDN地址)
            String fileUrl = "/api/" + uploadPath + fileName;
            log.info("店铺头像上传成功: {}", fileUrl);
            
            return fileUrl;
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传的文件
     */
    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过5MB");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new RuntimeException("无法识别文件类型");
        }
        
        boolean allowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equalsIgnoreCase(contentType)) {
                allowed = true;
                break;
            }
        }
        
        if (!allowed) {
            throw new RuntimeException("只支持jpg、png、gif、webp格式的图片");
        }
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
