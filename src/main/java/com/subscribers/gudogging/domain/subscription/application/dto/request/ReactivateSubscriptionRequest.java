package com.subscribers.gudogging.domain.subscription.application.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReactivateSubscriptionRequest(
    @NotNull @FutureOrPresent LocalDate nextBillingDate
) {
}
