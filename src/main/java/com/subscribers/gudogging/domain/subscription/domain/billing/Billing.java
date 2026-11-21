package com.subscribers.gudogging.domain.subscription.domain.billing;

import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import java.time.LocalDate;

/**
 * 결제 1건 — 결제일과 그날 적용된 결제 조건의 금액 · 주기.
 */
public record Billing(LocalDate date, int amount, int cycleValue, BillingCycleUnit cycleUnit) {
}
