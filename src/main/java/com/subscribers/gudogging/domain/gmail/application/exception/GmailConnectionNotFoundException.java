package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class GmailConnectionNotFoundException extends ApplicationException {

    public GmailConnectionNotFoundException() {
        super(GmailErrorCode.GMAIL_CONNECTION_NOT_FOUND);
    }
}
