package com.subscribers.gudogging.domain.subscription.domain.repository;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import java.util.UUID;

public interface SubscriptionDraftRepository {

    void save(SubscriptionDraft subscriptionDraft);

    void deleteByUserId(UUID userId);
}
