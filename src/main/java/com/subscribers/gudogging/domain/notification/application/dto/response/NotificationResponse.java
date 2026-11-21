package com.subscribers.gudogging.domain.notification.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 목록 항목. 로고는 저장된 구독(묶음 알림이면 금액이 가장 큰 구독)의 서비스 로고이고, 직접 입력 구독이면 null.
 */
public record NotificationResponse(
    UUID notificationId,
    NotificationType type,
    String title,
    String body,
    UUID subscriptionId,
    String logoUrl,
    LocalDateTime sentAt,
    boolean read
) {

    public static NotificationResponse from(Notification notification) {
        ServiceCatalog serviceCatalog = notification.getSubscription().getServiceCatalog();
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getBody(),
            notification.getSubscription().getId(),
            serviceCatalog == null ? null : serviceCatalog.getLogoUrl(),
            notification.getSentAt(),
            notification.isRead()
        );
    }
}
