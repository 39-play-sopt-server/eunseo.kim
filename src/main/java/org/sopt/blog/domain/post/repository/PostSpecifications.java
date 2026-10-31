package org.sopt.blog.domain.post.repository;

import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.entity.Post;
import org.springframework.data.jpa.domain.Specification;

public class PostSpecifications {
    // 제목 포함 검색
    public static Specification<Post> titleContains(String title) {
        return (root, query, cb) -> cb.like(root.get("title"), "%" + title + "%");
    }

    // 카테고리 일치
    public static Specification<Post> hasCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Post> fetchMember() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {   // count 쿼리일 땐 fetch 하면 안 됨
                root.fetch("member");
            }
            return cb.conjunction();  // "항상 참" 조건 (= 조건 없음)
        };
    }

    public static Specification<Post> hasMember(Long memberId) {
        return (root, query, cb) -> cb.equal(root.get("member").get("id"), memberId);
    }
}
