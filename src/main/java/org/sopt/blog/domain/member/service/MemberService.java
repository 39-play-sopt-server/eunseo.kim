package org.sopt.blog.domain.member.service;

import org.sopt.blog.domain.member.entity.Member;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sopt.blog.domain.member.controller.dto.response.MemberSummaryResponse;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND, "id=" + memberId + " 번 회원이 존재하지 않습니다."));
        if (member.isDeleted()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND, "이미 탈퇴한 회원입니다.");
        }
        member.markDeleted(); // dirty checking으로 UPDATE 자동 실행
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> getMembers() {
        return memberRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(MemberSummaryResponse::from)
                .toList();
    }
}
