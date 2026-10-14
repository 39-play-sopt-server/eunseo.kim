package org.sopt.blog.domain.post.controller.dto.response;

import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;

public record PostResponse (
    String title,
    String content,
    Category category,
    String author,
    int viewCount
) {
    // from: static factory method(Post 엔티티를 넣으면, PostResponse로 변환해서 돌려주는 메서드)
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getAuthor(),
                post.getViewCount()
        );
    }
}
