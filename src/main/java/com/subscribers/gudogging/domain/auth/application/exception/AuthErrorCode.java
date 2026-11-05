package com.subscribers.gudogging.domain.auth.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCodeInterface {

    INVALID_GOOGLE_ID_TOKEN(40101, HttpStatus.UNAUTHORIZED, "유효하지 않은 Google 인증 정보입니다."),
    EXPIRED_ACCESS_TOKEN(40103, HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(40104, HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
