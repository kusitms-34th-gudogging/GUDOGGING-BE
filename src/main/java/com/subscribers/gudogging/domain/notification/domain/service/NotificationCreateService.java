package com.subscribers.gudogging.domain.notification.domain.service;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationCreateService {

    private final NotificationRepository notificationRepository;

    public List<Notification> createNotifications(List<Notification> notifications) {
        return notificationRepository.saveAll(notifications);
    }
}
