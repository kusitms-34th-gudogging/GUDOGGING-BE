package com.subscribers.gudogging.global.common.exception;

public class UnAuthorizedException extends ApplicationException {
    public UnAuthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
