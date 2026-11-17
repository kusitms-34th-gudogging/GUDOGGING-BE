package com.subscribers.gudogging.domain.catalog.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import java.util.UUID;

public record ServiceResponse(
    UUID serviceId,
    String name,
    Category category,
    String logoUrl
) {

    public static ServiceResponse from(ServiceCatalog serviceCatalog) {
        return new ServiceResponse(
            serviceCatalog.getId(),
            serviceCatalog.getName(),
            serviceCatalog.getCategory(),
            serviceCatalog.getLogoUrl()
        );
    }
}
