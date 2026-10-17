package org.sopt.blog.global.common.response;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
    // 데이터 있는 성공 응답
    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    // 데이터 없는 성공 응답 (작성/수정/삭제용)
    public static ApiResponse<Void> success(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // 실패 응답 (2단계에서 전역 예외처리에서 사용)
    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}