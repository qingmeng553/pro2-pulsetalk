package com.community.config;

import com.community.common.util.MinioBucketUtil;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置
 *
 * <p>提供 MinioClient Bean，并在应用启动后自动创建桶(community-upload)与公共读策略。
 * MinIO 暂不可达时仅告警不阻塞启动(上传时会惰性重试建桶)。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    /**
     * MinIO Java SDK 客户端
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * 启动后自动初始化桶(失败仅告警，不阻塞应用启动)
     */
    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient, MinioProperties properties) {
        return args -> {
            try {
                MinioBucketUtil.ensureBucketWithPublicRead(minioClient, properties.getBucket());
                log.info("[MinIO] 桶初始化完成: {}", properties.getBucket());
            } catch (Exception e) {
                log.warn("[MinIO] 桶初始化失败(上传时将自动重试): {}", e.getMessage());
            }
        };
    }
}
