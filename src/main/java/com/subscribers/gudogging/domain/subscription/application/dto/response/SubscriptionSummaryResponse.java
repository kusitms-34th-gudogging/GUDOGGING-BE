package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.billing.BillingSchedule;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 구독 목록 항목. 구독 중이면 nextBillingDate · dDay, 구독 종료면 canceledAt · totalPaidAmount만 채운다. totalPaidAmount는 결제 조건 이력(수정 전
 * 조건)의 결제를 그때 금액으로 포함한다.
 */
public record SubscriptionSummaryResponse(
    UUID subscriptionId,
    String serviceName,
    String logoUrl,
    int amount,
    long monthlyAmount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    Integer dDay,
    LocalDate canceledAt,
    Long totalPaidAmount
) {

    public static SubscriptionSummaryResponse from(Subscription subscription,
                                                   List<SubscriptionBillingHistory> billingHistories, LocalDate today) {
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        boolean active = subscription.getStatus() == SubscriptionStatus.ACTIVE;
        LocalDate nextBillingDate = active ? subscription.nextBillingOn(today) : null;
        return new SubscriptionSummaryResponse(
            subscription.getId(),
            subscription.getName(),
            serviceCatalog != null ? serviceCatalog.getLogoUrl() : null,
            subscription.getAmount(),
            subscription.monthlyAmount(),
            subscription.getBillingCycleValue(),
            subscription.getBillingCycleUnit(),
            nextBillingDate,
            active ? (int) ChronoUnit.DAYS.between(today, nextBillingDate) : null,
            active ? null : subscription.getCanceledOn(),
            active ? null : BillingSchedule.of(subscription, billingHistories).totalPaidAmount(today)
        );
    }
}
