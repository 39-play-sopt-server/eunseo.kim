package org.sopt.blog.domain.auth.service;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
