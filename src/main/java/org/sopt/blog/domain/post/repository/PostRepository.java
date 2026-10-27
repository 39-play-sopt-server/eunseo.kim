package org.sopt.blog.domain.post.repository;

import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCategory(Category category);
}

