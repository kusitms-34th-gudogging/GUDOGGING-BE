package com.subscribers.gudogging.global.common.exception;


public record ErrorResponse(String errorField, String errorMessage, Object inputValue) {

    public static ErrorResponse of(String field, String msg, Object value) {
        return new ErrorResponse(field, msg, value);
    }

}
