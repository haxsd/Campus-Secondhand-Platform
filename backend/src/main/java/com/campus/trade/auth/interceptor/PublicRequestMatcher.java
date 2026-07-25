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
        String method = request.getMethod();
        String path = pathWithinApplication(request);

        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        if ("/error".equals(path) || path.startsWith("/actuator/") || path.startsWith("/uploads/")) {
            return true;
        }
        if (HttpMethod.POST.matches(method) && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }
        if (!HttpMethod.GET.matches(method)) {
            return false;
        }

        return "/categories".equals(path)
                || "/products".equals(path)
                || PRODUCT_DETAIL_PATTERN.matcher(path).matches()
                || SELLER_REVIEW_PATTERN.matcher(path).matches();
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestUri.substring(contextPath.length());
    }
}
