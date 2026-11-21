package com.subscribers.gudogging.domain.calendar.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CalendarErrorCode implements ErrorCodeInterface {

    INVALID_CALENDAR_MONTH(40601, HttpStatus.BAD_REQUEST, "조회할 수 없는 연월입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
