package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class SubscriptionNotEndedException extends ApplicationException {

    public SubscriptionNotEndedException() {
        super(SubscriptionErrorCode.SUBSCRIPTION_NOT_ENDED);
    }
}
