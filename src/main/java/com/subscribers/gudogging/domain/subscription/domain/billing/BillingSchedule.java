package com.subscribers.gudogging.domain.subscription.domain.billing;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 구독 결제 일정 — 결제 조건 이력(수정 전 조건)과 현재 조건을 이어 붙여 결제일 · 결제 횟수 · 금액을 계산한다 (ERD 6.2). 결제 금액은 결제일마다 그때 조건의 금액이다.
 */
public final class BillingSchedule {

    private final Subscription subscription;
    private final List<Period> periods;

    private BillingSchedule(Subscription subscription, List<Period> periods) {
        this.subscription = subscription;
        this.periods = periods;
    }

    /**
     * 이력은 기준일 순(같으면 저장 순)으로 이어 붙인다. 이력 조건의 결제일은 기준일 ~ 수정일(ended_on)이고, 다음 조건의 기준일과 같은 날은 다음 조건으로 센다.
     */
    public static BillingSchedule of(Subscription subscription, Collection<SubscriptionBillingHistory> histories) {
        List<SubscriptionBillingHistory> sortedHistories = histories.stream()
            .sorted(Comparator.comparing(SubscriptionBillingHistory::getBillingAnchorOn)
                .thenComparing(SubscriptionBillingHistory::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        List<Period> periods = new ArrayList<>();
        for (int i = 0; i < sortedHistories.size(); i++) {
            SubscriptionBillingHistory history = sortedHistories.get(i);
            LocalDate nextAnchorOn = i + 1 < sortedHistories.size()
                ? sortedHistories.get(i + 1).getBillingAnchorOn()
                : subscription.getBillingAnchorOn();
            LocalDate lastBillingOn = earlierOf(history.getEndedOn(), nextAnchorOn.minusDays(1));
            periods.add(new Period(history.billingCycle(), history.getAmount(), history.getEndedOn(), lastBillingOn));
        }
        periods.add(new Period(subscription.billingCycle(), subscription.getAmount(), null, LocalDate.MAX));
        return new BillingSchedule(subscription, List.copyOf(periods));
    }

    /**
     * base 이후(포함) 첫 결제일. base가 수정일 전이면 그때 조건의 결제일부터 보고, 수정일부터는 수정한 조건(사용자가 입력한 다음 결제일)을 따른다.
     */
    public LocalDate nextBillingOn(LocalDate base) {
        for (Period period : periods) {
            if (period.endedOn() != null && !base.isBefore(period.endedOn())) {
                continue;
            }
            LocalDate billingOn = period.cycle().firstDateOnOrAfter(base);
            if (!billingOn.isAfter(period.lastBillingOn())) {
                return billingOn;
            }
        }
        throw new IllegalStateException("현재 결제 조건은 결제일 상한이 없어야 한다.");
    }

    /**
     * 지금까지 결제 횟수: 등록일 ~ (구독 종료면 해지일, 구독 중이면 today) 사이 결제일 수.
     */
    public int paidCount(LocalDate today) {
        return Math.toIntExact(periods.stream()
            .mapToLong(period -> paidCountOf(period, today))
            .sum());
    }

    /**
     * 지금까지 결제 금액: 조건마다 결제 횟수 × 그때 금액의 합.
     */
    public long totalPaidAmount(LocalDate today) {
        long total = 0;
        for (Period period : periods) {
            total = Math.addExact(total, Math.multiplyExact(paidCountOf(period, today), period.amount()));
        }
        return total;
    }

    private long paidCountOf(Period period, LocalDate today) {
        LocalDate paidUntil = subscription.getStatus() == SubscriptionStatus.ENDED
            ? subscription.getCanceledOn()
            : today;
        return period.cycle().countBetween(subscription.startedOn(), earlierOf(paidUntil, period.lastBillingOn()));
    }

    private static LocalDate earlierOf(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /**
     * 결제 조건 하나의 적용 구간. endedOn은 이력 조건의 수정일(현재 조건은 null), lastBillingOn은 이 조건으로 세는 마지막 결제일 상한.
     */
    private record Period(BillingCycle cycle, int amount, LocalDate endedOn, LocalDate lastBillingOn) {
    }
}
