package com.community.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sa-Token 拦截器注册。 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有请求，注解鉴权自动放行未标注的公开接口(游客只读接口)
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
    }
}
