package com.subscribers.gudogging.domain.notification.application.usecase;

import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationUseCase {

    private final UserGetService userGetService;

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

    private User findUser(UUID userId) {
        return userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
    }
}
