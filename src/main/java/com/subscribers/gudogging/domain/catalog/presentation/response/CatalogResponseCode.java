package com.subscribers.gudogging.domain.catalog.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CatalogResponseCode implements ResponseCodeInterface {

    SERVICES_FOUND(200, HttpStatus.OK, "서비스 목록을 조회했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
