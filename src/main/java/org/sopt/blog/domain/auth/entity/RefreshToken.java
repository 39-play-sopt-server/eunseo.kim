package org.sopt.blog.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.global.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 36)
    private String tokenFamily;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefreshTokenStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected RefreshToken() {
    }

    public RefreshToken(
            Member member,
            String jti,
            String tokenHash,
            String tokenFamily,
            LocalDateTime expiresAt
    ) {
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
        this.jti = requireText(jti, "토큰 식별자는 필수입니다.");
        this.tokenHash = requireText(tokenHash, "토큰 해시는 필수입니다.");
        this.tokenFamily = requireText(tokenFamily, "토큰 패밀리는 필수입니다.");
        this.expiresAt = Objects.requireNonNull(expiresAt, "만료 시각은 필수입니다.");
        this.status = RefreshTokenStatus.ACTIVE;
    }

    public void rotate() {
        this.status = RefreshTokenStatus.ROTATED;
    }

    public void revoke() {
        this.status = RefreshTokenStatus.REVOKED;
    }

    public boolean isActive() {
        return status == RefreshTokenStatus.ACTIVE;
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

    public String getTokenHash() {
        return tokenHash;
    }

    public String getTokenFamily() {
        return tokenFamily;
    }

    public Member getMember() {
        return member;
    }

    public RefreshTokenStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
