package com.subscribers.gudogging.domain.subscription.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSubscriptionsRequest(
    @NotEmpty @Size(max = 50) List<@Valid SubscriptionItemRequest> subscriptions
) {
}
