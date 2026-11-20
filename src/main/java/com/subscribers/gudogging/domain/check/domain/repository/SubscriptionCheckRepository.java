package com.subscribers.gudogging.domain.check.domain.repository;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionCheckRepository extends JpaRepository<SubscriptionCheck, UUID> {

    List<SubscriptionCheck> findAllBySubscriptionInAndDeletedFalse(Collection<Subscription> subscriptions);

    Optional<SubscriptionCheck> findFirstBySubscriptionAndDeletedFalseOrderByCreatedAtDesc(Subscription subscription);

    @Query("""
        select c
        from SubscriptionCheck c
        join fetch c.subscription s
        left join fetch s.serviceCatalog
        where c.id = :id
          and s.user = :user
          and c.deleted = false
          and s.deleted = false
        """)
    Optional<SubscriptionCheck> findWithSubscriptionByIdAndUser(@Param("id") UUID id, @Param("user") User user);
}
