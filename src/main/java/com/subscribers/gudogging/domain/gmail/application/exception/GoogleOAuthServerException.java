package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class GoogleOAuthServerException extends ApplicationException {

    public GoogleOAuthServerException() {
        super(GmailErrorCode.GOOGLE_OAUTH_SERVER_ERROR);
    }
}
