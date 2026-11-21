package com.subscribers.gudogging.domain.home.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 구독 점검하기 항목 (H-02). 이번 결제 주기에 점검하지 않은 구독만 담기므로 needsCheckup은 항상 true.
 */
public record HomeUpcomingPaymentResponse(
    UUID subscriptionId,
    String serviceName,
    String logoUrl,
    int amount,
    LocalDate nextBillingDate,
    int dDay,
    boolean needsCheckup
) {

    public static HomeUpcomingPaymentResponse of(Subscription subscription, LocalDate today) {
        LocalDate nextBillingDate = subscription.nextBillingOn(today);
        return new HomeUpcomingPaymentResponse(subscription.getId(), subscription.getName(),
            HomeResponse.logoUrlOf(subscription), subscription.getAmount(), nextBillingDate,
            Math.toIntExact(ChronoUnit.DAYS.between(today, nextBillingDate)), true);
    }
}
