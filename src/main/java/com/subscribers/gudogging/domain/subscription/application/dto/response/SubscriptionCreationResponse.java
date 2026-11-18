package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.List;

/**
 * 구독 일괄 등록 결과 (A-05) — '등록된 구독 N'과 방금 등록한 구독 목록 (요청 순서).
 */
public record SubscriptionCreationResponse(
    int createdCount,
    List<SubscriptionResponse> subscriptions
) {

    public static SubscriptionCreationResponse from(List<Subscription> createdSubscriptions) {
        List<SubscriptionResponse> subscriptions = createdSubscriptions.stream()
            .map(SubscriptionResponse::from)
            .toList();
        return new SubscriptionCreationResponse(subscriptions.size(), subscriptions);
    }
}
