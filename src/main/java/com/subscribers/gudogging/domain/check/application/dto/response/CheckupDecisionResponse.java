package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import java.util.UUID;

public record CheckupDecisionResponse(
    UUID checkupId,
    CheckDecision decision
) {

    public static CheckupDecisionResponse from(SubscriptionCheck subscriptionCheck) {
        return new CheckupDecisionResponse(subscriptionCheck.getId(), subscriptionCheck.getDecision());
    }
}
