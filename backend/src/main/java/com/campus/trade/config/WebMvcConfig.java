package com.campus.trade.config;

import com.campus.trade.auth.interceptor.AdminInterceptor;
import com.campus.trade.auth.interceptor.LoginInterceptor;
import com.campus.trade.file.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
    private final FileStorageService fileStorageService;

    public WebMvcConfig(
            LoginInterceptor loginInterceptor,
            AdminInterceptor adminInterceptor,
            FileStorageService fileStorageService
    ) {
        // Spring 自动从容器中注入已经创建好的两个拦截器 Bean。
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
        // 文件服务负责创建目录；MVC 配置只负责把 URL 映射到该目录。
        this.fileStorageService = fileStorageService;
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

    /**
     * 将上传目录中的图片映射为公开静态资源。
     *
     * <p>访问 /api/uploads/xxx.jpg 时，会由 Spring 读取本机上传目录中的对应文件。
     * PublicRequestMatcher 已放行 /uploads/**，浏览器加载 img 标签时无需再携带 token。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStorageService.getUploadDirectory().toUri().toString();
        // ResourceLocations 表示目录时必须以 / 结束，避免把 uploads 当作文件名而不是目录。
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
