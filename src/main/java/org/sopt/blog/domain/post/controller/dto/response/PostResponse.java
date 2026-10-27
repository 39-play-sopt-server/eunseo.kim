package org.sopt.blog.domain.post.controller.dto.response;

import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;

public record PostResponse (
    Long id,
    String title,
    String content,
    Category category,
    String author,
    int viewCount
) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getAuthor(),
                post.getViewCount()
        );
    }
}
