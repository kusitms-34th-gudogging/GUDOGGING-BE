package com.subscribers.gudogging.global.common.exception;

public abstract class ApplicationException extends RuntimeException {
    private final ErrorCodeInterface errorCode;

    public ApplicationException(ErrorCodeInterface errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApplicationException(ErrorCodeInterface errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCodeInterface getErrorCode() {
        return errorCode;
    }
}
