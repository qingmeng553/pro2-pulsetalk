package com.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置项(minio.*)
 *
 * <p>endpoint(内部连接地址) 与 public-endpoint(返回给浏览器的地址) 分离：
 * 容器环境内部用 http://minio:9000，而浏览器访问宿主机映射端口 localhost:9000。
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** SDK 连接地址(后端 → MinIO)，如 http://127.0.0.1:9000 */
    private String endpoint;

    /** 返回给浏览器访问的图片 URL 前缀(公网部署改为外网可访问地址) */
    private String publicEndpoint;

    /** Access Key */
    private String accessKey;

    /** Secret Key */
    private String secretKey;

    /** 桶名称(自动创建) */
    private String bucket;
}
