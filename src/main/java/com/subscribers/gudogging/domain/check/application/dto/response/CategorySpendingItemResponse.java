package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;

public record CategorySpendingItemResponse(
    String serviceName,
    String logoUrl,
    long monthlyAmount
) {

    public static CategorySpendingItemResponse from(Subscription subscription) {
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        return new CategorySpendingItemResponse(
            subscription.getName(),
            serviceCatalog != null ? serviceCatalog.getLogoUrl() : null,
            subscription.monthlyAmount()
        );
    }
}
