package org.sopt.blog.domain.post.controller.dto.response;

import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;

public record PostResponse (
    Long id,
    String title,
    String content,
    Category category,
    String authorName,
    int viewCount,
    int likeCount
) {

    public static PostResponse from(Post post) {
        Member member = post.getMember();
        String authorName = member.isDeleted() ? "탈퇴한 회원" : member.getName();

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                authorName,
                post.getViewCount(),
                post.getLikeCount()
        );
    }
}
