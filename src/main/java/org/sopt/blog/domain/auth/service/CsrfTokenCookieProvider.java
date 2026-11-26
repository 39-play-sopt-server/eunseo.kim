package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.global.security.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Cookie-authenticated refresh endpoint용 double-submit CSRF 토큰을 만든다.
 * Refresh Token과 달리 이 토큰은 브라우저 JavaScript가 읽어 요청 헤더로 돌려보내야 한다.
 */
@Component
@RequiredArgsConstructor
public class CsrfTokenCookieProvider {

    public static final String COOKIE_NAME = "XSRF-TOKEN";
    public static final String HEADER_NAME = "X-XSRF-TOKEN";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final JwtProperties jwtProperties;
    private final RefreshCookieProperties refreshCookieProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ResponseCookie create() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return cookie(token, Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()));
    }

    public ResponseCookie delete() {
        return cookie("", Duration.ZERO);
    }

    public boolean matches(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null
                || cookieToken.isBlank() || headerToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8),
                headerToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(false)
                .secure(refreshCookieProperties.isSecure())
                .sameSite(refreshCookieProperties.getSameSite())
                .path(refreshCookieProperties.getPath())
                .maxAge(maxAge)
                .build();
    }
}
