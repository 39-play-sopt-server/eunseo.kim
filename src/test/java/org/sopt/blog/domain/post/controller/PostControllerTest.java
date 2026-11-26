package org.sopt.blog.domain.post.controller;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.post.controller.dto.request.PostRequest;
import org.sopt.blog.domain.post.entity.Category;
import org.sopt.blog.domain.post.service.PostService;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostControllerTest {

    @Test
    void 게시글_작성은_JWT_Principal의_회원_ID를_서비스로_전달한다() {
        PostService postService = mock(PostService.class);
        PostController postController = new PostController(postService);
        PostRequest request = new PostRequest("제목", "내용", Category.NOTICE);

        ResponseEntity<ApiResponse<Void>> response = postController.createPost(1L, request);

        verify(postService).createPost(request, 1L);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}
