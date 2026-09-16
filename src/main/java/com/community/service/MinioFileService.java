package com.community.service;

import org.springframework.web.multipart.MultipartFile;

/** MinIO 对象存储服务。 */
public interface MinioFileService {

    /**
     * 上传用户头像
     *
     * @param file 图片文件(jpg/png/gif/webp，≤5MB)
     * @return 可访问的图片 URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 上传帖子配图
     *
     * @param file 图片文件(jpg/png/gif/webp，≤5MB)
     * @return 可访问的图片 URL
     */
    String uploadPostImage(MultipartFile file);
}
