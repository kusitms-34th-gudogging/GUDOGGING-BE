package com.subscribers.gudogging.domain.check.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class CheckupNotFoundException extends ApplicationException {

    public CheckupNotFoundException() {
        super(CheckErrorCode.CHECKUP_NOT_FOUND);
    }
}
