package com.subscribers.gudogging.domain.notification.application.dto.response;

import com.subscribers.gudogging.domain.user.domain.entity.User;

/**
 * 결제 3일 전 점검 알림(D-3) 수신 여부. D-1 · 해지 확인 알림은 이 값과 관계없이 발송한다.
 */
public record NotificationSettingResponse(boolean paymentReminderEnabled) {

    public static NotificationSettingResponse from(User user) {
        return new NotificationSettingResponse(user.isCheckNotificationEnabled());
    }
}
