package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.domain.member.service.MemberService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

class AuthServiceTest {

    @Test
    void 올바른_비밀번호로_로그인하면_토큰_쌍을_발급한다() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        MemberService memberService = mock(MemberService.class);
        AuthService authService = new AuthService(
                memberRepository,
                passwordEncoder,
                tokenService,
                memberService
        );
        Member member = new Member("은서", "eunseo@example.com", "encoded-password");
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        when(memberRepository.findByEmail("eunseo@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(tokenService.issueTokenPair(member)).thenReturn(tokenPair);

        TokenPair result = authService.login("eunseo@example.com", "plain-password");

        assertEquals(tokenPair, result);
        verify(tokenService).issueTokenPair(member);
    }

    @Test
    void 비밀번호가_틀리면_토큰을_발급하지_않는다() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        MemberService memberService = mock(MemberService.class);
        AuthService authService = new AuthService(
                memberRepository,
                passwordEncoder,
                tokenService,
                memberService
        );
        Member member = new Member("은서", "eunseo@example.com", "encoded-password");

        when(memberRepository.findByEmail("eunseo@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("eunseo@example.com", "wrong-password")
        );

        verify(tokenService, never()).issueTokenPair(member);
    }

    @Test
    void 탈퇴한_회원은_올바른_비밀번호여도_토큰을_발급하지_않는다() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        MemberService memberService = mock(MemberService.class);
        AuthService authService = new AuthService(
                memberRepository,
                passwordEncoder,
                tokenService,
                memberService
        );
        Member member = new Member("은서", "eunseo@example.com", "encoded-password");
        member.markDeleted();

        when(memberRepository.findByEmail("eunseo@example.com")).thenReturn(Optional.of(member));

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("eunseo@example.com", "plain-password")
        );

        verify(passwordEncoder, never()).matches("plain-password", "encoded-password");
        verify(tokenService, never()).issueTokenPair(member);
    }

    @Test
    void 회원_탈퇴하면_회원은_soft_delete하고_토큰을_무효화한다() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        MemberService memberService = mock(MemberService.class);
        AuthService authService = new AuthService(
                memberRepository,
                passwordEncoder,
                tokenService,
                memberService
        );

        authService.withdraw(1L, "access-token", "refresh-token");

        org.mockito.InOrder inOrder = inOrder(memberService, tokenService);
        inOrder.verify(memberService).deleteMember(1L);
        inOrder.verify(tokenService).logout("access-token", "refresh-token");
        inOrder.verify(tokenService).revokeAllRefreshTokens(1L);
    }
}
