package org.sopt.blog.domain.auth.entity;

import java.util.Locale;

public enum OAuthProvider {
    KAKAO;

    public static OAuthProvider from(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("소셜 로그인 제공자는 필수입니다.");
        }

        try {
            return valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        }
    }
}
