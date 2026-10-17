package org.sopt.blog.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.blog.domain.post.controller.dto.request.PostRequest;
import org.sopt.blog.domain.post.controller.dto.response.PostPageResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostResponse;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Post", description = "게시글 API")
public interface PostControllerDocs {

    @Operation(summary = "게시글 작성", description = "제목, 내용, 카테고리, 작성자를 받아 게시글을 작성합니다.")
    ResponseEntity<ApiResponse<Void>> createPost(PostRequest request);

    @Operation(summary = "게시글 전체 목록 조회", description = "전체 게시글의 요약 목록을 반환합니다.")
    ResponseEntity<ApiResponse<PostPageResponse>> getAllPosts(int page, int size);

    @Operation(summary = "게시글 상세 조회", description = "index로 게시글 상세를 조회합니다. 조회수 +1.")
    ResponseEntity<ApiResponse<PostResponse>> getPostById(int index);

    @Operation(summary = "게시글 수정", description = "index로 게시글을 수정합니다.")
    ResponseEntity<ApiResponse<Void>> updatePost(int index, PostRequest request);

    @Operation(summary = "게시글 삭제", description = "index로 게시글을 삭제합니다.")
    ResponseEntity<ApiResponse<Void>> deletePost(int index);
}