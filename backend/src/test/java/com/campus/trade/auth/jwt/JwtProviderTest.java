package com.campus.trade.auth.jwt;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 签发和解析测试。
 */
class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(
            "campus-trade-test",
            "campus-trade-test-secret-must-be-at-least-32-bytes",
            Duration.ofMinutes(30)
    ));

    @Test
    void shouldIssueAndParseToken() {
        IssuedToken issuedToken = jwtProvider.issue(1001L, 1);

        JwtClaims claims = jwtProvider.parse(issuedToken.value());

        assertThat(claims.userId()).isEqualTo(1001L);
        assertThat(claims.role()).isEqualTo(1);
        assertThat(claims.tokenId()).isEqualTo(issuedToken.tokenId());
        assertThat(issuedToken.ttl()).isEqualTo(Duration.ofMinutes(30));
    }
}
