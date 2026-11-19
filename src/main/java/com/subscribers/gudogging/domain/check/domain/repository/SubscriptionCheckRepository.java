package com.subscribers.gudogging.domain.check.domain.repository;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionCheckRepository extends JpaRepository<SubscriptionCheck, UUID> {

    List<SubscriptionCheck> findAllBySubscriptionInAndDeletedFalse(Collection<Subscription> subscriptions);

    Optional<SubscriptionCheck> findFirstBySubscriptionAndDeletedFalseOrderByCreatedAtDesc(Subscription subscription);
}
