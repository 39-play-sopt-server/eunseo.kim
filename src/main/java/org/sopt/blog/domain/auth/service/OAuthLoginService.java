package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.domain.auth.client.OAuthClient;
import org.sopt.blog.domain.auth.client.OAuthUser;
import org.sopt.blog.domain.auth.entity.OAuthAccount;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.auth.repository.OAuthAccountRepository;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final List<OAuthClient> oauthClients;
    private final OAuthAccountRepository oauthAccountRepository;
    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    public URI createAuthorizationUri(OAuthProvider provider, String state) {
        return findOAuthClient(provider).createAuthorizationUri(state);
    }

    @Transactional
    public TokenPair login(
            OAuthProvider provider,
            String authorizationCode
    ) {
        OAuthClient oauthClient = findOAuthClient(provider);
        OAuthUser oauthUser = oauthClient.getOAuthUser(authorizationCode);

        Member member = oauthAccountRepository
                .findByProviderAndProviderUserId(
                        provider,
                        oauthUser.providerUserId()
                )
                .map(OAuthAccount::getMember)
                .orElseGet(() -> createSocialMember(provider, oauthUser));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 회원은 로그인할 수 없습니다.");
        }

        return tokenService.issueTokenPair(member);
    }

    private OAuthClient findOAuthClient(OAuthProvider provider) {
        return oauthClients.stream()
                .filter(oauthClient -> oauthClient.provider() == provider)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.")
                );
    }

    private Member createSocialMember(
            OAuthProvider provider,
            OAuthUser oauthUser
    ) {
        Member member = memberRepository.save(
                Member.createSocialMember(oauthUser.nickname())
        );

        oauthAccountRepository.save(
                new OAuthAccount(
                        member,
                        provider,
                        oauthUser.providerUserId()
                )
        );

        return member;
    }
}
