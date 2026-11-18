package com.subscribers.gudogging.domain.catalog.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import java.util.List;

/**
 * 서비스 목록 · 검색 결과. 매칭되는 서비스가 없으면 빈 배열.
 */
public record ServiceListResponse(
    List<ServiceResponse> services
) {

    public static ServiceListResponse from(List<ServiceCatalog> serviceCatalogs) {
        List<ServiceResponse> services = serviceCatalogs.stream()
            .map(ServiceResponse::from)
            .toList();
        return new ServiceListResponse(services);
    }
}
