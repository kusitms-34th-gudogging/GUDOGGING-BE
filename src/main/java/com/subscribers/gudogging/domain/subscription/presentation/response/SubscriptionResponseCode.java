package com.subscribers.gudogging.domain.subscription.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubscriptionResponseCode implements ResponseCodeInterface {

    SUBSCRIPTIONS_CREATED(201, HttpStatus.CREATED, "구독이 등록되었습니다."),
    SUBSCRIPTIONS_FOUND(200, HttpStatus.OK, "구독 목록을 조회했습니다."),
    SUBSCRIPTIONS_DELETED(200, HttpStatus.OK, "구독을 삭제했습니다."),
    SUBSCRIPTION_FOUND(200, HttpStatus.OK, "구독 상세를 조회했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
