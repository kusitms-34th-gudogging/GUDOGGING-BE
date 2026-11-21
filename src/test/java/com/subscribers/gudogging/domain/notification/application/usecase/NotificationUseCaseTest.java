package com.subscribers.gudogging.domain.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.Platform;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationUseCase")
class NotificationUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @InjectMocks
    private NotificationUseCase notificationUseCase;

    private final UUID userId = UUID.randomUUID();

    private User givenUser() {
        User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
        return user;
    }

    private void givenNoUser() {
        given(userGetService.findUserById(userId)).willReturn(Optional.empty());
    }

    @Nested
    @DisplayName("registerPushToken")
    class RegisterPushToken {

        @Test
        @DisplayName("토큰과 기기 종류를 회원에 저장한다")
        void savesTokenAndPlatform() {
            // given
            User user = givenUser();

            // when
            notificationUseCase.registerPushToken(userId, new RegisterPushTokenRequest("fcm-token", Platform.IOS));

            // then
            assertThat(user.getPushToken()).isEqualTo("fcm-token");
            assertThat(user.getPlatform()).isEqualTo(Platform.IOS);
        }

        @Test
        @DisplayName("이미 토큰이 있으면 새 토큰과 기기 종류로 덮어쓴다")
        void overwritesExistingToken() {
            // given
            User user = givenUser();
            user.updatePushToken("old-token", Platform.IOS);

            // when
            notificationUseCase.registerPushToken(userId,
                new RegisterPushTokenRequest("new-token", Platform.ANDROID));

            // then
            assertThat(user.getPushToken()).isEqualTo("new-token");
            assertThat(user.getPlatform()).isEqualTo(Platform.ANDROID);
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenNoUser();

            // when & then
            assertThatThrownBy(() -> notificationUseCase.registerPushToken(userId,
                new RegisterPushTokenRequest("fcm-token", Platform.IOS)))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getNotificationSetting")
    class GetNotificationSetting {

        @Test
        @DisplayName("회원의 설정값을 반환한다")
        void returnsSetting() {
            // given
            User user = givenUser();
            user.changeCheckNotification(false);

            // when
            NotificationSettingResponse response = notificationUseCase.getNotificationSetting(userId);

            // then
            assertThat(response.paymentReminderEnabled()).isFalse();
        }

        @Test
        @DisplayName("새로 가입한 회원은 true를 반환한다")
        void returnsTrueByDefault() {
            // given
            givenUser();

            // when
            NotificationSettingResponse response = notificationUseCase.getNotificationSetting(userId);

            // then
            assertThat(response.paymentReminderEnabled()).isTrue();
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenNoUser();

            // when & then
            assertThatThrownBy(() -> notificationUseCase.getNotificationSetting(userId))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateNotificationSetting")
    class UpdateNotificationSetting {

        @Test
        @DisplayName("false로 바꾸면 저장하고 false를 반환한다")
        void turnsOff() {
            // given
            User user = givenUser();

            // when
            NotificationSettingResponse response = notificationUseCase.updateNotificationSetting(userId,
                new UpdateNotificationSettingRequest(false));

            // then
            assertThat(user.isCheckNotificationEnabled()).isFalse();
            assertThat(response.paymentReminderEnabled()).isFalse();
        }

        @Test
        @DisplayName("true로 바꾸면 저장하고 true를 반환한다")
        void turnsOn() {
            // given
            User user = givenUser();
            user.changeCheckNotification(false);

            // when
            NotificationSettingResponse response = notificationUseCase.updateNotificationSetting(userId,
                new UpdateNotificationSettingRequest(true));

            // then
            assertThat(user.isCheckNotificationEnabled()).isTrue();
            assertThat(response.paymentReminderEnabled()).isTrue();
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenNoUser();

            // when & then
            assertThatThrownBy(() -> notificationUseCase.updateNotificationSetting(userId,
                new UpdateNotificationSettingRequest(true)))
                .isInstanceOf(UserNotFoundException.class);
        }
    }
}
