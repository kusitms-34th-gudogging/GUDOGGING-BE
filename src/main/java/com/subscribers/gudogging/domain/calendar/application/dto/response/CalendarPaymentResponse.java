package com.subscribers.gudogging.domain.calendar.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.billing.Billing;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.UUID;

/**
 * 날짜별 결제 1건 (C-02 목록 항목). 금액 · 주기는 그 결제일에 적용된 결제 조건, 직접 입력 구독은 logoUrl = null.
 */
public record CalendarPaymentResponse(
    UUID subscriptionId,
    String serviceName,
    String logoUrl,
    int amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    PaymentMethod paymentMethod
) {

    public static CalendarPaymentResponse of(Subscription subscription, Billing billing) {
        String logoUrl = subscription.getServiceCatalog() == null
            ? null
            : subscription.getServiceCatalog().getLogoUrl();
        return new CalendarPaymentResponse(subscription.getId(), subscription.getName(), logoUrl,
            billing.amount(), billing.cycleValue(), billing.cycleUnit(), subscription.getPaymentMethod());
    }
}
