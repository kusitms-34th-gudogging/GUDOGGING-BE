package com.subscribers.gudogging.domain.auth.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthResponseCode implements ResponseCodeInterface {

    LOGIN_SUCCESS(200, HttpStatus.OK, "로그인되었습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
