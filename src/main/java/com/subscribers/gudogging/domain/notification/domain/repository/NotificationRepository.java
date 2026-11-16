package com.subscribers.gudogging.domain.notification.domain.repository;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
