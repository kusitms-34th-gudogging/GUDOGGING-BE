package com.subscribers.gudogging.domain.notification.domain.service;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.repository.NotificationRepository;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationGetService {

    private final NotificationRepository notificationRepository;

    public List<Notification> findNotificationsBySubscriptions(Collection<Subscription> subscriptions) {
        return notificationRepository.findAllBySubscriptionInAndDeletedFalse(subscriptions);
    }
}
