package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sopt.blog.domain.auth.client.OAuthClient;
import org.sopt.blog.domain.auth.client.OAuthUser;
import org.sopt.blog.domain.auth.entity.OAuthAccount;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.auth.repository.OAuthAccountRepository;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.Optional;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthLoginServiceTest {

    @Test
    void 기존_카카오_회원이면_새_회원가입_없이_토큰을_발급한다() {
        OAuthClient oauthClient = mockOAuthClient();
        OAuthAccountRepository oauthAccountRepository = mock(OAuthAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        TokenService tokenService = mock(TokenService.class);
        OAuthLoginService oauthLoginService = new OAuthLoginService(
                List.of(oauthClient),
                oauthAccountRepository,
                memberRepository,
                tokenService
        );
        OAuthUser oauthUser = new OAuthUser("123456789", "카카오닉네임");
        Member member = Member.createSocialMember("기존닉네임");
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        when(oauthClient.getOAuthUser("authorization-code")).thenReturn(oauthUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "123456789"
        )).thenReturn(Optional.of(new OAuthAccount(
                member,
                OAuthProvider.KAKAO,
                "123456789"
        )));
        when(tokenService.issueTokenPair(member)).thenReturn(tokenPair);

        TokenPair result = oauthLoginService.login(
                OAuthProvider.KAKAO,
                "authorization-code"
        );

        assertEquals(tokenPair, result);
        verify(memberRepository, never()).save(any(Member.class));
        verify(oauthAccountRepository, never()).save(any(OAuthAccount.class));
        verify(tokenService).issueTokenPair(member);
    }

    @Test
    void 처음_카카오_로그인하면_이메일과_비밀번호_없이_회원을_생성한다() {
        OAuthClient oauthClient = mockOAuthClient();
        OAuthAccountRepository oauthAccountRepository = mock(OAuthAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        TokenService tokenService = mock(TokenService.class);
        OAuthLoginService oauthLoginService = new OAuthLoginService(
                List.of(oauthClient),
                oauthAccountRepository,
                memberRepository,
                tokenService
        );
        OAuthUser oauthUser = new OAuthUser("123456789", "카카오닉네임");
        Member savedMember = Member.createSocialMember("카카오닉네임");
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        when(oauthClient.getOAuthUser("authorization-code")).thenReturn(oauthUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "123456789"
        )).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);
        when(tokenService.issueTokenPair(savedMember)).thenReturn(tokenPair);

        TokenPair result = oauthLoginService.login(
                OAuthProvider.KAKAO,
                "authorization-code"
        );

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        ArgumentCaptor<OAuthAccount> oauthAccountCaptor = ArgumentCaptor.forClass(OAuthAccount.class);

        assertEquals(tokenPair, result);
        verify(memberRepository).save(memberCaptor.capture());
        verify(oauthAccountRepository).save(oauthAccountCaptor.capture());
        assertEquals("카카오닉네임", memberCaptor.getValue().getName());
        assertNull(memberCaptor.getValue().getEmail());
        assertNull(memberCaptor.getValue().getPassword());
        assertEquals(savedMember, oauthAccountCaptor.getValue().getMember());
        assertEquals(OAuthProvider.KAKAO, oauthAccountCaptor.getValue().getProvider());
        assertEquals("123456789", oauthAccountCaptor.getValue().getProviderUserId());
    }

    @Test
    void 탈퇴한_카카오_회원은_토큰을_발급하지_않는다() {
        OAuthClient oauthClient = mockOAuthClient();
        OAuthAccountRepository oauthAccountRepository = mock(OAuthAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        TokenService tokenService = mock(TokenService.class);
        OAuthLoginService oauthLoginService = new OAuthLoginService(
                List.of(oauthClient),
                oauthAccountRepository,
                memberRepository,
                tokenService
        );
        OAuthUser oauthUser = new OAuthUser("123456789", "카카오닉네임");
        Member deletedMember = Member.createSocialMember("탈퇴한회원");
        deletedMember.markDeleted();

        when(oauthClient.getOAuthUser("authorization-code")).thenReturn(oauthUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "123456789"
        )).thenReturn(Optional.of(new OAuthAccount(
                deletedMember,
                OAuthProvider.KAKAO,
                "123456789"
        )));

        assertThrows(
                IllegalArgumentException.class,
                () -> oauthLoginService.login(
                        OAuthProvider.KAKAO,
                        "authorization-code"
                )
        );

        verify(tokenService, never()).issueTokenPair(any(Member.class));
    }

    @Test
    void provider_중립_인가_URL_생성을_OAuthClient에_위임한다() {
        OAuthClient oauthClient = mockOAuthClient();
        OAuthLoginService oauthLoginService = new OAuthLoginService(
                List.of(oauthClient),
                mock(OAuthAccountRepository.class),
                mock(MemberRepository.class),
                mock(TokenService.class)
        );
        URI authorizationUri = URI.create("https://provider.example/authorize?state=state");

        when(oauthClient.createAuthorizationUri("state")).thenReturn(authorizationUri);

        assertEquals(
                authorizationUri,
                oauthLoginService.createAuthorizationUri(OAuthProvider.KAKAO, "state")
        );
        verify(oauthClient).createAuthorizationUri("state");
    }

    private OAuthClient mockOAuthClient() {
        OAuthClient oauthClient = mock(OAuthClient.class);
        when(oauthClient.provider()).thenReturn(OAuthProvider.KAKAO);
        return oauthClient;
    }
}
