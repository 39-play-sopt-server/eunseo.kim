package org.sopt.blog.domain.member.controller;

import jakarta.validation.Valid;
import org.sopt.blog.domain.member.controller.dto.request.MemberRequest;
import org.sopt.blog.domain.member.controller.dto.response.MemberResponse;
import org.sopt.blog.domain.member.service.MemberService;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
public class MemberController implements MemberControllerDocs {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 회원 생성
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(@Valid @RequestBody MemberRequest request) {
        MemberResponse response = memberService.createMember(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "회원이 생성되었습니다.", response));
    }

    // 회원 삭제
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(200, "회원이 삭제되었습니다."));
    }
}
