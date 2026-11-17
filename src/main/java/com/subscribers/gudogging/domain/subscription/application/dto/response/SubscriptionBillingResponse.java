package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 구독 정보 수정 결과 — 재계산한 연 환산 금액 · 다음 결제일.
 */
public record SubscriptionBillingResponse(
    UUID subscriptionId,
    int yearlyAmount,
    LocalDate nextBillingDate
) {

    public static SubscriptionBillingResponse from(Subscription subscription, LocalDate today) {
        return new SubscriptionBillingResponse(
            subscription.getId(),
            subscription.yearlyAmount(),
            subscription.nextBillingOn(today)
        );
    }
}
