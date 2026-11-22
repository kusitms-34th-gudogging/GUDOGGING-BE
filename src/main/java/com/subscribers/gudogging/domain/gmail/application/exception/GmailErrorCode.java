package com.subscribers.gudogging.domain.gmail.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GmailErrorCode implements ErrorCodeInterface {

    INVALID_GMAIL_AUTH_CODE(40801, HttpStatus.BAD_REQUEST, "Google 인증 코드가 유효하지 않습니다."),
    GMAIL_PERMISSION_REQUIRED(40802, HttpStatus.BAD_REQUEST, "Gmail 읽기 권한 동의가 필요합니다."),
    GMAIL_CONNECTION_NOT_FOUND(40803, HttpStatus.NOT_FOUND, "연동된 Gmail이 없습니다."),
    GOOGLE_OAUTH_SERVER_ERROR(40804, HttpStatus.BAD_GATEWAY, "Google 서버와 통신하지 못했습니다."),
    GMAIL_RECONNECT_REQUIRED(40805, HttpStatus.CONFLICT, "Gmail 권한이 만료되었습니다. 다시 연결해 주세요."),
    GMAIL_ANALYSIS_FAILED(40806, HttpStatus.BAD_GATEWAY, "결제 메일을 분석하지 못했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
