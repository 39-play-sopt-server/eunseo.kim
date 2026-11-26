package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.global.security.JwtProperties;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrfTokenCookieProviderTest {

    private CsrfTokenCookieProvider csrfTokenCookieProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(1_209_600_000);

        RefreshCookieProperties refreshCookieProperties = new RefreshCookieProperties();
        refreshCookieProperties.setPath("/auth");
        refreshCookieProperties.setSecure(true);
        refreshCookieProperties.setSameSite("Strict");

        csrfTokenCookieProvider = new CsrfTokenCookieProvider(
                jwtProperties,
                refreshCookieProperties
        );
    }

    @Test
    void JavaScript가_읽을_수_있는_Strict_Secure_CSRF_쿠키를_생성한다() {
        ResponseCookie cookie = csrfTokenCookieProvider.create();

        assertEquals("XSRF-TOKEN", cookie.getName());
        assertTrue(cookie.getValue().length() >= 32);
        assertEquals("/auth", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertTrue(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
    }

    @Test
    void 서로_다른_요청마다_새_CSRF_토큰을_생성하고_동일한_헤더만_검증한다() {
        String firstToken = csrfTokenCookieProvider.create().getValue();
        String secondToken = csrfTokenCookieProvider.create().getValue();

        assertNotEquals(firstToken, secondToken);
        assertTrue(csrfTokenCookieProvider.matches(firstToken, firstToken));
        assertFalse(csrfTokenCookieProvider.matches(firstToken, secondToken));
        assertFalse(csrfTokenCookieProvider.matches(firstToken, null));
        assertFalse(csrfTokenCookieProvider.matches(null, firstToken));
    }
}
