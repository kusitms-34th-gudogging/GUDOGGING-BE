package com.subscribers.gudogging.domain.notification.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationResponseCode implements ResponseCodeInterface {

    PUSH_TOKEN_REGISTERED(200, HttpStatus.OK, "푸시 토큰을 등록했습니다."),
    NOTIFICATION_SETTING_FOUND(200, HttpStatus.OK, "알림 설정을 조회했습니다."),
    NOTIFICATION_SETTING_UPDATED(200, HttpStatus.OK, "알림 설정을 변경했습니다."),
    NOTIFICATIONS_FOUND(200, HttpStatus.OK, "알림 목록을 조회했습니다."),
    NOTIFICATION_READ(200, HttpStatus.OK, "알림을 읽음 처리했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
