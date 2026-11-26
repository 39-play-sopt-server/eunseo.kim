package org.sopt.blog.domain.member.controller.dto.response;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberSummaryResponseTest {

    @Test
    void 회원_목록_응답에서_이름과_이메일을_escape한다() {
        Member member = new Member(
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>@example.com",
                "password"
        );

        MemberSummaryResponse response = MemberSummaryResponse.from(member);

        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", response.name());
        assertEquals("&lt;img src=x onerror=alert(1)&gt;@example.com", response.email());
    }
}
