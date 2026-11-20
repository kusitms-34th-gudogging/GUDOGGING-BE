package com.subscribers.gudogging.domain.subscription.domain.repository;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionDraftRepository {

    void save(SubscriptionDraft subscriptionDraft);

    Optional<SubscriptionDraft> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
