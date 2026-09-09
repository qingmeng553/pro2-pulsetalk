package com.community.service.impl;

import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.common.util.ImageFileValidator;
import com.community.common.util.MinioBucketUtil;
import com.community.config.MinioProperties;
import com.community.service.MinioFileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MinIO 对象存储服务实现
 *
 * <p>路径按业务目录/日期分桶子目录存放：{@code avatars/yyyyMM/uuid.ext}、{@code post-images/yyyyMM/uuid.ext}。
 * 上传后返回形如 {@code http://public-endpoint/bucket/object} 的公共可访问 URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileServiceImpl implements MinioFileService {

    /** 头像目录 */
    private static final String AVATAR_DIR = "avatars";
    /** 帖子配图目录 */
    private static final String POST_IMAGE_DIR = "post-images";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /** 桶是否已确认存在(惰性建桶) */
    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    @Override
    public String uploadAvatar(MultipartFile file) {
        return doUpload(file, AVATAR_DIR);
    }

    @Override
    public String uploadPostImage(MultipartFile file) {
        return doUpload(file, POST_IMAGE_DIR);
    }

    /**
     * 上传核心逻辑：校验 → 拼对象名 → 上传 → 拼公共URL
     */
    private String doUpload(MultipartFile file, String dir) {
        // 1. 格式/大小/魔数三重校验(拒绝可执行脚本伪装图片)
        String ext = ImageFileValidator.validateAndGetExt(file);

        // 2. 组装对象名: {dir}/{yyyyMM}/{uuid}.{ext}
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String objectName = dir + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            // 3. 确保桶存在(启动初始化失败时惰性重试)
            ensureBucket();

            // 4. 上传(内存字节流，文件已受 5MB 上限约束)
            byte[] bytes = file.getBytes();
            String contentType = mimeOf(ext);
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .contentType(contentType)
                        .stream(is, bytes.length, -1)
                        .build());
            }
            log.info("[MinIO] 上传成功 object={}, size={}B", objectName, bytes.length);

            // 5. 返回浏览器可访问 URL(public-endpoint/bucket/object)
            return publicUrl(objectName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MinIO] 上传失败 object={}", objectName, e);
            throw new BusinessException(ResultCode.ERROR, "图片上传失败，请稍后再试");
        }
    }

    /**
     * 幂等确认桶存在(避免每次上传都探测)
     */
    private void ensureBucket() throws Exception {
        if (bucketReady.compareAndSet(false, true)) {
            MinioBucketUtil.ensureBucketWithPublicRead(minioClient, properties.getBucket());
        }
    }

    /**
     * 拼接公共访问 URL(兼容 public-endpoint 末尾带斜杠的配置)
     */
    private String publicUrl(String objectName) {
        String base = properties.getPublicEndpoint();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + properties.getBucket() + "/" + objectName;
    }

    /** 扩展名 → Content-Type */
    private String mimeOf(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
