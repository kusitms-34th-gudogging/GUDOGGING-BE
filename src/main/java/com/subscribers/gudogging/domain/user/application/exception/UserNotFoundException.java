package com.subscribers.gudogging.domain.user.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
