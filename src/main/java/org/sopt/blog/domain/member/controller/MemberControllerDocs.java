package org.sopt.blog.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sopt.blog.domain.member.controller.dto.request.MemberRequest;
import org.sopt.blog.domain.member.controller.dto.response.MemberResponse;
import org.sopt.blog.global.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    })
    @Operation(summary = "회원 생성", description = "이름을 받아 회원을 생성합니다.")
    ResponseEntity<ApiResponse<MemberResponse>> createMember(@Valid MemberRequest request);

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @Operation(summary = "회원 삭제", description = "memberId로 회원을 삭제합니다.")
    ResponseEntity<ApiResponse<Void>> deleteMember(Long memberId);
}
