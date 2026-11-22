package com.subscribers.gudogging.domain.gmail.domain;

import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import java.time.LocalDate;

/**
 * LLM이 메일에서 뽑은 구독 결제 1건. 값을 모르면 null이다.
 */
public record GmailExtractedPayment(
    String serviceName,
    Integer amount,
    Integer billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate paidDate,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod,
    boolean uncertain
) {
}
