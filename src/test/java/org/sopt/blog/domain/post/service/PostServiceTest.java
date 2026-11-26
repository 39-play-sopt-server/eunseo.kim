package org.sopt.blog.domain.post.service;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.domain.post.controller.dto.request.PostUpdateRequest;
import org.sopt.blog.domain.post.entity.Post;
import org.sopt.blog.domain.post.repository.PostRepository;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

    @Test
    void 다른_회원의_게시글은_수정할_수_없다() {
        PostRepository postRepository = mock(PostRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        PostService postService = new PostService(postRepository, memberRepository);
        Post post = mock(Post.class);
        Member author = mock(Member.class);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(author);
        when(author.getId()).thenReturn(2L);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> postService.updatePost(
                        10L,
                        new PostUpdateRequest("수정 제목", "수정 내용"),
                        1L
                )
        );

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(post, never()).updateTitle("수정 제목");
        verify(post, never()).updateContent("수정 내용");
    }

    @Test
    void 다른_회원의_게시글은_삭제할_수_없다() {
        PostRepository postRepository = mock(PostRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        PostService postService = new PostService(postRepository, memberRepository);
        Post post = mock(Post.class);
        Member author = mock(Member.class);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(post.getMember()).thenReturn(author);
        when(author.getId()).thenReturn(2L);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> postService.deletePost(10L, 1L)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(postRepository, never()).delete(post);
    }
}
