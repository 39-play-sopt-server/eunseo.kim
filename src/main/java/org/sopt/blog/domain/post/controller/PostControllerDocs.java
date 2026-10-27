package org.sopt.blog.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.sopt.blog.domain.post.controller.dto.request.PostRequest;
import org.sopt.blog.domain.post.controller.dto.request.PostUpdateRequest;
import org.sopt.blog.domain.post.controller.dto.response.PostPageResponse;
import org.sopt.blog.domain.post.controller.dto.response.PostResponse;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Post", description = "게시글 API")
public interface PostControllerDocs {

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    })
    @Operation(summary = "게시글 작성", description = "제목, 내용, 카테고리, 작성자를 받아 게시글을 작성합니다.")
    ResponseEntity<ApiResponse<Void>> createPost(@Valid PostRequest request);

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    })
    @Operation(summary = "게시글 전체 목록 조회", description = "전체 게시글의 요약 목록을 반환합니다.")
    ResponseEntity<ApiResponse<PostPageResponse>> getAllPosts(
            @Min(0) int page,
            @Min(1) @Max(100) int size,
            @RequestParam(required = false) Category category
    );

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @Operation(summary = "게시글 상세 조회", description = "postId로 게시글 상세를 조회합니다. 조회수 +1.")
    ResponseEntity<ApiResponse<PostResponse>> getPostById(Long postId);

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @Operation(summary = "게시글 수정", description = "postId로 게시글을 수정합니다.")
    ResponseEntity<ApiResponse<Void>> updatePost(Long postId, @Valid PostUpdateRequest request);

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @Operation(summary = "게시글 삭제", description = "postId로 게시글을 삭제합니다.")
    ResponseEntity<ApiResponse<Void>> deletePost(Long postId);
}