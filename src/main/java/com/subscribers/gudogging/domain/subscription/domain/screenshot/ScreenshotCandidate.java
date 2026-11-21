package com.subscribers.gudogging.domain.subscription.domain.screenshot;

import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 스크린샷에서 찾은 구독 후보 1건 (A-09 항목). serviceId는 OCR 서버가 매칭한 사전 등록 서비스 ID다.
 * amount · nextBillingDate가 null이면 reviewFields에 해당 필드가 반드시 들어 있다.
 */
public record ScreenshotCandidate(
    UUID serviceId,
    Integer amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod,
    boolean needsReview,
    List<ScreenshotReviewField> reviewFields
) {

    private static final int MIN_BILLING_CYCLE_VALUE = 1;
    private static final int MAX_BILLING_CYCLE_VALUE = 31;
    private static final int DEFAULT_BILLING_CYCLE_VALUE = 1;
    private static final BillingCycleUnit DEFAULT_BILLING_CYCLE_UNIT = BillingCycleUnit.MONTH;

    /**
     * OCR 결과로 후보를 만든다. 결제 주기를 못 정했으면 1개월 + BILLING_CYCLE, 금액·다음 결제일이 없으면 AMOUNT · NEXT_BILLING_DATE를 넣는다.
     * 결제 수단을 모르면 iOS 구독 화면이므로 APP_STORE로 둔다.
     */
    public static ScreenshotCandidate of(UUID serviceId, Integer amount, Integer billingCycleValue,
                                         BillingCycleUnit billingCycleUnit, LocalDate nextBillingDate,
                                         PaymentMethod paymentMethod, boolean needsReview,
                                         Collection<ScreenshotReviewField> reviewFields) {
        Set<ScreenshotReviewField> fields = EnumSet.noneOf(ScreenshotReviewField.class);
        fields.addAll(reviewFields);

        boolean validCycle = billingCycleValue != null && billingCycleUnit != null
            && billingCycleValue >= MIN_BILLING_CYCLE_VALUE && billingCycleValue <= MAX_BILLING_CYCLE_VALUE;
        if (!validCycle) {
            fields.add(ScreenshotReviewField.BILLING_CYCLE);
        }
        return create(serviceId, amount,
            validCycle ? billingCycleValue : DEFAULT_BILLING_CYCLE_VALUE,
            validCycle ? billingCycleUnit : DEFAULT_BILLING_CYCLE_UNIT,
            nextBillingDate,
            paymentMethod != null ? paymentMethod : PaymentMethod.APP_STORE,
            needsReview, fields);
    }

    /**
     * 등록 API에서 막히지 않도록 서버가 보정한다 — 금액이 0 이하면 null + AMOUNT, 다음 결제일이 오늘 이전이면 NEXT_BILLING_DATE.
     */
    public ScreenshotCandidate correct(LocalDate today) {
        Set<ScreenshotReviewField> fields = EnumSet.noneOf(ScreenshotReviewField.class);
        fields.addAll(reviewFields);
        Integer correctedAmount = amount != null && amount > 0 ? amount : null;
        if (nextBillingDate != null && nextBillingDate.isBefore(today)) {
            fields.add(ScreenshotReviewField.NEXT_BILLING_DATE);
        }
        return create(serviceId, correctedAmount, billingCycleValue, billingCycleUnit, nextBillingDate,
            paymentMethod, needsReview, fields);
    }

    private static ScreenshotCandidate create(UUID serviceId, Integer amount, int billingCycleValue,
                                              BillingCycleUnit billingCycleUnit, LocalDate nextBillingDate,
                                              PaymentMethod paymentMethod, boolean needsReview,
                                              Set<ScreenshotReviewField> fields) {
        if (amount == null) {
            fields.add(ScreenshotReviewField.AMOUNT);
        }
        if (nextBillingDate == null) {
            fields.add(ScreenshotReviewField.NEXT_BILLING_DATE);
        }
        return new ScreenshotCandidate(serviceId, amount, billingCycleValue, billingCycleUnit, nextBillingDate,
            paymentMethod, needsReview || !fields.isEmpty(), List.copyOf(fields));
    }
}
