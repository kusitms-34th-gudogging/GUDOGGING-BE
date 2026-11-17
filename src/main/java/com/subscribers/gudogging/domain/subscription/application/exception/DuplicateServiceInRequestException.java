package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class DuplicateServiceInRequestException extends ApplicationException {

    public DuplicateServiceInRequestException() {
        super(SubscriptionErrorCode.DUPLICATE_SERVICE_IN_REQUEST);
    }
}
