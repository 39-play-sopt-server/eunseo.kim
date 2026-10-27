package org.sopt.blog.domain.post.service;

import org.sopt.blog.domain.post.controller.dto.request.PostUpdateRequest;
import org.sopt.blog.domain.post.entity.Category;
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

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // 게시글 작성
    public void createPost(PostRequest request) {
        Post post = new Post(
                request.title(),
                request.content(),
                request.category(),
                request.author()
        );
        postRepository.save(post);
    }

    // 전체 목록 조회
    public PostPageResponse getPosts(int page, int size, Category category) {
        List<Post> list;
        if (category == null) {
            list = postRepository.findAll();
        } else {
            list = postRepository.findByCategory(category);
        }

        int totalCount = list.size();
        int from = page * size;
        int to = Math.min(from + size, totalCount);

        List<PostSummaryResponse> pageItems = (from >= totalCount)
                ? List.of()
                : list.subList(from, to).stream().map(PostSummaryResponse::from).toList();

        return PostPageResponse.of(pageItems, page, size, totalCount);
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