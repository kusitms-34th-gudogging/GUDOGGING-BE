package com.subscribers.gudogging.domain.subscription.domain.repository;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
        select s.serviceCatalog.id
        from Subscription s
        where s.user = :user
          and s.status = com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus.ACTIVE
          and s.deleted = false
          and s.serviceCatalog is not null
        """)
    List<UUID> findActiveServiceIdsByUser(@Param("user") User user);
}
