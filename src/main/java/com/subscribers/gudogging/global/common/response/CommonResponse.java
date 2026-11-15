package com.subscribers.gudogging.global.common.response;


import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;


public record CommonResponse<T>(int code, String message, T data) {

    public static CommonResponse<Void> success(ResponseCodeInterface responseCode) {
        return new CommonResponse<>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    public static <T> CommonResponse<T> success(ResponseCodeInterface responseCode, T data) {
        return new CommonResponse<>(responseCode.getCode(), responseCode.getMessage(), data);
    }

    public static CommonResponse<Void> error(ErrorCodeInterface errorCode) {
        return new CommonResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static CommonResponse<Void> error(ErrorCodeInterface errorCode, String message) {
        return new CommonResponse<>(errorCode.getCode(), message, null);
    }

    public static <T> CommonResponse<T> error(ErrorCodeInterface errorCode, T data) {
        return new CommonResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
    }

}
