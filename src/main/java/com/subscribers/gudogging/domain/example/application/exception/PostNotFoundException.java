package com.subscribers.gudogging.domain.example.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class PostNotFoundException extends ApplicationException {

    public PostNotFoundException() {
        super(PostErrorCode.POST_NOT_FOUND);
    }
}
