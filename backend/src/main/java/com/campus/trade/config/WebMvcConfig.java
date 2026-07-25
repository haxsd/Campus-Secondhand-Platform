package com.campus.trade.config;

import com.campus.trade.auth.interceptor.AdminInterceptor;
import com.campus.trade.auth.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebMvcConfig(
            LoginInterceptor loginInterceptor,
            AdminInterceptor adminInterceptor
    ) {
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    /**
     * 登录认证先执行，管理员权限后执行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .order(1);
    }
}
