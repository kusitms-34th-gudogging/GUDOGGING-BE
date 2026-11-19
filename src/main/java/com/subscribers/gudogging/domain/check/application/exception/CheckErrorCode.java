package com.subscribers.gudogging.domain.check.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CheckErrorCode implements ErrorCodeInterface {

    CHECK_SUBSCRIPTION_NOT_ACTIVE(40501, HttpStatus.CONFLICT, "구독 중인 구독만 점검할 수 있습니다."),
    INVALID_CHECKUP_ANSWERS(40502, HttpStatus.BAD_REQUEST, "이용도 · 만족도 · 계획 응답을 하나씩 보내야 합니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
