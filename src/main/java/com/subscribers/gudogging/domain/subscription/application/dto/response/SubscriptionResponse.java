package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 방금 등록한 구독 항목 (A-05). 등록 직후라 결제 기준일(billing_anchor_on)이 곧 다음 결제일이다.
 */
public record SubscriptionResponse(
    UUID subscriptionId,
    String serviceName,
    String logoUrl,
    int amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod
) {

    public static SubscriptionResponse from(Subscription subscription) {
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getName(),
            serviceCatalog != null ? serviceCatalog.getLogoUrl() : null,
            subscription.getAmount(),
            subscription.getBillingCycleValue(),
            subscription.getBillingCycleUnit(),
            subscription.getBillingAnchorOn(),
            subscription.getPaymentMethod()
        );
    }
}
