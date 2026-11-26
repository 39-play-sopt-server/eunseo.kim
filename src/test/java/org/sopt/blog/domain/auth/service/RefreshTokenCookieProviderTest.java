package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.global.security.JwtProperties;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCookieProviderTest {

    private RefreshTokenCookieProvider refreshTokenCookieProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(1_209_600_000);

        RefreshCookieProperties refreshCookieProperties = new RefreshCookieProperties();
        refreshCookieProperties.setName("refreshToken");
        refreshCookieProperties.setPath("/auth");
        refreshCookieProperties.setSecure(true);
        refreshCookieProperties.setSameSite("Strict");

        refreshTokenCookieProvider = new RefreshTokenCookieProvider(
                jwtProperties,
                refreshCookieProperties
        );
    }

    @Test
    void Refresh_Token_쿠키를_보안_속성과_함께_생성한다() {
        ResponseCookie cookie = refreshTokenCookieProvider.create("refresh-token");

        assertEquals("refreshToken", cookie.getName());
        assertEquals("refresh-token", cookie.getValue());
        assertEquals("/auth", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofMillis(1_209_600_000), cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }

    @Test
    void Refresh_Token_쿠키를_삭제한다() {
        ResponseCookie cookie = refreshTokenCookieProvider.delete();

        assertEquals("refreshToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }
}
