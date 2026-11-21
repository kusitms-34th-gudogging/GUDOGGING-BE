package com.subscribers.gudogging.domain.subscription.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorCode implements ErrorCodeInterface {

    DUPLICATE_SUBSCRIPTION(40401, HttpStatus.CONFLICT, "이미 구독 중인 서비스가 포함되어 있습니다."),
    DUPLICATE_SERVICE_IN_REQUEST(40402, HttpStatus.BAD_REQUEST, "같은 서비스를 중복해서 등록할 수 없습니다."),
    INVALID_SUBSCRIPTION_SORT(40403, HttpStatus.BAD_REQUEST, "구독 상태에 맞지 않는 정렬 기준입니다."),
    SUBSCRIPTION_NOT_FOUND(40404, HttpStatus.NOT_FOUND, "구독을 찾을 수 없습니다."),
    SUBSCRIPTION_NOT_ACTIVE(40405, HttpStatus.CONFLICT, "구독 중인 구독만 수정할 수 있습니다."),
    SUBSCRIPTION_ALREADY_ENDED(40406, HttpStatus.CONFLICT, "이미 종료된 구독입니다."),
    SUBSCRIPTION_NOT_ENDED(40407, HttpStatus.CONFLICT, "구독 종료된 구독만 재등록할 수 있습니다."),
    INVALID_SCREENSHOT_COUNT(40408, HttpStatus.BAD_REQUEST, "스크린샷은 1장부터 5장까지 올릴 수 있습니다."),
    UNSUPPORTED_SCREENSHOT_FORMAT(40409, HttpStatus.BAD_REQUEST, "PNG, JPG 이미지만 올릴 수 있습니다."),
    SCREENSHOT_TOO_LARGE(40410, HttpStatus.CONTENT_TOO_LARGE, "이미지 1장당 최대 10MB까지 업로드할 수 있습니다."),
    SCREENSHOT_ANALYSIS_FAILED(40411, HttpStatus.BAD_GATEWAY, "스크린샷을 분석하지 못했습니다. 잠시 후 다시 시도해주세요.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
