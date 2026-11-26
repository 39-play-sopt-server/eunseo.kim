package org.sopt.blog.domain.member.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MemberTest {

    @Test
    void 새_회원의_기본_권한은_USER다() {
        Member member = new Member("은서", "eunseo@example.com", "encoded-password");

        assertEquals(MemberRole.ROLE_USER, member.getRole());
    }

    @Test
    void 소셜_회원은_이메일과_비밀번호_없이_생성된다() {
        Member member = Member.createSocialMember("카카오닉네임");

        assertEquals("카카오닉네임", member.getName());
        assertNull(member.getEmail());
        assertNull(member.getPassword());
        assertEquals(MemberRole.ROLE_USER, member.getRole());
    }
}
