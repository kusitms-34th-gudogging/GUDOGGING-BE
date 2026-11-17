package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorCode implements ErrorCodeInterface {

    DUPLICATE_SUBSCRIPTION(40401, HttpStatus.CONFLICT, "이미 구독 중인 서비스가 포함되어 있습니다."),
    DUPLICATE_SERVICE_IN_REQUEST(40402, HttpStatus.BAD_REQUEST, "같은 서비스를 중복해서 등록할 수 없습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
