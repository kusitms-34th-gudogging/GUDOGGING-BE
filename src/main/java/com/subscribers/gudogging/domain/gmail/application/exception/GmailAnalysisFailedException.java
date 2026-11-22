package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class GmailAnalysisFailedException extends ApplicationException {

    public GmailAnalysisFailedException() {
        super(GmailErrorCode.GMAIL_ANALYSIS_FAILED);
    }
}
