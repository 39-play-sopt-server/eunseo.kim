package org.sopt.blog.domain.post.service;

import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.domain.post.controller.dto.request.PostUpdateRequest;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.repository.PostSpecifications;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.sopt.blog.domain.post.controller.dto.request.PostRequest;
import org.sopt.blog.domain.post.controller.dto.response.PostPageResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostSummaryResponse;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.repository.PostRepository;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public PostService(PostRepository postRepository, MemberRepository memberRepository) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
    }

    // 게시글 작성
    public void createPost(PostRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND, "id=" + request.memberId() + " 번 회원이 존재하지 않습니다."));
        if (member.isDeleted()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND, "id=" + request.memberId() + " 번 회원이 이미 탈퇴했습니다.");
        }
        Post post = new Post(
                request.title(),
                request.content(),
                request.category(),
                member
        );
        postRepository.save(post);
    }

    // 전체 목록 조회
    public PostPageResponse getPosts(int page, int size, Category category, String title, Long memberId) {
        // Pageable 생성 (최신순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Post> spec = Specification.allOf(PostSpecifications.fetchMember());
        if (title != null && !title.isBlank()) {
            spec = spec.and(PostSpecifications.titleContains(title));
        }
        if (category != null) {
            spec = spec.and(PostSpecifications.hasCategory(category));
        }
        if (memberId != null) {
            spec = spec.and(PostSpecifications.hasMember(memberId));
        }

        Page<Post> postPage = postRepository.findAll(spec, pageable);

        List<PostSummaryResponse> pageItems = postPage.getContent().stream()
                .map(PostSummaryResponse::from)
                .toList();
        return PostPageResponse.of(pageItems, page, size, postPage.getTotalElements());
    }

    // 단건 상세 조회
    @Transactional
    public PostResponse getPostById(Long postId) {
        Post post = findPost(postId);
        post.increaseViewCount();
        return PostResponse.from(post);
    }


    // 게시글 수정
    @Transactional
    public void updatePost(Long id, PostUpdateRequest request) {
        Post post = findPost(id);
        post.updateTitle(request.title());
        post.updateContent(request.content());
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    // index로 Post 엔티티 찾기 + 유효성 검증
    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "id=" + id + " 번 게시글이 존재하지 않습니다."));
    }
}