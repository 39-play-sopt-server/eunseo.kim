package org.sopt.blog.domain.post.controller.dto.response;

import org.sopt.blog.domain.post.entity.Post;

// 목록 조회용 DTO
public record PostSummaryResponse(
        String title
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(post.getTitle());
    }
}

