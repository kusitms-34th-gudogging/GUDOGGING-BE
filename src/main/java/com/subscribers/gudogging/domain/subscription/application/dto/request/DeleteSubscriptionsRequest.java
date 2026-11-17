package com.subscribers.gudogging.domain.subscription.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record DeleteSubscriptionsRequest(
    @NotEmpty @Size(max = 100) List<@NotNull UUID> subscriptionIds
) {
}
