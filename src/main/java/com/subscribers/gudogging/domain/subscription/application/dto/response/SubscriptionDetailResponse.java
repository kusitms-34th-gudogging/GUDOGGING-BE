package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 구독 상세 — M-03 구독 중 · M-05 구독 종료 · C-03 캘린더 구독 상세.
 * baseDate가 있으면 그 날짜 기준으로 상태 · 다음 결제일 · D-n을 계산하고, 지금까지 결제 금액 · 횟수는 항상 오늘 기준이다.
 * 점검 필드(latestCheckup · pastCheckups · pastCheckupAverage)는 구독 점검 후속 스펙 전까지 null · 빈 목록으로 반환한다.
 */
public record SubscriptionDetailResponse(
    UUID subscriptionId,
    SubscriptionStatus status,
    String serviceName,
    String logoUrl,
    Category category,
    int amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    LocalDate startedAt,
    PaymentMethod paymentMethod,
    boolean isSteady,
    LocalDate canceledAt,
    int yearlyAmount,
    int totalPaidAmount,
    int paidCount,
    Integer dDay,
    LatestCheckupResponse latestCheckup,
    List<PastCheckupResponse> pastCheckups,
    PastCheckupAverageResponse pastCheckupAverage
) {

    public static SubscriptionDetailResponse of(Subscription subscription, LocalDate today, LocalDate baseDate) {
        LocalDate base = baseDate != null ? baseDate : today;
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        LocalDate nextBillingDate = nextBillingDateOf(subscription, base);
        return new SubscriptionDetailResponse(
            subscription.getId(),
            baseDate != null ? subscription.statusOn(baseDate) : subscription.getStatus(),
            subscription.getName(),
            serviceCatalog != null ? serviceCatalog.getLogoUrl() : null,
            subscription.getCategory(),
            subscription.getAmount(),
            subscription.getBillingCycleValue(),
            subscription.getBillingCycleUnit(),
            nextBillingDate,
            subscription.startedOn(),
            subscription.getPaymentMethod(),
            subscription.isSteadyUse(),
            subscription.getCanceledOn(),
            subscription.yearlyAmount(),
            subscription.totalPaidAmount(today),
            subscription.paidCount(today),
            nextBillingDate != null ? (int) ChronoUnit.DAYS.between(base, nextBillingDate) : null,
            null,
            List.of(),
            null
        );
    }

    /**
     * 기준일 이후(포함) 첫 결제일. 구독 종료이고 그 날짜가 해지일 이후면 null.
     */
    private static LocalDate nextBillingDateOf(Subscription subscription, LocalDate base) {
        LocalDate nextBillingDate = subscription.nextBillingOn(base);
        boolean ended = subscription.getStatus() == SubscriptionStatus.ENDED;
        return ended && nextBillingDate.isAfter(subscription.getCanceledOn()) ? null : nextBillingDate;
    }
}
