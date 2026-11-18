package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.List;

public record SubscriptionDeletionResponse(
    int deletedCount
) {

    public static SubscriptionDeletionResponse from(List<Subscription> deletedSubscriptions) {
        return new SubscriptionDeletionResponse(deletedSubscriptions.size());
    }
}
