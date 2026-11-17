package com.subscribers.gudogging.domain.notification.domain.repository;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllBySubscriptionInAndDeletedFalse(Collection<Subscription> subscriptions);
}
