package com.subscribers.gudogging.domain.home.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.UUID;

/**
 * 잘 안 쓰는 구독 미리보기 항목 (H-02). scorePercent는 가장 최근 점검 활용도.
 */
public record HomeUsageItemResponse(
    UUID subscriptionId,
    String serviceName,
    String logoUrl,
    int amount,
    int scorePercent
) {

    public static HomeUsageItemResponse of(Subscription subscription, SubscriptionCheck latestCheck) {
        return new HomeUsageItemResponse(subscription.getId(), subscription.getName(),
            HomeResponse.logoUrlOf(subscription), subscription.getAmount(), latestCheck.getScorePercent());
    }
}
