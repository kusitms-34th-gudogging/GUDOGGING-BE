package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class GmailPermissionRequiredException extends ApplicationException {

    public GmailPermissionRequiredException() {
        super(GmailErrorCode.GMAIL_PERMISSION_REQUIRED);
    }
}
