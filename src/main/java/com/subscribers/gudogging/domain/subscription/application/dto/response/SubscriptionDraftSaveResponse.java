package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import java.time.LocalDateTime;

public record SubscriptionDraftSaveResponse(
    LocalDateTime savedAt
) {

    public static SubscriptionDraftSaveResponse from(SubscriptionDraft subscriptionDraft) {
        return new SubscriptionDraftSaveResponse(subscriptionDraft.savedAt());
    }
}
