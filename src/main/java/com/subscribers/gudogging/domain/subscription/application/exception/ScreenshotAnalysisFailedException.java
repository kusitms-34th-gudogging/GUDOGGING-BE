package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class ScreenshotAnalysisFailedException extends ApplicationException {

    public ScreenshotAnalysisFailedException() {
        super(SubscriptionErrorCode.SCREENSHOT_ANALYSIS_FAILED);
    }
}
