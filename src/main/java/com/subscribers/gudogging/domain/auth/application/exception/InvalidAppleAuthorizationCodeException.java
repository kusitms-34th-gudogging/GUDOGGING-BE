package com.subscribers.gudogging.domain.auth.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidAppleAuthorizationCodeException extends ApplicationException {

    public InvalidAppleAuthorizationCodeException() {
        super(AuthErrorCode.INVALID_APPLE_AUTHORIZATION_CODE);
    }
}
