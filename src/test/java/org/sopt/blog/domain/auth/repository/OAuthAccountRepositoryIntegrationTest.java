package org.sopt.blog.domain.auth.repository;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.entity.OAuthAccount;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OAuthAccountRepositoryIntegrationTest {

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 제공자와_제공자_사용자_ID로_소셜_계정을_조회한다() {
        Member member = memberRepository.saveAndFlush(
                Member.createSocialMember("카카오닉네임")
        );
        OAuthAccount oauthAccount = oauthAccountRepository.saveAndFlush(
                new OAuthAccount(member, OAuthProvider.KAKAO, "123456789")
        );

        OAuthAccount found = oauthAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.KAKAO, "123456789")
                .orElseThrow();

        assertEquals(oauthAccount.getId(), found.getId());
        assertEquals(member.getId(), found.getMember().getId());
    }
}
