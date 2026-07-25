package com.campus.trade.auth.service;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.jwt.IssuedToken;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.vo.LoginResponse;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证业务单元测试，不依赖真实数据库与 Redis。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private LoginSessionService loginSessionService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userMapper,
                passwordEncoder,
                jwtProvider,
                loginSessionService
        );
    }

    @Test
    void shouldRegisterUserAndInitializeCreditSummary() {
        RegisterRequest request = new RegisterRequest(
                "20260001",
                "13812345678",
                "Abc12345",
                "小明",
                "东校区"
        );
        when(userMapper.existsByStudentNoOrPhone("20260001", "13812345678"))
                .thenReturn(false);
        when(passwordEncoder.encode("Abc12345")).thenReturn("bcrypt-hash");

        // 模拟 MyBatis useGeneratedKeys 在插入后回填自增主键。
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        authService.register(request);

        verify(userMapper).insertCreditSummary(101L);
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest(
                "20260001",
                "13812345678",
                "Abc12345",
                "小明",
                "东校区"
        );
        when(userMapper.existsByStudentNoOrPhone("20260001", "13812345678"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BizException.class)
                .hasMessage("学号或手机号已注册");
    }

    @Test
    void shouldLoginAndCreateRedisSession() {
        User user = normalUser();
        when(userMapper.selectByAccount("20260001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Abc12345", "bcrypt-hash")).thenReturn(true);
        when(jwtProvider.issue(101L, 0)).thenReturn(new IssuedToken(
                "jwt-value",
                "token-id",
                Duration.ofDays(7)
        ));

        LoginResponse response = authService.login(new LoginRequest("20260001", "Abc12345"));

        assertThat(response.token()).isEqualTo("jwt-value");
        assertThat(response.user().id()).isEqualTo(101L);
        assertThat(response.user().nickname()).isEqualTo("小明");
        verify(loginSessionService).create("token-id", 101L, 0, Duration.ofDays(7));
    }

    @Test
    void shouldUseGenericMessageWhenPasswordIsWrong() {
        User user = normalUser();
        when(userMapper.selectByAccount("20260001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("20260001", "wrong-password")
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("账号或密码错误");
    }

    private User normalUser() {
        User user = new User();
        user.setId(101L);
        user.setStudentNo("20260001");
        user.setPhone("13812345678");
        user.setPassword("bcrypt-hash");
        user.setNickname("小明");
        user.setCampus("东校区");
        user.setRole(0);
        user.setStatus(0);
        return user;
    }
}
