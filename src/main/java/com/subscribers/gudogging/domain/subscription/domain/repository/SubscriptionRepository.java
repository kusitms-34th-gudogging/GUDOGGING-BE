package com.subscribers.gudogging.domain.subscription.domain.repository;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
}
