package com.subscribers.gudogging.domain.check.domain.repository;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionCheckRepository extends JpaRepository<SubscriptionCheck, UUID> {
}
