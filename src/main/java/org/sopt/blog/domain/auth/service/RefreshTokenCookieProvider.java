package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.global.security.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {

    private final JwtProperties jwtProperties;
    private final RefreshCookieProperties refreshCookieProperties;

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(
                        refreshCookieProperties.getName(),
                        refreshToken
                )
                .httpOnly(true)
                .secure(refreshCookieProperties.isSecure())
                .sameSite(refreshCookieProperties.getSameSite())
                .path(refreshCookieProperties.getPath())
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie.from(
                        refreshCookieProperties.getName(),
                        ""
                )
                .httpOnly(true)
                .secure(refreshCookieProperties.isSecure())
                .sameSite(refreshCookieProperties.getSameSite())
                .path(refreshCookieProperties.getPath())
                .maxAge(Duration.ZERO)
                .build();
    }
}
