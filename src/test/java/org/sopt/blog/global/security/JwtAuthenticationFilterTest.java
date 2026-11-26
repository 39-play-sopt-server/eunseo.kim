package org.sopt.blog.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.sopt.blog.domain.member.entity.MemberRole;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 블랙리스트에_있는_Access_Token은_인증하지_않는다() throws ServletException, IOException {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtProvider,
                revokedAccessTokenRepository,
                memberRepository
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        TokenPayload payload = new TokenPayload(
                1L,
                "revoked-access-token-jti",
                TokenType.ACCESS,
                MemberRole.ROLE_USER,
                Instant.now(),
                Instant.now().plusSeconds(1800)
        );
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");

        when(jwtProvider.parseAccessToken("access-token")).thenReturn(payload);
        when(revokedAccessTokenRepository.existsByJti(payload.jti())).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(revokedAccessTokenRepository).existsByJti(payload.jti());
    }

    @Test
    void 블랙리스트에_없는_Access_Token은_인증한다() throws ServletException, IOException {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtProvider,
                revokedAccessTokenRepository,
                memberRepository
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        TokenPayload payload = new TokenPayload(
                1L,
                "active-access-token-jti",
                TokenType.ACCESS,
                MemberRole.ROLE_USER,
                Instant.now(),
                Instant.now().plusSeconds(1800)
        );
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");

        when(jwtProvider.parseAccessToken("access-token")).thenReturn(payload);
        when(revokedAccessTokenRepository.existsByJti(payload.jti())).thenReturn(false);
        when(memberRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertEquals(1L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals(
                "ROLE_USER",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void 탈퇴한_회원의_Access_Token은_인증하지_않는다() throws ServletException, IOException {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        RevokedAccessTokenRepository revokedAccessTokenRepository = mock(RevokedAccessTokenRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtProvider,
                revokedAccessTokenRepository,
                memberRepository
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        TokenPayload payload = new TokenPayload(
                1L,
                "withdrawn-member-access-token-jti",
                TokenType.ACCESS,
                MemberRole.ROLE_USER,
                Instant.now(),
                Instant.now().plusSeconds(1800)
        );
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");

        when(jwtProvider.parseAccessToken("access-token")).thenReturn(payload);
        when(revokedAccessTokenRepository.existsByJti(payload.jti())).thenReturn(false);
        when(memberRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(memberRepository).existsByIdAndDeletedAtIsNull(1L);
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
