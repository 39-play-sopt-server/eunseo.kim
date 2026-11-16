package org.sopt.blog.domain.member.controller;

import org.sopt.blog.domain.member.service.MemberService;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
public class MemberController implements MemberControllerDocs {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(200, "회원이 삭제되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<Long> me(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberId);
    }
}
