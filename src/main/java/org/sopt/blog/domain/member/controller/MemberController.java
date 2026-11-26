package org.sopt.blog.domain.member.controller;

import org.sopt.blog.domain.member.service.MemberService;
import org.sopt.blog.global.common.response.ApiResponse;
import org.sopt.blog.domain.member.controller.dto.response.MemberSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public ResponseEntity<Long> me(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberId);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberSummaryResponse>>> getMembers() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "회원 목록 조회에 성공했습니다.",
                        memberService.getMembers()
                )
        );
    }
}
