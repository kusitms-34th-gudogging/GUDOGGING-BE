package com.subscribers.gudogging.domain.notification.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCodeInterface {

    NOTIFICATION_NOT_FOUND(40701, HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
