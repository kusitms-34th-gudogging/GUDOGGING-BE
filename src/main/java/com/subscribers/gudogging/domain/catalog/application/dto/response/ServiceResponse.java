package com.subscribers.gudogging.domain.catalog.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import java.util.UUID;

/**
 * 서비스 목록 항목. defaultAmount는 구독 정보 입력(A-03) 결제 금액 기본값 — 없으면 null.
 */
public record ServiceResponse(
    UUID serviceId,
    String serviceName,
    Category category,
    String logoUrl,
    Integer defaultAmount
) {

    public static ServiceResponse from(ServiceCatalog serviceCatalog) {
        return new ServiceResponse(
            serviceCatalog.getId(),
            serviceCatalog.getName(),
            serviceCatalog.getCategory(),
            serviceCatalog.getLogoUrl(),
            serviceCatalog.getDefaultAmount()
        );
    }
}
