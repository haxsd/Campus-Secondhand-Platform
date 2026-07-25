package com.campus.trade.auth.interceptor;

import com.campus.trade.auth.jwt.JwtClaims;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.service.LoginSessionService;
import com.campus.trade.common.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 登录拦截器的可选认证测试。
 *
 * <p>商品详情既允许匿名访问，也需要在已登录用户访问时记录浏览历史，
 * 因此必须验证“公开接口无 token 放行、有合法 token 仍建立 UserContext”这两个分支。</p>
 */
@ExtendWith(MockitoExtension.class)
class LoginInterceptorTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private LoginSessionService loginSessionService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldAllowAnonymousAccessToPublicProductDetail() throws Exception {
        LoginInterceptor interceptor = newInterceptor();
        MockHttpServletRequest request = productDetailRequest();

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(UserContext.get()).isEmpty();
    }

    @Test
    void shouldAuthenticateUserWhenPublicProductDetailCarriesValidToken() throws Exception {
        LoginInterceptor interceptor = newInterceptor();
        MockHttpServletRequest request = productDetailRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        JwtClaims claims = new JwtClaims(7L, 0, "token-id");
        when(jwtProvider.parse("valid-token")).thenReturn(claims);
        when(loginSessionService.isActive(claims)).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(UserContext.requireCurrentUser().userId()).isEqualTo(7L);
    }

    private LoginInterceptor newInterceptor() {
        return new LoginInterceptor(
                new PublicRequestMatcher(),
                jwtProvider,
                loginSessionService,
                new ObjectMapper()
        );
    }

    private MockHttpServletRequest productDetailRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/10");
        request.setContextPath("/api");
        return request;
    }
}
