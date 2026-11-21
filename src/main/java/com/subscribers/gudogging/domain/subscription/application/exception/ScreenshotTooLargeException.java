package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class ScreenshotTooLargeException extends ApplicationException {

    public ScreenshotTooLargeException() {
        super(SubscriptionErrorCode.SCREENSHOT_TOO_LARGE);
    }
}
