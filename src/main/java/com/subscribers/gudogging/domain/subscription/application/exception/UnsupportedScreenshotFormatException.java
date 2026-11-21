package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class UnsupportedScreenshotFormatException extends ApplicationException {

    public UnsupportedScreenshotFormatException() {
        super(SubscriptionErrorCode.UNSUPPORTED_SCREENSHOT_FORMAT);
    }
}
