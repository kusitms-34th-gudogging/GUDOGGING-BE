package com.subscribers.gudogging.domain.notification.domain.repository;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllBySubscriptionInAndDeletedFalse(Collection<Subscription> subscriptions);

    boolean existsByUserAndIsReadFalseAndDeletedFalseAndSentAtGreaterThanEqual(User user, LocalDateTime from);
}
