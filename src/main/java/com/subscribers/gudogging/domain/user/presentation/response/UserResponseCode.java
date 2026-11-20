package com.subscribers.gudogging.domain.user.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserResponseCode implements ResponseCodeInterface {

    USER_WITHDRAWN(200, HttpStatus.OK, "회원 탈퇴되었습니다."),
    USER_ACCOUNT_FOUND(200, HttpStatus.OK, "내 정보를 조회했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
