package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class GmailReconnectRequiredException extends ApplicationException {

    public GmailReconnectRequiredException() {
        super(GmailErrorCode.GMAIL_RECONNECT_REQUIRED);
    }
}
