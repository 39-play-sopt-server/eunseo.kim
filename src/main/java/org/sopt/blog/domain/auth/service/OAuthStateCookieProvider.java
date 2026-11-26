package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.domain.auth.config.OAuthStateCookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class OAuthStateCookieProvider {

    private static final int STATE_BYTE_LENGTH = 32;

    private final OAuthStateCookieProperties oauthStateCookieProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String createState() {
        byte[] bytes = new byte[STATE_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public ResponseCookie create(String state) {
        return ResponseCookie.from(
                        oauthStateCookieProperties.getName(),
                        state
                )
                .httpOnly(true)
                .secure(oauthStateCookieProperties.isSecure())
                .sameSite(oauthStateCookieProperties.getSameSite())
                .path(oauthStateCookieProperties.getPath())
                .maxAge(Duration.ofSeconds(
                        oauthStateCookieProperties.getMaxAgeSeconds()
                ))
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie.from(
                        oauthStateCookieProperties.getName(),
                        ""
                )
                .httpOnly(true)
                .secure(oauthStateCookieProperties.isSecure())
                .sameSite(oauthStateCookieProperties.getSameSite())
                .path(oauthStateCookieProperties.getPath())
                .maxAge(Duration.ZERO)
                .build();
    }

    public boolean matches(String cookieState, String callbackState) {
        if (cookieState == null || callbackState == null) {
            return false;
        }

        return MessageDigest.isEqual(
                cookieState.getBytes(StandardCharsets.UTF_8),
                callbackState.getBytes(StandardCharsets.UTF_8)
        );
    }
}
