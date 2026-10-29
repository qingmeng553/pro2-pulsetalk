package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.community.common.api.R;
import com.community.common.util.IpUtil;
import com.community.dto.BindEmailDTO;
import com.community.dto.ForgotQueryDTO;
import com.community.dto.ForgotResetDTO;
import com.community.dto.LoginDTO;
import com.community.dto.RebindEmailDTO;
import com.community.dto.RegisterDTO;
import com.community.dto.ResetByCodeDTO;
import com.community.dto.SecurityQuestionDTO;
import com.community.dto.SendCodeDTO;
import com.community.dto.UpdatePasswordDTO;
import com.community.dto.UpdateProfileDTO;
import com.community.service.AuthService;
import com.community.service.CaptchaService;
import com.community.service.EmailAuthService;
import com.community.vo.CaptchaVO;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import com.community.vo.SecurityQuestionVO;
import jakarta.servlet.http.HttpServletRequest;
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

/** 认证模块接口。 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final EmailAuthService emailAuthService;

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

    // ---- alpha2：图形码 / 邮箱绑定 / 邮箱找回 ----

    /** 图形验证码（公开，同 IP 限频） */
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha(HttpServletRequest request) {
        return R.ok(captchaService.generate(IpUtil.clientIp(request)));
    }

    /** 发送邮箱验证码：scene=bind/rebind 需登录，reset 公开（未绑定也返回成功） */
    @PostMapping("/email/send-code")
    public R<Void> sendEmailCode(@Valid @RequestBody SendCodeDTO dto, HttpServletRequest request) {
        emailAuthService.sendCode(dto, IpUtil.clientIp(request));
        return R.ok();
    }

    /** 首次绑定邮箱（登录 + 当前密码 + 验证码） */
    @SaCheckLogin
    @PostMapping("/email/bind")
    public R<Void> bindEmail(@Valid @RequestBody BindEmailDTO dto) {
        emailAuthService.bind(dto);
        return R.ok();
    }

    /** 换绑邮箱（登录 + 当前密码 + 新邮箱验证码） */
    @SaCheckLogin
    @PostMapping("/email/rebind")
    public R<Void> rebindEmail(@Valid @RequestBody RebindEmailDTO dto) {
        emailAuthService.rebind(dto);
        return R.ok();
    }

    /** 邮箱验证码重置密码（公开；成功后踢掉该用户全部会话） */
    @PostMapping("/forgot/reset-by-code")
    public R<Void> resetByCode(@Valid @RequestBody ResetByCodeDTO dto) {
        emailAuthService.resetByCode(dto);
        return R.ok();
    }
    // ---- v3：账号自助 ----
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
    // ---- v3：忘记密码(公开，限流保护) ----
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
