package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidSubscriptionSortException extends ApplicationException {

    public InvalidSubscriptionSortException() {
        super(SubscriptionErrorCode.INVALID_SUBSCRIPTION_SORT);
    }
}
