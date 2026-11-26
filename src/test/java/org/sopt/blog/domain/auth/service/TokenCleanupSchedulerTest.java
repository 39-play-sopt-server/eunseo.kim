package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.repository.RefreshTokenRepository;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TokenCleanupSchedulerTest {

    @Test
    void 만료된_Refresh와_Access_토큰_기록을_같은_기준_시각으로_삭제한다() {
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenCleanupScheduler tokenCleanupScheduler = new TokenCleanupScheduler(
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 3, 0);

        tokenCleanupScheduler.cleanupExpiredTokens(now);

        verify(refreshTokenRepository).deleteByExpiresAtBefore(now);
        verify(revokedAccessTokenRepository).deleteByExpiresAtBefore(now);
    }
}
