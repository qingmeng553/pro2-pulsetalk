package com.community.common.util;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;

/**
 * MinIO 桶初始化工具
 *
 * <p>自动创建业务桶并设置“公共只读(GetObject)”桶策略，
 * 使后端返回的图片 URL 可直接被浏览器 <img> 访问。
 */
public final class MinioBucketUtil {

    private MinioBucketUtil() {
    }

    /**
     * 幂等初始化桶：不存在则创建，并确保公共读策略
     *
     * @param client MinioClient
     * @param bucket 桶名称
     * @throws Exception MinIO 连接异常时抛出，由调用方决定容错策略
     */
    public static void ensureBucketWithPublicRead(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        // 桶策略：允许匿名 GetObject(公共只读)
        String policy = "{\n"
                + "  \"Version\": \"2012-10-17\",\n"
                + "  \"Statement\": [{\n"
                + "    \"Effect\": \"Allow\",\n"
                + "    \"Principal\": {\"AWS\": [\"*\"]},\n"
                + "    \"Action\": [\"s3:GetObject\"],\n"
                + "    \"Resource\": [\"arn:aws:s3:::" + bucket + "/*\"]\n"
                + "  }]\n"
                + "}";
        client.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(policy)
                .build());
    }
}
