package com.subscribers.gudogging.domain.example.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum PostErrorCode implements ErrorCodeInterface {

    POST_TITLE_BLANK(40201, HttpStatus.BAD_REQUEST, "게시글 제목은 비어 있을 수 없습니다."),
    POST_NOT_FOUND(40202, HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    PostErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
