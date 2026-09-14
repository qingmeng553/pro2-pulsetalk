package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.community.common.api.R;
import com.community.dto.ForgotQueryDTO;
import com.community.dto.ForgotResetDTO;
import com.community.dto.LoginDTO;
import com.community.dto.RegisterDTO;
import com.community.dto.SecurityQuestionDTO;
import com.community.dto.UpdatePasswordDTO;
import com.community.dto.UpdateProfileDTO;
import com.community.service.AuthService;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import com.community.vo.SecurityQuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证模块接口
 *
 * <pre>
 * POST /auth/register           用户注册
 * POST /auth/login              登录，返回 sa-token
 * POST /auth/logout             登出(需token)
 * GET  /auth/current-user       获取当前登录用户(返回role角色)
 * POST /auth/upload-avatar      上传头像(需登录，返回 minio 图片 url)
 * ---- v3 账号自助 ----
 * PUT  /auth/profile            修改昵称(改名，需登录)
 * PUT  /auth/password           修改密码(需登录，校验原密码)
 * GET  /auth/security-question  查询密保问题状态(需登录)
 * POST /auth/security-question  设置个人密保问题(需登录，仅可设置一次)
 * ---- v3 忘记密码(公开，带限流) ----
 * POST /auth/forgot/question    根据用户名获取密保问题
 * POST /auth/forgot/reset       校验密保答案并重置密码
 * </pre>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 用户注册 */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok();
    }

    /** 登录：返回 sa-token(token 已写入 Redis) */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    /** 登出(需登录) */
    @SaCheckLogin
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /** 获取当前登录用户(含 role，供前端渲染管理员徽章) */
    @SaCheckLogin
    @GetMapping("/current-user")
    public R<CurrentUserVO> currentUser() {
        return R.ok(authService.currentUser());
    }

    /** 上传头像(需登录)，返回 MinIO 图片 URL */
    @SaCheckLogin
    @PostMapping("/upload-avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(authService.uploadAvatar(file));
    }

    // ==================== v3：账号自助 ====================

    /** 修改昵称(改名) */
    @SaCheckLogin
    @PutMapping("/profile")
    public R<CurrentUserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return R.ok(authService.updateProfile(dto));
    }

    /** 修改密码(校验原密码) */
    @SaCheckLogin
    @PutMapping("/password")
    public R<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        authService.updatePassword(dto);
        return R.ok();
    }

    /** 查询密保问题状态(个人中心据 hasQuestion 决定是否展示设置入口) */
    @SaCheckLogin
    @GetMapping("/security-question")
    public R<SecurityQuestionVO> securityQuestion() {
        return R.ok(authService.securityQuestionStatus());
    }

    /** 设置个人密保问题(问题与答案由用户自定义，答案加密存储；仅可设置一次) */
    @SaCheckLogin
    @PostMapping("/security-question")
    public R<Void> setSecurityQuestion(@Valid @RequestBody SecurityQuestionDTO dto) {
        authService.setSecurityQuestion(dto);
        return R.ok();
    }

    // ==================== v3：忘记密码(公开，限流保护) ====================

    /** 忘记密码第一步：根据用户名查询密保问题 */
    @PostMapping("/forgot/question")
    public R<SecurityQuestionVO> forgotQuestion(@Valid @RequestBody ForgotQueryDTO dto) {
        return R.ok(authService.forgotQuestion(dto));
    }

    /** 忘记密码第二步：校验密保答案并重置密码 */
    @PostMapping("/forgot/reset")
    public R<Void> forgotReset(@Valid @RequestBody ForgotResetDTO dto) {
        authService.forgotReset(dto);
        return R.ok();
    }
}
