package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.JwtException;
import org.sopt.blog.domain.auth.entity.RefreshTokenStatus;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.sopt.blog.domain.auth.entity.RefreshToken;
import org.sopt.blog.domain.auth.repository.RefreshTokenRepository;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.global.security.JwtProvider;
import org.sopt.blog.global.security.TokenHashProvider;
import org.sopt.blog.global.security.TokenPayload;
import org.sopt.blog.domain.auth.entity.RevokedAccessToken;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final TokenHashProvider tokenHashProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Transactional
    public TokenPair issueTokenPair(Member member) {
        return issueTokenPair(member, UUID.randomUUID().toString());
    }

    private TokenPair issueTokenPair(Member member, String tokenFamily) {
        String accessToken = jwtProvider.createAccessToken(
                member.getId(),
                member.getRole()
        );
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        TokenPayload refreshPayload = jwtProvider.parseRefreshToken(refreshToken);

        RefreshToken savedRefreshToken = new RefreshToken(
                member,
                refreshPayload.jti(),
                tokenHashProvider.hash(refreshToken),
                tokenFamily,
                LocalDateTime.ofInstant(
                        refreshPayload.expiresAt(),
                        ZoneId.systemDefault()
                )
        );

        refreshTokenRepository.save(savedRefreshToken);

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional(noRollbackFor = CustomException.class)
    public TokenPair reissue(String rawRefreshToken) {
        TokenPayload payload = parseRefreshToken(rawRefreshToken);

        RefreshToken savedRefreshToken = refreshTokenRepository.findByJtiForUpdate(payload.jti())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.INVALID_REFRESH_TOKEN,
                        "저장된 Refresh Token이 없습니다."
                ));

        lockActiveTokenFamily(savedRefreshToken.getTokenFamily());

        boolean hashMatches = savedRefreshToken.getTokenHash()
                .equals(tokenHashProvider.hash(rawRefreshToken));

        boolean memberMatches = savedRefreshToken.getMember()
                .getId()
                .equals(payload.memberId());

        if (!savedRefreshToken.isActive()
                || savedRefreshToken.isExpiredAt(LocalDateTime.now())
                || !hashMatches
                || !memberMatches
                || savedRefreshToken.getMember().isDeleted()) {
            revokeTokenFamily(savedRefreshToken.getTokenFamily());

            throw new CustomException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh Token을 재사용할 수 없습니다."
            );
        }

        savedRefreshToken.rotate();

        return issueTokenPair(
                savedRefreshToken.getMember(),
                savedRefreshToken.getTokenFamily()
        );
    }

    @Transactional
    public void logout(String rawAccessToken, String rawRefreshToken) {
        TokenPayload accessPayload = parseAccessToken(rawAccessToken);

        blacklistAccessToken(accessPayload);
        revokeRefreshTokenFamily(rawRefreshToken, accessPayload.memberId());
    }

    @Transactional
    public void revokeAllRefreshTokens(Long memberId) {
        refreshTokenRepository.findAllByMember_IdAndStatus(
                memberId,
                RefreshTokenStatus.ACTIVE
        ).stream()
                .map(RefreshToken::getTokenFamily)
                .distinct()
                .forEach(this::revokeTokenFamily);
    }

    private TokenPayload parseRefreshToken(String rawRefreshToken) {
        try {
            return jwtProvider.parseRefreshToken(rawRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh Token 검증에 실패했습니다."
            );
        }
    }

    private void revokeTokenFamily(String tokenFamily) {
        lockActiveTokenFamily(tokenFamily).forEach(RefreshToken::revoke);
    }

    private List<RefreshToken> lockActiveTokenFamily(String tokenFamily) {
        return refreshTokenRepository.findAllByTokenFamilyAndStatusForUpdate(
                tokenFamily,
                RefreshTokenStatus.ACTIVE
        );
    }

    private TokenPayload parseAccessToken(String rawAccessToken) {
        try {
            return jwtProvider.parseAccessToken(rawAccessToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(
                    ErrorCode.INVALID_ACCESS_TOKEN,
                    "Access Token 검증에 실패했습니다."
            );
        }
    }

    private void blacklistAccessToken(TokenPayload accessPayload) {
        if (revokedAccessTokenRepository.existsByJti(accessPayload.jti())) {
            return;
        }

        revokedAccessTokenRepository.save(
                new RevokedAccessToken(
                        accessPayload.jti(),
                        LocalDateTime.ofInstant(
                                accessPayload.expiresAt(),
                                ZoneId.systemDefault()
                        )
                )
        );
    }

    private void revokeRefreshTokenFamily(String rawRefreshToken, Long memberId) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        try {
            TokenPayload refreshPayload = jwtProvider.parseRefreshToken(rawRefreshToken);

            if (!memberId.equals(refreshPayload.memberId())) {
                return;
            }

            refreshTokenRepository.findByJtiForUpdate(refreshPayload.jti())
                    .filter(savedRefreshToken ->
                            savedRefreshToken.getTokenHash()
                                    .equals(tokenHashProvider.hash(rawRefreshToken))
                    )
                    .ifPresent(savedRefreshToken ->
                            revokeTokenFamily(savedRefreshToken.getTokenFamily())
                    );
        } catch (JwtException | IllegalArgumentException ignored) {
            // Refresh Token이 없거나 유효하지 않아도 Access Token 로그아웃은 유지한다.
        }
    }
}
