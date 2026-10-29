package org.sopt.blog.domain.member.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest (
        @NotBlank(message = "이름은 필수입니다.") String name
) {}
