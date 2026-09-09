package com.community.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器注册
 *
 * <p>注册综合拦截器 SaInterceptor（默认开启注解鉴权）后，
 * 控制器方法上的 @SaCheckLogin / @SaCheckRole("admin") 才会生效。
 * 未登录/无权限的异常由 {@code GlobalExceptionHandler} 统一转成 401/403 业务码。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有请求，注解鉴权自动放行未标注的公开接口(游客只读接口)
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
    }
}
