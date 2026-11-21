package com.subscribers.gudogging.domain.home.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HomeResponseCode implements ResponseCodeInterface {

    HOME_FOUND(200, HttpStatus.OK, "홈 정보를 조회했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
