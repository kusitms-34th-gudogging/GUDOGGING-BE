package com.subscribers.gudogging.domain.notification.application.usecase;

import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationListResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.notification.application.exception.NotificationNotFoundException;
import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationUseCase {

    private final UserGetService userGetService;
    private final NotificationGetService notificationGetService;

    /**
     * 푸시 토큰과 기기 종류를 저장한다. 이미 있으면 덮어쓴다 (회원당 1개, 마지막 기기).
     */
    @Transactional
    public void registerPushToken(UUID userId, RegisterPushTokenRequest request) {
        User user = findUser(userId);
        user.updatePushToken(request.deviceToken(), request.platform());
    }

    public NotificationSettingResponse getNotificationSetting(UUID userId) {
        return NotificationSettingResponse.from(findUser(userId));
    }

    @Transactional
    public NotificationSettingResponse updateNotificationSetting(UUID userId,
                                                                 UpdateNotificationSettingRequest request) {
        User user = findUser(userId);
        user.changeCheckNotification(request.paymentReminderEnabled());
        return NotificationSettingResponse.from(user);
    }

    /**
     * 알림 목록 (H-04) — 최근 30일 알림을 최신순으로 내려준다.
     */
    public NotificationListResponse getNotifications(UUID userId) {
        User user = findUser(userId);
        return NotificationListResponse.from(notificationGetService.findRecentNotifications(user,
            LocalDateTime.now()));
    }

    /**
     * 알림을 읽음 처리한다. 이미 읽은 알림이어도 성공 — 홈 hasUnreadNotification에 바로 반영된다.
     */
    @Transactional
    public void readNotification(UUID userId, UUID notificationId) {
        User user = findUser(userId);
        Notification notification = notificationGetService.findNotification(user, notificationId)
            .orElseThrow(NotificationNotFoundException::new);
        notification.read();
    }

    private User findUser(UUID userId) {
        return userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
    }
}
