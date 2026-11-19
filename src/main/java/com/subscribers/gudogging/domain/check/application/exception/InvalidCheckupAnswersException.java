package com.subscribers.gudogging.domain.check.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidCheckupAnswersException extends ApplicationException {

    public InvalidCheckupAnswersException() {
        super(CheckErrorCode.INVALID_CHECKUP_ANSWERS);
    }
}
