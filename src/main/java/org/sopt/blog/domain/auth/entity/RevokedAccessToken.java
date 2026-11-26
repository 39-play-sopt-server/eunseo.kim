package org.sopt.blog.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.sopt.blog.global.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "revoked_access_tokens",
        indexes = @Index(name = "idx_revoked_access_token_expires_at", columnList = "expires_at")
)
public class RevokedAccessToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected RevokedAccessToken() {
    }

    public RevokedAccessToken(String jti, LocalDateTime expiresAt) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("토큰 식별자는 필수입니다.");
        }
        this.jti = jti;
        this.expiresAt = Objects.requireNonNull(expiresAt, "만료 시각은 필수입니다.");
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getJti() {
        return jti;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
