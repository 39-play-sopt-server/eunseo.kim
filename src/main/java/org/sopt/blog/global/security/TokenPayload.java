package org.sopt.blog.global.security;

import org.sopt.blog.domain.member.entity.MemberRole;

import java.time.Instant;

public record TokenPayload(
        Long memberId,
        String jti,
        TokenType tokenType,
        MemberRole role,
        Instant issuedAt,
        Instant expiresAt
) {
}
