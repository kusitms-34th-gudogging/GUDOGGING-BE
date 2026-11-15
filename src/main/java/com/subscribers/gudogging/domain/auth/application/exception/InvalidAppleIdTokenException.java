package com.subscribers.gudogging.domain.auth.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidAppleIdTokenException extends ApplicationException {

    public InvalidAppleIdTokenException() {
        super(AuthErrorCode.INVALID_APPLE_ID_TOKEN);
    }
}
