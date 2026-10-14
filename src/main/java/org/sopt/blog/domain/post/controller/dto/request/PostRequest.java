package org.sopt.blog.domain.post.controller.dto.request;

import org.sopt.blog.domain.post.entity.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostRequest (
    @NotBlank(message = "제목은 필수입니다.") String title,
    @NotBlank(message = "내용은 필수입니다.") String content,
    @NotNull(message = "카테고리는 필수입니다.") Category category,
    @NotBlank(message = "작성자는 필수입니다.") String author
) {
}
