package com.subscribers.gudogging.domain.gmail.domain;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.billing.BillingCycle;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 서비스 목록과 맞춘 Gmail 구독 후보 1건 (A-13 항목). amount · nextBillingDate가 null이면 reviewFields에 해당 필드가 들어 있다.
 */
public record GmailCandidate(
    ServiceCatalog service,
    Integer amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod,
    LocalDate paidDate,
    boolean needsReview,
    List<GmailReviewField> reviewFields
) {

    private static final int MIN_BILLING_CYCLE_VALUE = 1;
    private static final int MAX_BILLING_CYCLE_VALUE = 31;
    private static final int DEFAULT_BILLING_CYCLE_VALUE = 1;
    private static final BillingCycleUnit DEFAULT_BILLING_CYCLE_UNIT = BillingCycleUnit.MONTH;

    /**
     * 추출 결과를 보정해 후보를 만든다.
     * 금액이 0 이하면 null, 주기를 모르면 1개월, 다음 결제일은 메일 값이 오늘 이후면 그대로 쓰고 아니면 결제일 + 주기로 오늘 이후 첫 날짜를 계산한다.
     */
    public static GmailCandidate of(ServiceCatalog service, GmailExtractedPayment payment, LocalDate today) {
        Set<GmailReviewField> fields = EnumSet.noneOf(GmailReviewField.class);

        Integer amount = payment.amount() != null && payment.amount() > 0 ? payment.amount() : null;
        if (amount == null) {
            fields.add(GmailReviewField.AMOUNT);
        }

        boolean validCycle = payment.billingCycleValue() != null && payment.billingCycleUnit() != null
            && payment.billingCycleValue() >= MIN_BILLING_CYCLE_VALUE
            && payment.billingCycleValue() <= MAX_BILLING_CYCLE_VALUE;
        int cycleValue = validCycle ? payment.billingCycleValue() : DEFAULT_BILLING_CYCLE_VALUE;
        BillingCycleUnit cycleUnit = validCycle ? payment.billingCycleUnit() : DEFAULT_BILLING_CYCLE_UNIT;
        if (!validCycle) {
            fields.add(GmailReviewField.BILLING_CYCLE);
        }

        LocalDate nextBillingDate = nextBillingDate(payment, validCycle, cycleValue, cycleUnit, today);
        if (nextBillingDate == null) {
            fields.add(GmailReviewField.NEXT_BILLING_DATE);
        }

        PaymentMethod paymentMethod = payment.paymentMethod() != null ? payment.paymentMethod() : PaymentMethod.WEB_DIRECT;
        return new GmailCandidate(service, amount, cycleValue, cycleUnit, nextBillingDate, paymentMethod,
            payment.paidDate(), payment.uncertain() || !fields.isEmpty(), List.copyOf(fields));
    }

    private static LocalDate nextBillingDate(GmailExtractedPayment payment, boolean validCycle, int cycleValue,
                                             BillingCycleUnit cycleUnit, LocalDate today) {
        if (payment.nextBillingDate() != null && payment.nextBillingDate().isAfter(today)) {
            return payment.nextBillingDate();
        }
        if (payment.paidDate() == null || !validCycle) {
            return null;
        }
        return new BillingCycle(cycleValue, cycleUnit, payment.paidDate()).firstDateOnOrAfter(today.plusDays(1));
    }
}
