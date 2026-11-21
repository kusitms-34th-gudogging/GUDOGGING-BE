package com.subscribers.gudogging.domain.notification.application.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 결제 3일 전 점검 알림(D-3) 수신 여부 (E-05 토글).
 */
public record UpdateNotificationSettingRequest(
    @NotNull Boolean paymentReminderEnabled
) {
}
