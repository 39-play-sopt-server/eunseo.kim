package org.sopt.blog.domain.post.repository;

import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.member")
    Page<Post> findAllWithMember(Pageable pageable);

    @Query("select p from Post p join fetch p.member where p.category = :category")
    Page<Post> findByCategoryWithMember(@Param("category") Category category, Pageable pageable);
}

