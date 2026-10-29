package org.sopt.blog.global.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 공통
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(4001, HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(4002, HttpStatus.BAD_REQUEST, "요청 파라미터 타입이 올바르지 않습니다."),
    INVALID_JSON_FORMAT(4003, HttpStatus.BAD_REQUEST, "잘못된 JSON 형식입니다."),
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 게시글 관련
    POST_NOT_FOUND(1103, HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    // 회원 관련
    MEMBER_NOT_FOUND(1203, HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public Integer getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getMessage() { return message; }
}
