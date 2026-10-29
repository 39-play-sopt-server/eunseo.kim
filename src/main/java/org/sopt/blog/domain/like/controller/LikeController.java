package org.sopt.blog.domain.like.controller;

import jakarta.validation.Valid;
import org.sopt.blog.domain.like.controller.dto.request.LikeRequest;
import org.sopt.blog.domain.like.service.LikeService;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/likes")
public class LikeController {
    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    // 좋아요 누르기
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addLike(
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest request
    ) {
        likeService.addLike(postId, request.memberId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "좋아요를 눌렀습니다."));
    }

    // 좋아요 취소
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest request
    ) {
        likeService.removeLike(postId, request.memberId());
        return ResponseEntity.ok(ApiResponse.success(200, "좋아요를 취소했습니다."));
    }
}