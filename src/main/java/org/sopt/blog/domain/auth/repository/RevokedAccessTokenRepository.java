package org.sopt.blog.domain.auth.repository;

import org.sopt.blog.domain.auth.entity.RevokedAccessToken;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, Long> {

    boolean existsByJti(String jti);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
