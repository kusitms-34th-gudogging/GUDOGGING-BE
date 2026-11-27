package com.subscribers.gudogging.domain.subscription.domain.event;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 구독 등록 · 수정 · 재등록으로 다음 결제일이 정해졌다는 이벤트. 커밋 뒤 알림 즉시 발송(NOTIFICATION-008)에 쓴다.
 */
public record SubscriptionBillingChangedEvent(UUID userId, List<UUID> subscriptionIds) {

    public static SubscriptionBillingChangedEvent of(UUID userId, Collection<Subscription> subscriptions) {
        return new SubscriptionBillingChangedEvent(userId, subscriptions.stream()
            .map(Subscription::getId)
            .toList());
    }
}
