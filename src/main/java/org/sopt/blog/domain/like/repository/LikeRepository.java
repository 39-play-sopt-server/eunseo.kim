package org.sopt.blog.domain.like.repository;

import org.sopt.blog.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // (post_id, member_id)로 좋아요 존재 여부 확인
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    // (post_id, member_id)로 좋아요 삭제 (취소용)
    long deleteByPostIdAndMemberId(Long postId, Long memberId);
}