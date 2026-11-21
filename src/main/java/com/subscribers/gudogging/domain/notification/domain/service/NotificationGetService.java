package com.subscribers.gudogging.domain.notification.domain.service;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.repository.NotificationRepository;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationGetService {

    private static final int RETENTION_DAYS = 30;

    private final NotificationRepository notificationRepository;

    public List<Notification> findNotificationsBySubscriptions(Collection<Subscription> subscriptions) {
        return notificationRepository.findAllBySubscriptionInAndDeletedFalse(subscriptions);
    }

    /**
     * 회원에게 읽지 않은 알림이 있는지 — 알림 목록과 같이 삭제 제외 · 최근 30일(sent_at) 알림만 본다.
     */
    public boolean hasUnreadNotification(User user, LocalDateTime now) {
        return notificationRepository.existsByUserAndIsReadFalseAndDeletedFalseAndSentAtGreaterThanEqual(user,
            now.minusDays(RETENTION_DAYS));
    }
}
