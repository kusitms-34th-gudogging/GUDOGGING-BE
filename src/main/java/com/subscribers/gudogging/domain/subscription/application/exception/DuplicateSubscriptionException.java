package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class DuplicateSubscriptionException extends ApplicationException {

    public DuplicateSubscriptionException() {
        super(SubscriptionErrorCode.DUPLICATE_SUBSCRIPTION);
    }
}
