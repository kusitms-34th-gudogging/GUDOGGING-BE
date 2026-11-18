package com.subscribers.gudogging.domain.subscription.domain.repository;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionBillingHistoryRepository extends JpaRepository<SubscriptionBillingHistory, UUID> {

    List<SubscriptionBillingHistory> findAllBySubscriptionInAndDeletedFalse(Collection<Subscription> subscriptions);
}
