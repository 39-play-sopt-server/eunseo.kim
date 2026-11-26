package org.sopt.blog.domain.auth.entity;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTest {

    @Test
    void 새_Refresh_Token은_활성_상태이고_회전되면_비활성_상태가_된다() {
        RefreshToken token = new RefreshToken(
                new Member("은서", "eunseo@example.com", "encoded-password"),
                "123e4567-e89b-12d3-a456-426614174000",
                "a".repeat(64),
                "123e4567-e89b-12d3-a456-426614174001",
                LocalDateTime.now().plusDays(14)
        );

        assertTrue(token.isActive());

        token.rotate();

        assertFalse(token.isActive());
        assertFalse(token.isExpiredAt(LocalDateTime.now()));
    }
}
