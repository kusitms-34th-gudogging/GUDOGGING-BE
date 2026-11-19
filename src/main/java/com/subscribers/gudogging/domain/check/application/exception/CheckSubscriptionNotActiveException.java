package com.subscribers.gudogging.domain.check.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class CheckSubscriptionNotActiveException extends ApplicationException {

    public CheckSubscriptionNotActiveException() {
        super(CheckErrorCode.CHECK_SUBSCRIPTION_NOT_ACTIVE);
    }
}
