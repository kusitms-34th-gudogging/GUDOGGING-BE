package com.subscribers.gudogging.domain.check.domain.repository;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionCheckCancelReasonRepository extends JpaRepository<SubscriptionCheckCancelReason, UUID> {
}
