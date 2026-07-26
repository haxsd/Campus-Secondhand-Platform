package com.campus.trade.auth.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公开路径规则测试，重点防止同一路径上的写操作被误放行。
 */
class PublicRequestMatcherTest {

    private final PublicRequestMatcher matcher = new PublicRequestMatcher();

    @Test
    void shouldAllowPublicProductQuery() {
        MockHttpServletRequest request = request("GET", "/api/products/123");

        assertThat(matcher.isPublic(request)).isTrue();
    }

    @Test
    void shouldProtectProductWriteOperation() {
        MockHttpServletRequest request = request("PUT", "/api/products/123");

        assertThat(matcher.isPublic(request)).isFalse();
    }

    @Test
    void shouldAllowLoginButProtectCurrentUser() {
        assertThat(matcher.isPublic(request("POST", "/api/auth/login"))).isTrue();
        assertThat(matcher.isPublic(request("GET", "/api/users/me"))).isFalse();
    }

    private MockHttpServletRequest request(String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setContextPath("/api");
        request.setRequestURI(requestUri);
        return request;
    }
}
