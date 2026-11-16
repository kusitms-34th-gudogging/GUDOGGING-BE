package com.subscribers.gudogging.domain.auth.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class AppleServerException extends ApplicationException {

    public AppleServerException() {
        super(AuthErrorCode.APPLE_SERVER_ERROR);
    }
}
