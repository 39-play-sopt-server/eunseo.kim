package org.sopt.blog.domain.auth.repository;

import jakarta.persistence.LockModeType;
import org.sopt.blog.domain.auth.entity.RefreshToken;
import org.sopt.blog.domain.auth.entity.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select refreshToken from RefreshToken refreshToken where refreshToken.jti = :jti")
    Optional<RefreshToken> findByJtiForUpdate(@Param("jti") String jti);

    /**
     * Refresh Token family의 상태를 변경하기 전에 활성 토큰 전체를 잠근다.
     *
     * <p>재발급은 현재 활성 토큰을 회전시킨 뒤 같은 family의 다음 토큰을 만든다.
     * 따라서 재사용 탐지, 재발급, 로그아웃은 모두 이 조회를 통해 family 단위로
     * 직렬화되어야 한다.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select refreshToken
            from RefreshToken refreshToken
            where refreshToken.tokenFamily = :tokenFamily
              and refreshToken.status = :status
            """)
    List<RefreshToken> findAllByTokenFamilyAndStatusForUpdate(
            @Param("tokenFamily") String tokenFamily,
            @Param("status") RefreshTokenStatus status
    );

    List<RefreshToken> findAllByMember_IdAndStatus(
            Long memberId,
            RefreshTokenStatus status
    );

    long deleteByExpiresAtBefore(LocalDateTime now);
}
