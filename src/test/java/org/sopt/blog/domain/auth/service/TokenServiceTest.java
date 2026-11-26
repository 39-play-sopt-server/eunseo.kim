package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sopt.blog.domain.auth.entity.RefreshToken;
import org.sopt.blog.domain.auth.entity.RefreshTokenStatus;
import org.sopt.blog.domain.auth.entity.RevokedAccessToken;
import org.sopt.blog.domain.auth.repository.RefreshTokenRepository;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.entity.MemberRole;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.sopt.blog.global.security.JwtProvider;
import org.sopt.blog.global.security.TokenHashProvider;
import org.sopt.blog.global.security.TokenPayload;
import org.sopt.blog.global.security.TokenType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    @Test
    void 토큰_쌍을_발급하고_Refresh_Token의_해시를_저장한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        Member member = mock(Member.class);
        Instant expiresAt = Instant.parse("2026-12-01T00:00:00Z");

        when(member.getId()).thenReturn(1L);
        when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
        when(jwtProvider.createAccessToken(1L, MemberRole.ROLE_USER)).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtProvider.parseRefreshToken("refresh-token")).thenReturn(
                new TokenPayload(
                        1L,
                        "refresh-jti",
                        TokenType.REFRESH,
                        null,
                        Instant.parse("2026-11-17T00:00:00Z"),
                        expiresAt
                )
        );
        when(tokenHashProvider.hash("refresh-token")).thenReturn("hashed-refresh-token");

        TokenPair tokenPair = tokenService.issueTokenPair(member);

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();

        assertEquals("access-token", tokenPair.accessToken());
        assertEquals("refresh-token", tokenPair.refreshToken());
        assertEquals("refresh-jti", savedRefreshToken.getJti());
        assertEquals("hashed-refresh-token", savedRefreshToken.getTokenHash());
        assertNotEquals("refresh-token", savedRefreshToken.getTokenHash());
        assertEquals(
                LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()),
                savedRefreshToken.getExpiresAt()
        );
        assertNotNull(savedRefreshToken.getTokenFamily());
    }

    @Test
    void Refresh_Token을_회전시키고_같은_family로_새_토큰_쌍을_발급한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        Member member = mock(Member.class);
        RefreshToken oldRefreshToken = new RefreshToken(
                member,
                "old-jti",
                "old-hash",
                "token-family",
                LocalDateTime.now().plusDays(14)
        );
        TokenPayload oldPayload = new TokenPayload(
                1L,
                "old-jti",
                TokenType.REFRESH,
                null,
                Instant.parse("2026-11-17T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z")
        );
        TokenPayload newPayload = new TokenPayload(
                1L,
                "new-jti",
                TokenType.REFRESH,
                null,
                Instant.parse("2026-11-17T00:00:01Z"),
                Instant.parse("2026-12-01T00:00:01Z")
        );

        when(member.getId()).thenReturn(1L);
        when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
        when(jwtProvider.parseRefreshToken("old-refresh-token")).thenReturn(oldPayload);
        when(refreshTokenRepository.findByJtiForUpdate("old-jti"))
                .thenReturn(Optional.of(oldRefreshToken));
        when(tokenHashProvider.hash("old-refresh-token")).thenReturn("old-hash");
        when(jwtProvider.createAccessToken(1L, MemberRole.ROLE_USER)).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");
        when(jwtProvider.parseRefreshToken("new-refresh-token")).thenReturn(newPayload);
        when(tokenHashProvider.hash("new-refresh-token")).thenReturn("new-hash");

        TokenPair result = tokenService.reissue("old-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken newRefreshToken = captor.getValue();

        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
        assertEquals(RefreshTokenStatus.ROTATED, oldRefreshToken.getStatus());
        assertEquals("new-jti", newRefreshToken.getJti());
        assertEquals("token-family", newRefreshToken.getTokenFamily());
        assertEquals("new-hash", newRefreshToken.getTokenHash());
    }

    @Test
    void 이미_회전된_Refresh_Token을_재사용하면_활성_family를_폐기한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        Member member = mock(Member.class);
        RefreshToken reusedRefreshToken = new RefreshToken(
                member,
                "old-jti",
                "old-hash",
                "token-family",
                LocalDateTime.now().plusDays(14)
        );
        RefreshToken activeRefreshToken = new RefreshToken(
                member,
                "active-jti",
                "active-hash",
                "token-family",
                LocalDateTime.now().plusDays(14)
        );
        reusedRefreshToken.rotate();

        when(member.getId()).thenReturn(1L);
        when(jwtProvider.parseRefreshToken("reused-refresh-token")).thenReturn(
                new TokenPayload(
                        1L,
                        "old-jti",
                        TokenType.REFRESH,
                        null,
                        Instant.parse("2026-11-17T00:00:00Z"),
                        Instant.parse("2026-12-01T00:00:00Z")
                )
        );
        when(refreshTokenRepository.findByJtiForUpdate("old-jti"))
                .thenReturn(Optional.of(reusedRefreshToken));
        when(tokenHashProvider.hash("reused-refresh-token")).thenReturn("old-hash");
        when(refreshTokenRepository.findAllByTokenFamilyAndStatusForUpdate(
                "token-family",
                RefreshTokenStatus.ACTIVE
        )).thenReturn(List.of(activeRefreshToken));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> tokenService.reissue("reused-refresh-token")
        );

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals(RefreshTokenStatus.REVOKED, activeRefreshToken.getStatus());
    }

    @Test
    void 로그아웃하면_Access_Token은_블랙리스트에_저장하고_Refresh_Token_family는_폐기한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        Member member = mock(Member.class);
        Instant accessExpiresAt = Instant.parse("2026-12-01T00:00:00Z");
        RefreshToken currentRefreshToken = new RefreshToken(
                member,
                "refresh-jti",
                "refresh-hash",
                "token-family",
                LocalDateTime.now().plusDays(14)
        );
        RefreshToken anotherActiveRefreshToken = new RefreshToken(
                member,
                "another-refresh-jti",
                "another-refresh-hash",
                "token-family",
                LocalDateTime.now().plusDays(14)
        );
        TokenPayload accessPayload = new TokenPayload(
                1L,
                "access-jti",
                TokenType.ACCESS,
                MemberRole.ROLE_USER,
                Instant.parse("2026-11-17T00:00:00Z"),
                accessExpiresAt
        );
        TokenPayload refreshPayload = new TokenPayload(
                1L,
                "refresh-jti",
                TokenType.REFRESH,
                null,
                Instant.parse("2026-11-17T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z")
        );

        when(jwtProvider.parseAccessToken("access-token")).thenReturn(accessPayload);
        when(revokedAccessTokenRepository.existsByJti("access-jti")).thenReturn(false);
        when(jwtProvider.parseRefreshToken("refresh-token")).thenReturn(refreshPayload);
        when(refreshTokenRepository.findByJtiForUpdate("refresh-jti"))
                .thenReturn(Optional.of(currentRefreshToken));
        when(tokenHashProvider.hash("refresh-token")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findAllByTokenFamilyAndStatusForUpdate(
                "token-family",
                RefreshTokenStatus.ACTIVE
        )).thenReturn(List.of(currentRefreshToken, anotherActiveRefreshToken));

        tokenService.logout("access-token", "refresh-token");

        ArgumentCaptor<RevokedAccessToken> captor = ArgumentCaptor.forClass(RevokedAccessToken.class);
        verify(revokedAccessTokenRepository).save(captor.capture());
        RevokedAccessToken revokedAccessToken = captor.getValue();

        assertEquals("access-jti", revokedAccessToken.getJti());
        assertEquals(
                LocalDateTime.ofInstant(accessExpiresAt, ZoneId.systemDefault()),
                revokedAccessToken.getExpiresAt()
        );
        assertEquals(RefreshTokenStatus.REVOKED, currentRefreshToken.getStatus());
        assertEquals(RefreshTokenStatus.REVOKED, anotherActiveRefreshToken.getStatus());
    }

    @Test
    void Refresh_Token이_없어도_로그아웃하면_Access_Token은_블랙리스트에_저장한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        TokenPayload accessPayload = new TokenPayload(
                1L,
                "access-jti",
                TokenType.ACCESS,
                MemberRole.ROLE_USER,
                Instant.parse("2026-11-17T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z")
        );

        when(jwtProvider.parseAccessToken("access-token")).thenReturn(accessPayload);
        when(revokedAccessTokenRepository.existsByJti("access-jti")).thenReturn(false);

        tokenService.logout("access-token", null);

        verify(revokedAccessTokenRepository).save(org.mockito.ArgumentMatchers.any(RevokedAccessToken.class));
    }

    @Test
    void 회원의_모든_활성_Refresh_Token을_폐기한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        Member member = mock(Member.class);
        RefreshToken firstRefreshToken = new RefreshToken(
                member,
                "first-jti",
                "first-hash",
                "first-family",
                LocalDateTime.now().plusDays(14)
        );
        RefreshToken secondRefreshToken = new RefreshToken(
                member,
                "second-jti",
                "second-hash",
                "second-family",
                LocalDateTime.now().plusDays(14)
        );

        when(refreshTokenRepository.findAllByMember_IdAndStatus(
                1L,
                RefreshTokenStatus.ACTIVE
        )).thenReturn(List.of(firstRefreshToken, secondRefreshToken));
        when(refreshTokenRepository.findAllByTokenFamilyAndStatusForUpdate(
                "first-family",
                RefreshTokenStatus.ACTIVE
        )).thenReturn(List.of(firstRefreshToken));
        when(refreshTokenRepository.findAllByTokenFamilyAndStatusForUpdate(
                "second-family",
                RefreshTokenStatus.ACTIVE
        )).thenReturn(List.of(secondRefreshToken));

        tokenService.revokeAllRefreshTokens(1L);

        assertEquals(RefreshTokenStatus.REVOKED, firstRefreshToken.getStatus());
        assertEquals(RefreshTokenStatus.REVOKED, secondRefreshToken.getStatus());
        verify(refreshTokenRepository).findAllByMember_IdAndStatus(
                1L,
                RefreshTokenStatus.ACTIVE
        );
    }

    @Test
    void 탈퇴한_회원은_Refresh_Token으로_재발급할_수_없다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        TokenHashProvider tokenHashProvider = mock(TokenHashProvider.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        TokenService tokenService = new TokenService(
                jwtProvider,
                tokenHashProvider,
                refreshTokenRepository,
                revokedAccessTokenRepository
        );
        Member deletedMember = mock(Member.class);
        RefreshToken refreshToken = new RefreshToken(
                deletedMember,
                "refresh-jti",
                "refresh-hash",
                "token-family",
                LocalDateTime.now().plusDays(14)
        );
        TokenPayload refreshPayload = new TokenPayload(
                1L,
                "refresh-jti",
                TokenType.REFRESH,
                null,
                Instant.parse("2026-11-17T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z")
        );

        when(deletedMember.getId()).thenReturn(1L);
        when(deletedMember.isDeleted()).thenReturn(true);
        when(jwtProvider.parseRefreshToken("refresh-token")).thenReturn(refreshPayload);
        when(refreshTokenRepository.findByJtiForUpdate("refresh-jti"))
                .thenReturn(Optional.of(refreshToken));
        when(tokenHashProvider.hash("refresh-token")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findAllByTokenFamilyAndStatusForUpdate(
                "token-family",
                RefreshTokenStatus.ACTIVE
        )).thenReturn(List.of(refreshToken));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> tokenService.reissue("refresh-token")
        );

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals(RefreshTokenStatus.REVOKED, refreshToken.getStatus());
    }
}
