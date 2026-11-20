package com.subscribers.gudogging.domain.subscription.domain.draft;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 임시저장한 구독 1건 (A-04 목록 항목). serviceId가 null이면 직접 입력 서비스, 결제 정보가 모두 null이면 결제 정보 미입력 상태.
 */
public record SubscriptionDraftItem(
    UUID serviceId,
    String serviceName,
    Category category,
    Integer amount,
    Integer billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod
) {

    public static SubscriptionDraftItem of(UUID serviceId, String serviceName, Category category, Integer amount,
                                           Integer billingCycleValue, BillingCycleUnit billingCycleUnit,
                                           LocalDate nextBillingDate, PaymentMethod paymentMethod) {
        return new SubscriptionDraftItem(serviceId, serviceName, category, amount, billingCycleValue,
            billingCycleUnit, nextBillingDate, paymentMethod);
    }
}
