package org.sopt.blog.domain.post.service;

import org.sopt.blog.domain.post.controller.dto.request.PostRequest;
import org.sopt.blog.domain.post.controller.dto.response.PostPageResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostSummaryResponse;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;

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
    public PostPageResponse getPosts(int page, int size) {
        List<Post> all = postRepository.findAll();
        int totalCount = all.size();

        int from = page * size;
        int to = Math.min(from + size, totalCount);

        List<PostSummaryResponse> pageItems = (from >= totalCount)
                ? List.of()
                : all.subList(from, to).stream().map(PostSummaryResponse::from).toList();

        return PostPageResponse.of(pageItems, page, size, totalCount);
    }

    // 단건 상세 조회
    public PostResponse getPostById(int index) {
        Post post = findPost(index);
        post.increaseViewCount();
        return PostResponse.from(findPost(index));
    }

    // 게시글 수정
    public void updatePost(int index, PostRequest request) {
        Post post = findPost(index);
        post.updateTitle(request.title());
        post.updateContent(request.content());
    }

    // 게시글 삭제
    public void deletePost(int index) {
        findPost(index);
        postRepository.deleteByIndex(index);
    }

    // index로 Post 엔티티 찾기 + 유효성 검증
    private Post findPost(int index) {
        if (index < 0 || index >= postRepository.size()) {
            throw new CustomException(
                    ErrorCode.POST_NOT_FOUND,
                    "index=" + index + " 번 게시글이 존재하지 않습니다."
            );
        }
        return postRepository.findByIndex(index);
    }
}