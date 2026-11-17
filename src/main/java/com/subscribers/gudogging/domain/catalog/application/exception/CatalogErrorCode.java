package com.subscribers.gudogging.domain.catalog.application.exception;

import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CatalogErrorCode implements ErrorCodeInterface {

    SERVICE_NOT_FOUND(40301, HttpStatus.NOT_FOUND, "서비스를 찾을 수 없습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
