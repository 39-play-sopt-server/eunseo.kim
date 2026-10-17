package org.sopt.blog.global.common.exception;

public class CustomException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String errorMsg;

    public CustomException(ErrorCode errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public String getMessage() {
        return errorCode.getMessage();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}

