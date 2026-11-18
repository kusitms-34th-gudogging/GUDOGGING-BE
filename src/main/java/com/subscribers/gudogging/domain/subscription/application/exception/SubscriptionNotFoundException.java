package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class SubscriptionNotFoundException extends ApplicationException {

    public SubscriptionNotFoundException() {
        super(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }
}
