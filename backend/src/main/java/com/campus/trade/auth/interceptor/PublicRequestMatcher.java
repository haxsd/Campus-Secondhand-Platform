package com.campus.trade.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 公开请求判定器。
 *
 * <p>商品公开查询与卖家写操作会共用 /products 路径，不能仅按路径整段放行。
 * 将 HTTP 方法与精确路径一起判断，可避免未来新增写接口后被意外绕过认证。</p>
 *
 * <p>例如 GET /products/1 是公开商品详情，而 PUT /products/1 是卖家编辑。
 * 如果只按“/products/**”放行，攻击者无需登录就能调用编辑接口。</p>
 */
@Component
public class PublicRequestMatcher {

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/auth/register",
            "/auth/login"
    );

    private static final Pattern PRODUCT_DETAIL_PATTERN = Pattern.compile("^/products/\\d+$");
    private static final Pattern SELLER_REVIEW_PATTERN = Pattern.compile("^/reviews/seller/\\d+$");

    public boolean isPublic(HttpServletRequest request) {
        // request URI 包含 context-path=/api，先转换为应用内部路径，例如 /auth/login。
        String method = request.getMethod();
        String path = pathWithinApplication(request);

        // 浏览器跨域预检请求不携带业务 token，必须允许通过。
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        // 错误页、健康检查和上传文件访问不经过登录校验。
        if ("/error".equals(path) || path.startsWith("/actuator/") || path.startsWith("/uploads/")) {
            return true;
        }

        // POST 默认都是写操作，仅显式列出的注册和登录允许匿名调用。
        if (HttpMethod.POST.matches(method) && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }

        // 除上述两个 POST 外，其他非 GET 请求默认都要求登录。
        if (!HttpMethod.GET.matches(method)) {
            return false;
        }

        // GET 请求也采用白名单，防止未来新增的敏感查询接口被默认公开。
        return "/categories".equals(path)
                || "/products".equals(path)
                || PRODUCT_DETAIL_PATTERN.matcher(path).matches()
                || SELLER_REVIEW_PATTERN.matcher(path).matches();
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        // /api/auth/login 去掉 /api 后得到 /auth/login，便于规则不依赖部署前缀。
        return requestUri.substring(contextPath.length());
    }
}
