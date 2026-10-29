package org.sopt.blog.domain.like.service;

import org.sopt.blog.domain.like.entity.Like;
import org.sopt.blog.domain.like.repository.LikeRepository;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.repository.PostRepository;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public LikeService(LikeRepository likeRepository,
                       PostRepository postRepository,
                       MemberRepository memberRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
    }

    // 좋아요 누르기
    @Transactional
    public void addLike(Long postId, Long memberId) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "id=" + postId + " 번 게시글이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND, "id=" + memberId + " 번 회원이 없습니다."));

        if (likeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new CustomException(ErrorCode.LIKE_ALREADY_EXISTS, "이미 좋아요를 눌렀습니다.");
        }

        likeRepository.save(new Like(post, member));
        post.increaseLikeCount();
    }

    // 좋아요 취소
    @Transactional
    public void removeLike(Long postId, Long memberId) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "id=" + postId + " 번 게시글이 없습니다."));

        long deleted = likeRepository.deleteByPostIdAndMemberId(postId, memberId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.LIKE_NOT_FOUND, "좋아요를 누르지 않았습니다.");
        }

        post.decreaseLikeCount();
    }
}
