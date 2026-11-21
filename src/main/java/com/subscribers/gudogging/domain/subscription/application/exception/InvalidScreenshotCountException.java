package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidScreenshotCountException extends ApplicationException {

    public InvalidScreenshotCountException() {
        super(SubscriptionErrorCode.INVALID_SCREENSHOT_COUNT);
    }
}
