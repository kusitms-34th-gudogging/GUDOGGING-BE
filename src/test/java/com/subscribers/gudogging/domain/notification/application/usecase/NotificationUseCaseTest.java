package com.subscribers.gudogging.domain.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationListResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.notification.application.exception.NotificationErrorCode;
import com.subscribers.gudogging.domain.notification.application.exception.NotificationNotFoundException;
import com.subscribers.gudogging.domain.notification.domain.PushMessage;
import com.subscribers.gudogging.domain.notification.domain.PushSendResult;
import com.subscribers.gudogging.domain.notification.domain.PushSender;
import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.entity.NotificationType;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationCreateService;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.event.SubscriptionBillingChangedEvent;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private NotificationCreateService notificationCreateService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private SubscriptionCheckGetService subscriptionCheckGetService;

    @Mock
    private PushSender pushSender;

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
            user.changeCheckNotification(true);

            // when
            NotificationSettingResponse response = notificationUseCase.getNotificationSetting(userId);

            // then
            assertThat(response.paymentReminderEnabled()).isTrue();
        }

        @Test
        @DisplayName("새로 가입한 회원은 false를 반환한다")
        void returnsFalseByDefault() {
            // given
            givenUser();

            // when
            NotificationSettingResponse response = notificationUseCase.getNotificationSetting(userId);

            // then
            assertThat(response.paymentReminderEnabled()).isFalse();
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
            user.changeCheckNotification(true);

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

    // 11/4(수) 09:00 배치 — D-3 = 11/7, 내일 ~ 7일 뒤 = 11/5 ~ 11/11, D-1 = 11/5
    private final LocalDate today = LocalDate.of(2026, 11, 4);

    private User pushUser(boolean checkNotificationEnabled) {
        User user = User.create(SocialProvider.GOOGLE, UUID.randomUUID().toString(), null, null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.updatePushToken("fcm-token-" + user.getId(), Platform.IOS);
        user.changeCheckNotification(checkNotificationEnabled);
        return user;
    }

    private Subscription subscription(User user, String name, int amount, LocalDate billingOn) {
        return subscription(user, name, amount, 1, BillingCycleUnit.MONTH, billingOn);
    }

    private Subscription subscription(User user, String name, int amount, int cycleValue,
                                      BillingCycleUnit cycleUnit, LocalDate billingAnchorOn) {
        ServiceCatalog service = ServiceCatalog.create(name, null, null, Category.VIDEO_OTT, null, null, 1);
        Subscription subscription = Subscription.createFromCatalog(user, service, Category.VIDEO_OTT, amount,
            cycleValue, cycleUnit, billingAnchorOn, PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
        ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
        return subscription;
    }

    private SubscriptionCheck check(Subscription subscription, LocalDate targetBillingOn) {
        SubscriptionCheck check = SubscriptionCheck.create(subscription, targetBillingOn,
            subscription.getCategory(), 1, 1, 1, 30, CheckResultStage.of(30), null);
        ReflectionTestUtils.setField(check, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(check, "createdAt", today.atStartOfDay().minusDays(1));
        return check;
    }

    private void givenSaved() {
        given(notificationCreateService.createNotifications(anyList()))
            .willAnswer(invocation -> {
                List<Notification> notifications = invocation.getArgument(0);
                notifications.forEach(n -> ReflectionTestUtils.setField(n, "id", UUID.randomUUID()));
                return notifications;
            });
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedNotifications() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationCreateService).createNotifications(captor.capture());
        return captor.getValue();
    }

    private Notification sent(Subscription subscription, NotificationType type, LocalDate targetBillingOn) {
        return Notification.create(subscription.getUser(), subscription, type, targetBillingOn, "제목", "본문",
            today.minusDays(1).atTime(9, 0));
    }

    @Nested
    @DisplayName("sendScheduledNotifications")
    class SendScheduledNotifications {

        private void givenBatch(List<Subscription> subscriptions, List<SubscriptionCheck> checks,
                                List<Notification> sentNotifications) {
            given(subscriptionGetService.findActiveSubscriptionsOfPushUsers())
                .willReturn(subscriptions);
            given(subscriptionCheckGetService.findChecksBySubscriptions(subscriptions))
                .willReturn(checks);
            given(notificationGetService.findNotificationsFrom(subscriptions, today))
                .willReturn(sentNotifications);
        }

        @Test
        @DisplayName("D-3: 결제 3일 전 구독이 있으면 7일 안 결제 구독을 묶어 금액이 가장 큰 구독 기준으로 1건 보낸다")
        void sendsBundledCheckReminder() {
            // given
            User user = pushUser(true);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 8));
            Subscription netflix = subscription(user, "넷플릭스", 17000, LocalDate.of(2026, 10, 10));
            Subscription youtube = subscription(user, "유튜브", 14900, LocalDate.of(2026, 10, 20));
            givenBatch(List.of(wavve, tving, netflix, youtube), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            List<Notification> saved = savedNotifications();
            assertThat(saved).hasSize(1);
            Notification notification = saved.get(0);
            assertThat(notification.getType()).isEqualTo(NotificationType.CHECK_REMINDER);
            assertThat(notification.getSubscription()).isEqualTo(netflix);
            assertThat(notification.getTargetBillingOn()).isEqualTo(LocalDate.of(2026, 11, 10));
            assertThat(notification.getTitle()).isEqualTo("넷플릭스 외 2건, 34,400원 결제 예정");
            assertThat(notification.getBody()).isEqualTo("7일 안에 빠져나갈 구독료를 확인하세요.");
        }

        @Test
        @DisplayName("D-3: 묶을 구독이 1건이면 '{서비스명} {금액}원 결제 예정'으로 보낸다")
        void sendsSingleCheckReminder() {
            // given
            User user = pushUser(true);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            givenBatch(List.of(wavve), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(savedNotifications()).singleElement()
                .extracting(Notification::getTitle)
                .isEqualTo("웨이브 7,900원 결제 예정");
        }

        @Test
        @DisplayName("D-3: 결제 3일 전 구독이 없으면 7일 안 결제 구독이 있어도 보내지 않는다")
        void skipsWhenNoDueSubscription() {
            // given
            User user = pushUser(true);
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 8));
            givenBatch(List.of(tving), List.of(), List.of());

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
            verify(pushSender, never()).send(any());
        }

        @Test
        @DisplayName("D-3: 꾸준히 쓰는 구독과 이번 주기에 점검한 구독은 대상에서 뺀다")
        void excludesSteadyAndCheckedSubscriptions() {
            // given
            User user = pushUser(true);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            Subscription steady = subscription(user, "멜론", 10900, LocalDate.of(2026, 10, 7));
            steady.changeSteadyUse(true);
            Subscription checked = subscription(user, "넷플릭스", 17000, LocalDate.of(2026, 10, 10));
            givenBatch(List.of(wavve, steady, checked), List.of(check(checked, LocalDate.of(2026, 11, 10))), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(savedNotifications()).singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getSubscription()).isEqualTo(wavve);
                    assertThat(notification.getTitle()).isEqualTo("웨이브 7,900원 결제 예정");
                });
        }

        @Test
        @DisplayName("D-3: 알림 설정이 OFF인 회원에게는 D-3을 보내지 않는다")
        void skipsCheckReminderWhenSettingOff() {
            // given
            User user = pushUser(false);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            givenBatch(List.of(wavve), List.of(), List.of());

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
        }

        @Test
        @DisplayName("D-3: 7일 안에 여러 번 결제되는 구독은 횟수만큼 합계에 더한다")
        void sumsRepeatedBillingsInWindow() {
            // given — 매일 1,000원 → 11/5 ~ 11/11 7번
            User user = pushUser(true);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            Subscription daily = subscription(user, "데일리", 1000, 1, BillingCycleUnit.DAY, LocalDate.of(2026, 11, 1));
            givenBatch(List.of(wavve, daily), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then — 매일 결제라 D-1 알림도 함께 저장
            assertThat(savedNotifications())
                .filteredOn(notification -> notification.getType() == NotificationType.CHECK_REMINDER)
                .singleElement()
                .extracting(Notification::getTitle)
                .isEqualTo("웨이브 외 1건, 14,900원 결제 예정");
        }

        @Test
        @DisplayName("D-3: 같은 대표 구독 · 결제일 알림이 이미 있으면 그 회원은 보내지 않는다")
        void skipsWhenSameMainAlreadySent() {
            // given — 어제(11/3)는 웨이브(11/6) 때문에 '넷플릭스 · 11/10'으로 보냄, 오늘은 티빙(11/7)이 D-3
            User user = pushUser(true);
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 7));
            Subscription netflix = subscription(user, "넷플릭스", 17000, LocalDate.of(2026, 10, 10));
            givenBatch(List.of(tving, netflix), List.of(),
                List.of(sent(netflix, NotificationType.CHECK_REMINDER, LocalDate.of(2026, 11, 10))));

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
        }

        @Test
        @DisplayName("D-1: 내일 결제되는 구독마다 결제 알림을 보낸다 (꾸준히 쓰는 구독 · 알림 설정 OFF 회원 포함)")
        void sendsBillingReminder() {
            // given
            User user = pushUser(false);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            netflix.changeSteadyUse(true);
            givenBatch(List.of(netflix), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(savedNotifications()).singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getType()).isEqualTo(NotificationType.BILLING_REMINDER);
                    assertThat(notification.getTargetBillingOn()).isEqualTo(LocalDate.of(2026, 11, 5));
                    assertThat(notification.getTitle()).isEqualTo("내일 넷플릭스 13,500원이 결제돼요");
                    assertThat(notification.getBody()).isEqualTo("해지 안 했다면 오늘이 마지막 해지 기회예요.");
                });
        }

        @Test
        @DisplayName("D-1: 매일 결제 구독도 내일 결제되므로 결제 알림을 보낸다")
        void sendsBillingReminderForDailySubscription() {
            // given — 오늘(11/4)도 결제일인 매일 결제 구독
            User user = pushUser(false);
            Subscription daily = subscription(user, "데일리", 1000, 1, BillingCycleUnit.DAY, LocalDate.of(2026, 11, 1));
            givenBatch(List.of(daily), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(savedNotifications()).singleElement().satisfies(notification -> {
                assertThat(notification.getType()).isEqualTo(NotificationType.BILLING_REMINDER);
                assertThat(notification.getTargetBillingOn()).isEqualTo(LocalDate.of(2026, 11, 5));
                assertThat(notification.getTitle()).isEqualTo("내일 데일리 1,000원이 결제돼요");
            });
        }

        @Test
        @DisplayName("D-1: 이번 주기 점검에서 해지를 골랐으면 해지 확인으로 보낸다")
        void sendsCancelConfirm() {
            // given
            User user = pushUser(true);
            Subscription tving = subscription(user, "티빙", 13500, LocalDate.of(2026, 10, 5));
            SubscriptionCheck check = check(tving, LocalDate.of(2026, 11, 5));
            check.cancel(null, today.atStartOfDay());
            givenBatch(List.of(tving), List.of(check), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(savedNotifications()).singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getType()).isEqualTo(NotificationType.CANCEL_CONFIRM);
                    assertThat(notification.getTitle()).isEqualTo("티빙, 해지하셨나요?");
                    assertThat(notification.getBody()).isEqualTo("아직이라면 내일 13,500원이 결제돼요. 오늘 해지하세요.");
                });
        }

        @Test
        @DisplayName("D-1: 같은 구독 · 결제일 D-1 알림이 이미 있으면 보내지 않는다")
        void skipsWhenBillingReminderAlreadySent() {
            // given
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            givenBatch(List.of(netflix), List.of(),
                List.of(sent(netflix, NotificationType.BILLING_REMINDER, LocalDate.of(2026, 11, 5))));

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
        }

        @Test
        @DisplayName("저장한 알림마다 type · subscriptionId · notificationId를 담아 발송한다")
        void sendsPushWithPayload() {
            // given
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            givenBatch(List.of(netflix), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            Notification notification = savedNotifications().get(0);
            ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
            verify(pushSender).send(captor.capture());
            PushMessage message = captor.getValue();
            assertThat(message.token()).isEqualTo(user.getPushToken());
            assertThat(message.title()).isEqualTo("내일 넷플릭스 13,500원이 결제돼요");
            assertThat(message.data()).isEqualTo(Map.of(
                "type", "BILLING_REMINDER",
                "subscriptionId", netflix.getId().toString(),
                "notificationId", notification.getId().toString()));
        }

        @Test
        @DisplayName("한 건 발송이 실패해도 나머지는 발송한다")
        void continuesWhenOneSendFails() {
            // given
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 5));
            givenBatch(List.of(netflix, tving), List.of(), List.of());
            givenSaved();
            given(pushSender.send(any())).willThrow(new IllegalStateException("FCM 오류"))
                .willReturn(PushSendResult.SENT);

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            verify(pushSender, times(2)).send(any());
        }

        @Test
        @DisplayName("FCM이 없는 토큰이라고 응답하면 그 회원의 푸시 토큰과 기기 종류를 비우고 다음 알림은 보내지 않는다")
        void clearsTokenWhenInvalid() {
            // given — 같은 회원의 D-1 알림 2건
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 5));
            givenBatch(List.of(netflix, tving), List.of(), List.of());
            givenSaved();
            given(pushSender.send(any())).willReturn(PushSendResult.INVALID_TOKEN);

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(user.getPushToken()).isNull();
            assertThat(user.getPlatform()).isNull();
            verify(pushSender, times(1)).send(any());
            assertThat(savedNotifications()).hasSize(2);
        }

        @Test
        @DisplayName("정상 발송이면 푸시 토큰을 그대로 둔다")
        void keepsTokenWhenSent() {
            // given
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            givenBatch(List.of(netflix), List.of(), List.of());
            givenSaved();
            given(pushSender.send(any())).willReturn(PushSendResult.SENT);

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            assertThat(user.getPushToken()).isNotNull();
            assertThat(user.getPlatform()).isEqualTo(Platform.IOS);
        }

        @Test
        @DisplayName("푸시 받을 구독이 없으면 저장 · 발송하지 않는다")
        void doesNothingWithoutSubscriptions() {
            // given
            given(subscriptionGetService.findActiveSubscriptionsOfPushUsers())
                .willReturn(List.of());

            // when
            notificationUseCase.sendScheduledNotifications(today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
            verify(pushSender, never()).send(any());
        }
    }

    @Nested
    @DisplayName("sendInstantNotifications")
    class SendInstantNotifications {

        private void givenUserSubscriptions(User user, List<Subscription> subscriptions,
                                            List<SubscriptionCheck> checks, List<Notification> sentNotifications) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findActiveSubscriptionsOfPushUser(user)).willReturn(subscriptions);
            given(subscriptionCheckGetService.findChecksBySubscriptions(subscriptions)).willReturn(checks);
            given(notificationGetService.findNotificationsFrom(subscriptions, today)).willReturn(sentNotifications);
        }

        private SubscriptionBillingChangedEvent changed(Subscription... subscriptions) {
            return SubscriptionBillingChangedEvent.of(userId, List.of(subscriptions));
        }

        @Test
        @DisplayName("D-3: 등록한 구독이 결제 3일 전이면 회원의 7일 안 결제 구독을 묶어 1건 보낸다")
        void sendsBundledCheckReminderWhenChangedSubscriptionIsDue() {
            // given — 새로 등록한 웨이브가 11/7(3일 뒤), 기존 넷플릭스가 11/10
            User user = pushUser(true);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            Subscription netflix = subscription(user, "넷플릭스", 17000, LocalDate.of(2026, 10, 10));
            givenUserSubscriptions(user, List.of(wavve, netflix), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendInstantNotifications(changed(wavve), today);

            // then
            assertThat(savedNotifications()).singleElement().satisfies(notification -> {
                assertThat(notification.getType()).isEqualTo(NotificationType.CHECK_REMINDER);
                assertThat(notification.getSubscription()).isEqualTo(netflix);
                assertThat(notification.getTargetBillingOn()).isEqualTo(LocalDate.of(2026, 11, 10));
                assertThat(notification.getTitle()).isEqualTo("넷플릭스 외 1건, 24,900원 결제 예정");
            });
            verify(pushSender).send(any());
        }

        @Test
        @DisplayName("D-3: 등록한 구독이 결제 3일 전이 아니면 다른 구독이 3일 전이어도 보내지 않는다")
        void skipsCheckReminderWhenChangedSubscriptionIsNotDue() {
            // given — 결제 3일 전인 웨이브는 이번에 바뀐 구독이 아님
            User user = pushUser(true);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 8));
            givenUserSubscriptions(user, List.of(wavve, tving), List.of(), List.of());

            // when
            notificationUseCase.sendInstantNotifications(changed(tving), today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
            verify(pushSender, never()).send(any());
        }

        @Test
        @DisplayName("D-3: 알림 설정이 OFF면 등록한 구독이 결제 3일 전이어도 보내지 않는다")
        void skipsCheckReminderWhenSettingOff() {
            // given
            User user = pushUser(false);
            Subscription wavve = subscription(user, "웨이브", 7900, LocalDate.of(2026, 10, 7));
            givenUserSubscriptions(user, List.of(wavve), List.of(), List.of());

            // when
            notificationUseCase.sendInstantNotifications(changed(wavve), today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
        }

        @Test
        @DisplayName("D-1: 수정한 구독이 내일 결제면 점검하지 않았어도 결제 알림을 보내고, 바뀌지 않은 구독은 보내지 않는다")
        void sendsBillingReminderOnlyForChangedSubscription() {
            // given — 넷플릭스 · 티빙 모두 내일(11/5) 결제, 이번에 바뀐 구독은 넷플릭스
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            Subscription tving = subscription(user, "티빙", 9500, LocalDate.of(2026, 10, 5));
            givenUserSubscriptions(user, List.of(netflix, tving), List.of(), List.of());
            givenSaved();

            // when
            notificationUseCase.sendInstantNotifications(changed(netflix), today);

            // then
            assertThat(savedNotifications()).singleElement().satisfies(notification -> {
                assertThat(notification.getType()).isEqualTo(NotificationType.BILLING_REMINDER);
                assertThat(notification.getSubscription()).isEqualTo(netflix);
                assertThat(notification.getTargetBillingOn()).isEqualTo(LocalDate.of(2026, 11, 5));
                assertThat(notification.getTitle()).isEqualTo("내일 넷플릭스 13,500원이 결제돼요");
            });
        }

        @Test
        @DisplayName("D-1: 이번 주기 점검에서 해지를 고른 구독이 내일 결제면 해지 확인을 보낸다")
        void sendsCancelConfirm() {
            // given
            User user = pushUser(true);
            Subscription tving = subscription(user, "티빙", 13500, LocalDate.of(2026, 10, 5));
            SubscriptionCheck check = check(tving, LocalDate.of(2026, 11, 5));
            check.cancel(null, today.atStartOfDay());
            givenUserSubscriptions(user, List.of(tving), List.of(check), List.of());
            givenSaved();

            // when
            notificationUseCase.sendInstantNotifications(changed(tving), today);

            // then
            assertThat(savedNotifications()).singleElement()
                .extracting(Notification::getType)
                .isEqualTo(NotificationType.CANCEL_CONFIRM);
        }

        @Test
        @DisplayName("같은 구독 · 결제일 알림이 이미 있으면 보내지 않는다")
        void skipsWhenAlreadySent() {
            // given
            User user = pushUser(true);
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            givenUserSubscriptions(user, List.of(netflix), List.of(),
                List.of(sent(netflix, NotificationType.BILLING_REMINDER, LocalDate.of(2026, 11, 5))));

            // when
            notificationUseCase.sendInstantNotifications(changed(netflix), today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
        }

        @Test
        @DisplayName("푸시 토큰이 없는 회원이면 저장 · 발송하지 않는다")
        void doesNothingWithoutPushToken() {
            // given — 푸시 토큰이 없으면 푸시 대상 구독 조회가 빈 목록
            User user = givenUser();
            Subscription netflix = subscription(user, "넷플릭스", 13500, LocalDate.of(2026, 10, 5));
            given(subscriptionGetService.findActiveSubscriptionsOfPushUser(user)).willReturn(List.of());

            // when
            notificationUseCase.sendInstantNotifications(changed(netflix), today);

            // then
            verify(notificationCreateService, never()).createNotifications(anyList());
            verify(pushSender, never()).send(any());
        }
    }
}
