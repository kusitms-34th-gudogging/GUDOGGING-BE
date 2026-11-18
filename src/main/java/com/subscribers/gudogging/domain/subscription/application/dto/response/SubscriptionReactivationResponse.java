package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.util.UUID;

public record SubscriptionReactivationResponse(
    UUID subscriptionId,
    SubscriptionStatus status
) {

    public static SubscriptionReactivationResponse from(Subscription subscription) {
        return new SubscriptionReactivationResponse(subscription.getId(), subscription.getStatus());
    }
}
