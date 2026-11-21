package com.subscribers.gudogging.domain.notification.domain.repository;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllBySubscriptionInAndDeletedFalse(Collection<Subscription> subscriptions);

    boolean existsByUserAndIsReadFalseAndDeletedFalseAndSentAtGreaterThanEqual(User user, LocalDateTime from);

    @Query("""
        select n from Notification n
        join fetch n.subscription s
        left join fetch s.serviceCatalog
        where n.user = :user and n.deleted = false and n.sentAt >= :from
        order by n.sentAt desc
        """)
    List<Notification> findRecentWithSubscriptionByUser(@Param("user") User user, @Param("from") LocalDateTime from);

    Optional<Notification> findByIdAndUserAndDeletedFalse(UUID id, User user);

    List<Notification> findAllBySubscriptionInAndTargetBillingOnGreaterThanEqual(Collection<Subscription> subscriptions,
                                                                                  LocalDate from);
}
