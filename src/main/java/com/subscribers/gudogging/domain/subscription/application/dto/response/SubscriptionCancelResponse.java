package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCancelResponse(
    UUID subscriptionId,
    SubscriptionStatus status,
    LocalDate canceledAt
) {

    public static SubscriptionCancelResponse from(Subscription subscription) {
        return new SubscriptionCancelResponse(
            subscription.getId(),
            subscription.getStatus(),
            subscription.getCanceledOn()
        );
    }
}
