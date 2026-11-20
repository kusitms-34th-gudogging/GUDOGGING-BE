package com.subscribers.gudogging.domain.check.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CheckResponseCode implements ResponseCodeInterface {

    CHECKUP_FORM_FOUND(200, HttpStatus.OK, "점검 문항을 조회했습니다."),
    CHECKUP_SUBMITTED(201, HttpStatus.CREATED, "구독 점검을 제출했습니다."),
    CHECKUP_RESULT_FOUND(200, HttpStatus.OK, "점검 결과를 조회했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
