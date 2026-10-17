package org.sopt.blog.global.common.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"code", "message", "errorMsg"})
public class ExceptionDto {
    private final Integer code;
    private final String message;
    private final String errorMsg;

    public ExceptionDto(ErrorCode errorCode, String errorMsg) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.errorMsg = errorMsg;
    }

    public static ExceptionDto of(ErrorCode errorCode, String errorMsg) {
        return new ExceptionDto(errorCode, errorMsg);
    }

    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public String getErrorMsg() { return errorMsg; }
}

