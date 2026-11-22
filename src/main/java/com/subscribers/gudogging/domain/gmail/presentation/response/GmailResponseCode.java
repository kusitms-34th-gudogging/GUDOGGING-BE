package com.subscribers.gudogging.domain.gmail.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GmailResponseCode implements ResponseCodeInterface {

    GMAIL_CONNECTED(201, HttpStatus.CREATED, "Gmail을 연동했습니다."),
    GMAIL_CONNECTION_FOUND(200, HttpStatus.OK, "Gmail 연동 상태를 조회했습니다."),
    GMAIL_DISCONNECTED(200, HttpStatus.OK, "Gmail 연동을 해제했습니다."),
    GMAIL_ANALYZED(200, HttpStatus.OK, "결제 메일을 분석했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
