package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidGmailAuthCodeException extends ApplicationException {

    public InvalidGmailAuthCodeException() {
        super(GmailErrorCode.INVALID_GMAIL_AUTH_CODE);
    }
}
