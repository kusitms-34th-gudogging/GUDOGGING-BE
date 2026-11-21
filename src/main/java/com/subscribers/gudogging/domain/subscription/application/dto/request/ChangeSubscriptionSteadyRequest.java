package com.subscribers.gudogging.domain.subscription.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeSubscriptionSteadyRequest(
    @NotNull Boolean isSteady
) {
}
