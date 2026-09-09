package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.community.common.api.R;
import com.community.dto.LoginDTO;
import com.community.dto.RegisterDTO;
import com.community.service.AuthService;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证模块接口
 *
 * <pre>
 * POST /auth/register        用户注册
 * POST /auth/login           登录，返回 sa-token
 * POST /auth/logout          登出(需token)
 * GET  /auth/current-user    获取当前登录用户(返回role角色)
 * POST /auth/upload-avatar   上传头像(需登录，返回 minio 图片 url)
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
}
