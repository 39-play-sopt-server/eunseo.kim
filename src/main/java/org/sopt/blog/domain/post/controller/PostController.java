package org.sopt.blog.domain.post.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.sopt.blog.domain.post.controller.dto.request.PostRequest;
import org.sopt.blog.domain.post.controller.dto.request.PostUpdateRequest;
import org.sopt.blog.domain.post.controller.dto.response.PostPageResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostResponse;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.service.PostService;
import org.sopt.blog.global.common.response.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts")
@Validated
public class PostController implements PostControllerDocs {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 게시글 작성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createPost(@Valid @RequestBody PostRequest request) {
        postService.createPost(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "게시글이 작성되었습니다."));
    }

    // 전체 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PostPageResponse>> getAllPosts(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Category category
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(200, "게시글 목록 조회에 성공했습니다.", postService.getPosts(page, size, category, title))
        );
    }

    // 단건 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long postId) {
        return ResponseEntity.ok(
                ApiResponse.success(200, "게시글 상세 조회에 성공했습니다.", postService.getPostById(postId))
        );
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(@PathVariable Long postId, @Valid @RequestBody PostUpdateRequest request) {
        postService.updatePost(postId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "게시글이 수정되었습니다."));
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success(200, "게시글이 삭제되었습니다."));
    }
}