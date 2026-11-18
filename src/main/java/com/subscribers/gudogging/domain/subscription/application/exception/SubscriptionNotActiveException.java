package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class SubscriptionNotActiveException extends ApplicationException {

    public SubscriptionNotActiveException() {
        super(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
    }
}
