package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.billing.BillingSchedule;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 구독 상세 — M-03 구독 중 · M-05 구독 종료 · C-03 캘린더 구독 상세. baseDate가 있으면 그 날짜 기준으로 상태 · 다음 결제일 · D-n을 계산하고, 지금까지 결제 금액 · 횟수는 항상
 * 오늘 기준이며 결제 조건 이력(수정 전 조건)의 결제를 그때 금액으로 포함한다. 점검 필드(latestCheckup · pastCheckups · pastCheckupAverage)는 구독 점검 후속 스펙 전까지 null · 빈 목록으로 반환한다.
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
    long yearlyAmount,
    long totalPaidAmount,
    int paidCount,
    Integer dDay,
    LatestCheckupResponse latestCheckup,
    List<PastCheckupResponse> pastCheckups,
    PastCheckupAverageResponse pastCheckupAverage
) {

    public static SubscriptionDetailResponse of(Subscription subscription,
                                                List<SubscriptionBillingHistory> billingHistories, LocalDate today,
                                                LocalDate baseDate) {
        LocalDate base = baseDate != null ? baseDate : today;
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        BillingSchedule billingSchedule = BillingSchedule.of(subscription, billingHistories);
        LocalDate nextBillingDate = nextBillingDateOf(subscription, billingSchedule, base);
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
            billingSchedule.totalPaidAmount(today),
            billingSchedule.paidCount(today),
            nextBillingDate != null ? (int) ChronoUnit.DAYS.between(base, nextBillingDate) : null,
            null,
            List.of(),
            null
        );
    }

    /**
     * 기준일 이후(포함) 첫 결제일. 기준일이 구독 시작일(재등록하면 재등록일) 전이거나, 구독 종료이고 그 날짜가 해지일 이후면 null.
     */
    private static LocalDate nextBillingDateOf(Subscription subscription, BillingSchedule billingSchedule,
                                               LocalDate base) {
        if (base.isBefore(subscription.startedOn())) {
            return null;
        }
        LocalDate nextBillingDate = billingSchedule.nextBillingOn(base);
        boolean ended = subscription.getStatus() == SubscriptionStatus.ENDED;
        return ended && nextBillingDate.isAfter(subscription.getCanceledOn()) ? null : nextBillingDate;
    }
}
