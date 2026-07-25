package com.campus.trade.config;

import com.campus.trade.auth.interceptor.AdminInterceptor;
import com.campus.trade.auth.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局配置。
 *
 * <p>本类只负责“把拦截器注册到哪些路径、以什么顺序执行”，
 * 具体认证和管理员判断分别留在对应拦截器中。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebMvcConfig(
            LoginInterceptor loginInterceptor,
            AdminInterceptor adminInterceptor
    ) {
        // Spring 自动从容器中注入已经创建好的两个拦截器 Bean。
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    /**
     * 登录认证先执行，管理员权限后执行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器覆盖全部请求；内部 PublicRequestMatcher 决定哪些请求可以匿名访问。
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .order(0);

        // 管理员拦截器只覆盖 /admin/**，并在登录认证完成后执行。
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .order(1);
    }
}
