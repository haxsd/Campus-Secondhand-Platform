package com.campus.trade.auth.integration;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.jwt.JwtClaims;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.service.LoginSessionService;
import com.campus.trade.auth.vo.CurrentUserVO;
import com.campus.trade.auth.vo.LoginResponse;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证完整流程集成测试。
 *
 * <p>数据库事务在测试结束后自动回滚；Redis 登录态在 finally 中主动删除，
 * 因此不会向开发环境遗留测试用户或 token。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "runExternalTests", matches = "true")
class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private LoginSessionService loginSessionService;

    @Test
    void shouldRegisterLoginReadCurrentUserAndLogout() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String studentNo = "T" + unique;
        String phone = "199" + String.format("%08d", Math.floorMod(unique.hashCode(), 100_000_000));

        authService.register(new RegisterRequest(
                studentNo,
                phone,
                "Test1234",
                "集成测试用户",
                "东校区"
        ));

        LoginResponse loginResponse = authService.login(new LoginRequest(studentNo, "Test1234"));
        JwtClaims claims = jwtProvider.parse(loginResponse.token());

        try {
            assertThat(loginSessionService.isActive(claims)).isTrue();

            UserContext.set(new CurrentUser(claims.userId(), claims.role(), claims.tokenId()));
            CurrentUserVO currentUser = authService.currentUser();
            assertThat(currentUser.studentNo()).isEqualTo(studentNo);

            authService.logout();
            assertThat(loginSessionService.isActive(claims)).isFalse();
        } finally {
            // 即使中途断言失败，也清理 ThreadLocal 与 Redis 测试登录态。
            UserContext.clear();
            loginSessionService.delete(claims.tokenId());
        }
    }
}
