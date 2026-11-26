package org.sopt.blog.domain.member.controller.dto.response;

import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.entity.MemberRole;
import org.springframework.web.util.HtmlUtils;

public record MemberSummaryResponse(
        Long id,
        String name,
        String email,
        MemberRole role
) {

    public static MemberSummaryResponse from(Member member) {
        return new MemberSummaryResponse(
                member.getId(),
                HtmlUtils.htmlEscape(member.getName()),
                escapeNullable(member.getEmail()),
                member.getRole()
        );
    }

    private static String escapeNullable(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }
}
