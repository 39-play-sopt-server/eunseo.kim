package org.sopt.blog.domain.auth.client;

public record OAuthUser(
        String providerUserId,
        String nickname
) {
}