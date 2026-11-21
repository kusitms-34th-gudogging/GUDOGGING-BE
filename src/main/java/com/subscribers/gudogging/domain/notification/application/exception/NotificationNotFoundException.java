package com.subscribers.gudogging.domain.notification.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class NotificationNotFoundException extends ApplicationException {

    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
