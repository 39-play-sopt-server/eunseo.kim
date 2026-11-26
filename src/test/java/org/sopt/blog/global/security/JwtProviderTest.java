package org.sopt.blog.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.MemberRole;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
        );

        jwtProperties.setSecret(secret);
        jwtProperties.setIssuer("test-issuer");
        jwtProperties.setAccessTokenExpiration(1_800_000);
        jwtProperties.setRefreshTokenExpiration(1_209_600_000);

        jwtProvider = new JwtProvider(jwtProperties);
    }

    @Test
    void Access_Token을_발급하고_파싱한다() {
        String token = jwtProvider.createAccessToken(1L, MemberRole.ROLE_USER);

        TokenPayload payload = jwtProvider.parseAccessToken(token);

        assertEquals(1L, payload.memberId());
        assertEquals(TokenType.ACCESS, payload.tokenType());
        assertEquals(MemberRole.ROLE_USER, payload.role());
        assertNotNull(payload.jti());
    }

    @Test
    void Refresh_Token을_발급하고_파싱한다() {
        String token = jwtProvider.createRefreshToken(1L);

        TokenPayload payload = jwtProvider.parseRefreshToken(token);

        assertEquals(1L, payload.memberId());
        assertEquals(TokenType.REFRESH, payload.tokenType());
        assertNull(payload.role());
        assertNotNull(payload.jti());
    }

    @Test
    void Refresh_Token은_Access_Token으로_검증할_수_없다() {
        String refreshToken = jwtProvider.createRefreshToken(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtProvider.parseAccessToken(refreshToken)
        );
    }
}
