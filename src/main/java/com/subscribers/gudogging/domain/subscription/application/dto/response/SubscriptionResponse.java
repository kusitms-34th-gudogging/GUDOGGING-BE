package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
    UUID subscriptionId,
    UUID serviceId,
    String name,
    String logoUrl,
    Category category,
    int amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingOn,
    PaymentMethod paymentMethod
) {

    public static SubscriptionResponse from(Subscription subscription) {
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        return new SubscriptionResponse(
            subscription.getId(),
            serviceCatalog != null ? serviceCatalog.getId() : null,
            subscription.getName(),
            serviceCatalog != null ? serviceCatalog.getLogoUrl() : null,
            subscription.getCategory(),
            subscription.getAmount(),
            subscription.getBillingCycleValue(),
            subscription.getBillingCycleUnit(),
            subscription.getBillingAnchorOn(),
            subscription.getPaymentMethod()
        );
    }
}
