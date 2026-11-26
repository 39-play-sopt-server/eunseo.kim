package org.sopt.blog.domain.auth.entity;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuthAccountTest {

    @Test
    void 카카오_계정은_회원과_카카오_고유_ID를_연결한다() {
        Member member = Member.createSocialMember("카카오닉네임");

        OAuthAccount oauthAccount = new OAuthAccount(
                member,
                OAuthProvider.KAKAO,
                "123456789"
        );

        assertEquals(member, oauthAccount.getMember());
        assertEquals(OAuthProvider.KAKAO, oauthAccount.getProvider());
        assertEquals("123456789", oauthAccount.getProviderUserId());
    }
}
