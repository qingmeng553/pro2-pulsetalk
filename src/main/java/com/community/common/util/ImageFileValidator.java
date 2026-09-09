package com.community.common.util;

import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 图片文件校验器
 *
 * <p>约束：
 * <ul>
 *   <li>仅允许 jpg / png / gif / webp</li>
 *   <li>单文件最大 5MB</li>
 *   <li>双重校验：扩展名白名单 + 文件头魔数(拒绝伪装成图片的可执行脚本，如 .sh/.html/.js 等)</li>
 * </ul>
 */
public final class ImageFileValidator {

    private ImageFileValidator() {
    }

    /** 允许的图片格式 */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 单文件最大 5MB */
    public static final long MAX_SIZE = 5L * 1024 * 1024;

    /**
     * 校验并返回规范化扩展名（不含点）
     *
     * @param file 上传文件
     * @return 小写扩展名，如 jpg/png/gif/webp
     */
    public static String validateAndGetExt(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = (dot >= 0 ? original.substring(dot + 1) : "").toLowerCase(Locale.ROOT);
        // jpeg 统一归一为 jpg
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg/png/gif/webp 格式的图片");
        }

        // 魔数校验：防止把可执行脚本改名成图片上传
        try {
            byte[] head = file.getBytes();
            if (!matchMagic(head, ext)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "文件内容与图片格式不符，已拒绝上传");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.ERROR, "文件读取失败，请重试");
        }
        return ext;
    }

    /**
     * 校验文件头魔数与扩展名是否一致
     * <ul>
     *   <li>jpg:  FF D8 FF</li>
     *   <li>png:  89 50 4E 47 0D 0A 1A 0A</li>
     *   <li>gif:  47 49 46 38 (GIF8)</li>
     *   <li>webp: 52 49 46 46 .... 57 45 42 50 (RIFF....WEBP)</li>
     * </ul>
     */
    private static boolean matchMagic(byte[] head, String ext) {
        if (head == null || head.length < 12) {
            return false;
        }
        switch (ext) {
            case "jpg":
                return (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png":
                byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
                return Arrays.equals(Arrays.copyOf(head, 8), png);
            case "gif":
                byte[] gif = new byte[]{0x47, 0x49, 0x46, 0x38};
                return Arrays.equals(Arrays.copyOf(head, 4), gif);
            case "webp":
                // "RIFF" + size(4字节) + "WEBP"
                byte[] riff = new byte[]{0x52, 0x49, 0x46, 0x46};
                byte[] webp = new byte[]{0x57, 0x45, 0x42, 0x50};
                return Arrays.equals(Arrays.copyOf(head, 4), riff)
                        && Arrays.equals(Arrays.copyOfRange(head, 8, 12), webp);
            default:
                return false;
        }
    }
}
