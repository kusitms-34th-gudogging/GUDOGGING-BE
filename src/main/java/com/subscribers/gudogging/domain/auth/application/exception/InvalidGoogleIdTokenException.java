package com.subscribers.gudogging.domain.auth.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidGoogleIdTokenException extends ApplicationException {

    public InvalidGoogleIdTokenException() {
        super(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN);
    }
}
