package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.domain.auth.repository.RefreshTokenRepository;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Scheduled(cron = "${jwt.token-cleanup-cron}")
    @Transactional
    public void cleanupExpiredTokens() {
        cleanupExpiredTokens(LocalDateTime.now());
    }

    void cleanupExpiredTokens(LocalDateTime now) {
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        revokedAccessTokenRepository.deleteByExpiresAtBefore(now);
    }
}
