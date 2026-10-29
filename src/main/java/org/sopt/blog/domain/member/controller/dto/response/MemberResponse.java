package org.sopt.blog.domain.member.controller.dto.response;

import org.sopt.blog.domain.member.entity.Member;

public record MemberResponse (
    Long id,
    String name
){
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName());
    }
}
