package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.UUID;

public record SubscriptionSteadyResponse(
    UUID subscriptionId,
    boolean isSteady
) {

    public static SubscriptionSteadyResponse from(Subscription subscription) {
        return new SubscriptionSteadyResponse(subscription.getId(), subscription.isSteadyUse());
    }
}
