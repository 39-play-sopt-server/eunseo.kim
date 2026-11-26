package org.sopt.blog.domain.like.controller;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.like.service.LikeService;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LikeControllerTest {

    @Test
    void 좋아요는_JWT_Principal의_회원_ID로_처리한다() {
        LikeService likeService = mock(LikeService.class);
        LikeController likeController = new LikeController(likeService);

        ResponseEntity<ApiResponse<Void>> response = likeController.addLike(10L, 1L);

        verify(likeService).addLike(10L, 1L);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void 좋아요_취소는_JWT_Principal의_회원_ID로_처리한다() {
        LikeService likeService = mock(LikeService.class);
        LikeController likeController = new LikeController(likeService);

        ResponseEntity<ApiResponse<Void>> response = likeController.removeLike(10L, 1L);

        verify(likeService).removeLike(10L, 1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
