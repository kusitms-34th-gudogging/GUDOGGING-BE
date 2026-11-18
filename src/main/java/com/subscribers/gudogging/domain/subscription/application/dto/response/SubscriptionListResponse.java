package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.time.LocalDate;
import java.util.List;

/**
 * 구독 목록 (선택한 탭). monthlyTotal은 탭 구독의 월 환산 합계 — 구독 중 '월 N원' / 구독 종료 '월 N원 절약'.
 */
public record SubscriptionListResponse(
    int count,
    int activeCount,
    int endedCount,
    long monthlyTotal,
    List<SubscriptionSummaryResponse> subscriptions
) {

    public static SubscriptionListResponse of(List<Subscription> tabSubscriptions, int activeCount, int endedCount,
                                              LocalDate today) {
        long monthlyTotal = tabSubscriptions.stream()
            .mapToLong(Subscription::monthlyAmount)
            .sum();
        List<SubscriptionSummaryResponse> subscriptions = tabSubscriptions.stream()
            .map(subscription -> SubscriptionSummaryResponse.from(subscription, today))
            .toList();
        return new SubscriptionListResponse(tabSubscriptions.size(), activeCount, endedCount, monthlyTotal,
            subscriptions);
    }
}
