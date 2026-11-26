package org.sopt.blog.domain.post.controller.dto.response;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostResponseTest {

    @Test
    void 게시글_응답에서_사용자_입력_HTML을_escape한다() {
        Member member = new Member("<b>작성자</b>", "author@example.com", "password");
        Post post = new Post(
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>",
                Category.NOTICE,
                member
        );

        PostResponse detail = PostResponse.from(post);
        PostSummaryResponse summary = PostSummaryResponse.from(post);

        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", detail.title());
        assertEquals("&lt;img src=x onerror=alert(1)&gt;", detail.content());
        assertEquals("&lt;b&gt;작성자&lt;/b&gt;", detail.authorName());
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", summary.title());
        assertEquals("&lt;b&gt;작성자&lt;/b&gt;", summary.authorName());
    }
}
