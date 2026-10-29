package org.sopt.blog.domain.like.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record LikeRequest(
        @NotNull(message = "회원 ID는 필수입니다.") Long memberId
) {}