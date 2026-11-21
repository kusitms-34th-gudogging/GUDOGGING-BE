package com.subscribers.gudogging.domain.notification.application.dto.response;

import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import java.util.List;

public record NotificationListResponse(List<NotificationResponse> notifications) {

    public static NotificationListResponse from(List<Notification> notifications) {
        return new NotificationListResponse(notifications.stream()
            .map(NotificationResponse::from)
            .toList());
    }
}
