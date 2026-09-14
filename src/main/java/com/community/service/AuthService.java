package com.community.service;

import com.community.dto.ForgotQueryDTO;
import com.community.dto.ForgotResetDTO;
import com.community.dto.LoginDTO;
import com.community.dto.RegisterDTO;
import com.community.dto.SecurityQuestionDTO;
import com.community.dto.UpdatePasswordDTO;
import com.community.dto.UpdateProfileDTO;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import com.community.vo.SecurityQuestionVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户认证服务
 *
 * <p>Sa-Token 完成登录会话，token 存入 Redis(sa-token-redis-jackson 持久层)。
 * v3 扩展：改名、修改密码、密保问题设置与「忘记密码」找回。
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

    // ==================== v3：账号自助 ====================

    /**
     * 修改昵称(改名)，返回更新后的当前用户信息
     *
     * @param dto 新昵称
     */
    CurrentUserVO updateProfile(UpdateProfileDTO dto);

    /**
     * 修改密码(需校验原密码)
     *
     * @param dto 原密码 + 新密码
     */
    void updatePassword(UpdatePasswordDTO dto);

    /**
     * 设置个人密保问题(问题与答案均由用户自定义，答案加密存储；仅可设置一次)
     *
     * @param dto 问题与答案
     */
    void setSecurityQuestion(SecurityQuestionDTO dto);

    /**
     * 查询当前登录用户的密保问题状态(个人中心据此决定是否展示设置入口)
     */
    SecurityQuestionVO securityQuestionStatus();

    /**
     * 忘记密码-第一步：根据用户名查询其密保问题
     *
     * @param dto 用户名
     * @return hasQuestion=false 表示未设置密保问题
     */
    SecurityQuestionVO forgotQuestion(ForgotQueryDTO dto);

    /**
     * 忘记密码-第二步：校验密保答案并重置密码(带限流防爆破)
     *
     * @param dto 用户名 + 答案 + 新密码
     */
    void forgotReset(ForgotResetDTO dto);
}
