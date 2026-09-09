package com.community.service;

import com.community.dto.LoginDTO;
import com.community.dto.RegisterDTO;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户认证服务
 *
 * <p>Sa-Token 完成登录会话，token 存入 Redis(sa-token-redis-jackson 持久层)。
 */
public interface AuthService {

    /**
     * 用户注册(默认 USER 角色，密码加密存储)
     *
     * @param dto 注册参数
     */
    void register(RegisterDTO dto);

    /**
     * 登录：校验密码并签发 sa-token
     *
     * @param dto 登录参数
     * @return token + 用户信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 登出：注销当前登录会话(token 失效)
     */
    void logout();

    /**
     * 获取当前登录用户信息(含 role)
     */
    CurrentUserVO currentUser();

    /**
     * 上传头像(需登录)，更新用户头像并返回 MinIO URL
     */
    String uploadAvatar(MultipartFile file);
}
