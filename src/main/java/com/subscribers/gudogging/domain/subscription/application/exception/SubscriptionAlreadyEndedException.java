package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class SubscriptionAlreadyEndedException extends ApplicationException {

    public SubscriptionAlreadyEndedException() {
        super(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ENDED);
    }
}
