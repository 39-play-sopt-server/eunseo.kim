package org.sopt.blog.domain.member.service;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.member.controller.dto.response.MemberSummaryResponse;
import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    @Test
    void 활성_회원_목록을_응답_DTO로_변환한다() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        Member newestMember = new Member("은서", "eunseo@example.com", "encoded-password");
        Member olderMember = new Member("솝트", "sopt@example.com", "encoded-password");

        when(memberRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc())
                .thenReturn(List.of(newestMember, olderMember));

        List<MemberSummaryResponse> responses = memberService.getMembers();

        assertEquals(2, responses.size());
        assertEquals("은서", responses.get(0).name());
        assertEquals("솝트", responses.get(1).name());
        verify(memberRepository).findAllByDeletedAtIsNullOrderByCreatedAtDesc();
    }
}
