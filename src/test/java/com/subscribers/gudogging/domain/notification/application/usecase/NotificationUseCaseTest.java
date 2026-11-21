package com.subscribers.gudogging.domain.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationListResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.notification.application.exception.NotificationErrorCode;
import com.subscribers.gudogging.domain.notification.application.exception.NotificationNotFoundException;
import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.entity.NotificationType;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.Platform;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationUseCase")
class NotificationUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @Mock
    private NotificationGetService notificationGetService;

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

    private Subscription catalogSubscription(User user, String name, String logoUrl) {
        ServiceCatalog service = ServiceCatalog.create(name, null, null, Category.VIDEO_OTT, logoUrl, null, 1);
        Subscription subscription = Subscription.createFromCatalog(user, service, Category.VIDEO_OTT, 17000, 1,
            BillingCycleUnit.MONTH, LocalDate.of(2026, 11, 10), PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
        ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
        return subscription;
    }

    private Subscription customSubscription(User user, String name) {
        Subscription subscription = Subscription.createCustom(user, name, Category.ETC, 9900, 1,
            BillingCycleUnit.MONTH, LocalDate.of(2026, 11, 10), PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
        ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
        return subscription;
    }

    private Notification notification(User user, Subscription subscription, NotificationType type, String title,
                                      LocalDateTime sentAt) {
        Notification notification = Notification.create(user, subscription, type, LocalDate.of(2026, 11, 10), title,
            "본문", sentAt);
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        return notification;
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("받은 최신순 그대로 종류 · 문구 · 구독 ID · 로고 · 발송 시각 · 읽음 여부를 반환한다")
        void returnsNotifications() {
            // given
            User user = givenUser();
            Subscription netflix = catalogSubscription(user, "넷플릭스", "https://cdn/netflix.png");
            Notification latest = notification(user, netflix, NotificationType.CHECK_REMINDER,
                "넷플릭스 외 2건, 63,400원 결제 예정", LocalDateTime.of(2026, 11, 16, 9, 0));
            Notification older = notification(user, netflix, NotificationType.BILLING_REMINDER,
                "내일 넷플릭스 17,000원이 결제돼요", LocalDateTime.of(2026, 11, 9, 9, 0));
            older.read();
            given(notificationGetService.findRecentNotifications(eq(user), any(LocalDateTime.class)))
                .willReturn(List.of(latest, older));

            // when
            NotificationListResponse response = notificationUseCase.getNotifications(userId);

            // then
            assertThat(response.notifications()).hasSize(2);
            NotificationResponse first = response.notifications().get(0);
            assertThat(first.notificationId()).isEqualTo(latest.getId());
            assertThat(first.type()).isEqualTo(NotificationType.CHECK_REMINDER);
            assertThat(first.title()).isEqualTo("넷플릭스 외 2건, 63,400원 결제 예정");
            assertThat(first.body()).isEqualTo("본문");
            assertThat(first.subscriptionId()).isEqualTo(netflix.getId());
            assertThat(first.logoUrl()).isEqualTo("https://cdn/netflix.png");
            assertThat(first.sentAt()).isEqualTo(LocalDateTime.of(2026, 11, 16, 9, 0));
            assertThat(first.read()).isFalse();
            assertThat(response.notifications().get(1).read()).isTrue();
        }

        @Test
        @DisplayName("직접 입력 구독의 알림이면 logoUrl이 null이다")
        void returnsNullLogoForCustomSubscription() {
            // given
            User user = givenUser();
            Notification custom = notification(user, customSubscription(user, "헬스장"),
                NotificationType.BILLING_REMINDER, "내일 헬스장 9,900원이 결제돼요", LocalDateTime.of(2026, 11, 9, 9, 0));
            given(notificationGetService.findRecentNotifications(eq(user), any(LocalDateTime.class)))
                .willReturn(List.of(custom));

            // when
            NotificationListResponse response = notificationUseCase.getNotifications(userId);

            // then
            assertThat(response.notifications().get(0).logoUrl()).isNull();
        }

        @Test
        @DisplayName("알림이 없으면 빈 배열을 반환한다")
        void returnsEmptyList() {
            // given
            User user = givenUser();
            given(notificationGetService.findRecentNotifications(eq(user), any(LocalDateTime.class)))
                .willReturn(List.of());

            // when
            NotificationListResponse response = notificationUseCase.getNotifications(userId);

            // then
            assertThat(response.notifications()).isEmpty();
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenNoUser();

            // when & then
            assertThatThrownBy(() -> notificationUseCase.getNotifications(userId))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("readNotification")
    class ReadNotification {

        private final UUID notificationId = UUID.randomUUID();

        @Test
        @DisplayName("읽지 않은 알림을 읽음 처리한다")
        void readsNotification() {
            // given
            User user = givenUser();
            Notification notification = notification(user, catalogSubscription(user, "넷플릭스", null),
                NotificationType.BILLING_REMINDER, "내일 넷플릭스 17,000원이 결제돼요", LocalDateTime.of(2026, 11, 9, 9, 0));
            given(notificationGetService.findNotification(user, notificationId)).willReturn(Optional.of(notification));

            // when
            notificationUseCase.readNotification(userId, notificationId);

            // then
            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("이미 읽은 알림이어도 성공한다")
        void succeedsWhenAlreadyRead() {
            // given
            User user = givenUser();
            Notification notification = notification(user, catalogSubscription(user, "넷플릭스", null),
                NotificationType.BILLING_REMINDER, "내일 넷플릭스 17,000원이 결제돼요", LocalDateTime.of(2026, 11, 9, 9, 0));
            notification.read();
            given(notificationGetService.findNotification(user, notificationId)).willReturn(Optional.of(notification));

            // when
            notificationUseCase.readNotification(userId, notificationId);

            // then
            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("알림이 없거나 다른 회원 것이면 NOTIFICATION_NOT_FOUND 예외를 던진다")
        void throwsWhenNotificationNotFound() {
            // given
            User user = givenUser();
            given(notificationGetService.findNotification(user, notificationId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationUseCase.readNotification(userId, notificationId))
                .isInstanceOf(NotificationNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenNoUser();

            // when & then
            assertThatThrownBy(() -> notificationUseCase.readNotification(userId, notificationId))
                .isInstanceOf(UserNotFoundException.class);
        }
    }
}
