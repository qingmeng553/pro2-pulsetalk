package com.community.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码工具：盐值 + SHA-256 单向散列，结果格式为 {@code salt$sha256hex}
 *
 * <p>散列口径：{@code sha256( salt + rawPassword )}，盐值为 16 字节随机数转十六进制。
 * 验证时按相同口径重算并做常量时间比较，防时序侧信道。
 *
 * <p>说明：本实现刻意不引入 Spring Security，避免重型依赖；
 * 生产环境可平滑升级为 BCrypt/Argon2(仅替换本类实现与 sql/init.sql 中的预置散列)。
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /** 分隔符：盐值与散列值之间的分隔符 */
    private static final String SEP = "$";

    /** 随机盐生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 密码加密：生成随机盐并计算散列
     *
     * @param rawPassword 明文密码
     * @return salt$sha256hex
     */
    public static String encode(String rawPassword) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        return saltHex + SEP + sha256Hex(saltHex + rawPassword);
    }

    /**
     * 密码校验
     *
     * @param rawPassword    明文密码
     * @param encodedPassword 数据库存储的 salt$sha256hex
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        int idx = encodedPassword.lastIndexOf(SEP);
        if (idx <= 0 || idx == encodedPassword.length() - 1) {
            return false;
        }
        String saltHex = encodedPassword.substring(0, idx);
        String expect = encodedPassword.substring(idx + 1);
        String actual = sha256Hex(saltHex + rawPassword);
        // 常量时间比较
        return MessageDigest.isEqual(expect.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    /** SHA-256 十六进制 */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // JDK 必然内置 SHA-256，不会走到这里
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}
