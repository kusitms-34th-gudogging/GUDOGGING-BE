package com.subscribers.gudogging.domain.notification.application.usecase;

import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationListResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.notification.application.exception.NotificationNotFoundException;
import com.subscribers.gudogging.domain.notification.domain.NotificationContent;
import com.subscribers.gudogging.domain.notification.domain.PushMessage;
import com.subscribers.gudogging.domain.notification.domain.PushSendResult;
import com.subscribers.gudogging.domain.notification.domain.PushSender;
import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.entity.NotificationType;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationCreateService;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationUseCase {

    private static final int CHECK_REMINDER_DAYS_BEFORE = 3;
    private static final int BUNDLE_DAYS = 7;

    private final UserGetService userGetService;
    private final NotificationGetService notificationGetService;
    private final NotificationCreateService notificationCreateService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionCheckGetService subscriptionCheckGetService;
    private final PushSender pushSender;

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

    /**
     * 매일 09:00 배치 — D-3 점검 알림(회원별 1건 묶음)과 D-1 결제 알림 · 해지 확인(구독마다 1건)을 저장하고 발송한다. 푸시 토큰이 있는 회원의 구독 중 구독만 대상.
     */
    @Transactional
    public void sendScheduledNotifications(LocalDate today) {
        List<Subscription> subscriptions = subscriptionGetService.findActiveSubscriptionsOfPushUsers();
        if (subscriptions.isEmpty()) {
            return;
        }
        Map<UUID, SubscriptionCheck> latestChecks = findLatestChecks(subscriptions);
        Set<SentKey> sentKeys = notificationGetService.findNotificationsFrom(subscriptions, today).stream()
            .map(SentKey::of)
            .collect(Collectors.toSet());
        LocalDateTime sentAt = LocalDateTime.now();

        List<Notification> notifications = new ArrayList<>();
        subscriptions.stream()
            .collect(Collectors.groupingBy(Subscription::getUser, LinkedHashMap::new, Collectors.toList()))
            .forEach((user, userSubscriptions) -> checkReminderOf(user, userSubscriptions, latestChecks, sentKeys,
                today, sentAt).ifPresent(notifications::add));
        subscriptions.stream()
            .map(subscription -> billingReminderOf(subscription, latestChecks.get(subscription.getId()), sentKeys,
                today, sentAt))
            .flatMap(Optional::stream)
            .forEach(notifications::add);
        if (notifications.isEmpty()) {
            return;
        }
        notificationCreateService.createNotifications(notifications).forEach(this::send);
    }

    private User findUser(UUID userId) {
        return userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    /**
     * 구독별 가장 최근 점검 (created_at 기준, 홈과 같은 방식).
     */
    private Map<UUID, SubscriptionCheck> findLatestChecks(List<Subscription> subscriptions) {
        BinaryOperator<SubscriptionCheck> later = BinaryOperator.maxBy(
            Comparator.comparing(SubscriptionCheck::getCreatedAt, Comparator.nullsFirst(LocalDateTime::compareTo)));
        return subscriptionCheckGetService.findChecksBySubscriptions(subscriptions).stream()
            .collect(Collectors.toMap(check -> check.getSubscription().getId(), Function.identity(), later));
    }

    /**
     * D-3 점검 알림 — 알림 설정 ON 회원의 '꾸준히 쓰는 구독 아님 · 이번 주기 점검 안 함' 구독 중 결제 3일 전 구독이 있으면, 내일 ~ 7일 뒤 결제될 대상 구독을 묶어
     * 1건. 대표 구독(금액이 가장 큰 구독) · 결제일 알림이 이미 있으면 보내지 않는다 (앞 묶음에 이미 들어간 구독).
     */
    private Optional<Notification> checkReminderOf(User user, List<Subscription> userSubscriptions,
                                                   Map<UUID, SubscriptionCheck> latestChecks, Set<SentKey> sentKeys,
                                                   LocalDate today, LocalDateTime sentAt) {
        if (!user.isCheckNotificationEnabled()) {
            return Optional.empty();
        }
        LocalDate from = today.plusDays(1);
        LocalDate to = today.plusDays(BUNDLE_DAYS);
        List<Subscription> targets = userSubscriptions.stream()
            .filter(subscription -> !subscription.isSteadyUse())
            .filter(subscription -> !isCheckedThisCycle(subscription, latestChecks.get(subscription.getId()), today))
            .toList();
        boolean hasDueSubscription = targets.stream()
            .anyMatch(subscription -> subscription.nextBillingOn(today)
                .equals(today.plusDays(CHECK_REMINDER_DAYS_BEFORE)));
        if (!hasDueSubscription) {
            return Optional.empty();
        }
        List<Subscription> bundle = targets.stream()
            .filter(subscription -> subscription.billingCycle().countBetween(from, to) > 0)
            .sorted(Comparator.comparingInt(Subscription::getAmount).reversed()
                .thenComparing(subscription -> subscription.nextBillingOn(from))
                .thenComparing(Subscription::getName))
            .toList();
        Subscription main = bundle.get(0);
        LocalDate mainBillingOn = main.nextBillingOn(from);
        if (sentKeys.contains(new SentKey(main.getId(), NotificationType.CHECK_REMINDER, mainBillingOn))) {
            return Optional.empty();
        }
        long totalAmount = bundle.stream()
            .mapToLong(subscription -> subscription.billingCycle().countBetween(from, to) * subscription.getAmount())
            .sum();
        NotificationContent content = NotificationContent.checkReminder(main.getName(), bundle.size() - 1,
            totalAmount);
        return Optional.of(Notification.create(user, main, NotificationType.CHECK_REMINDER, mainBillingOn,
            content.title(), content.body(), sentAt));
    }

    /**
     * D-1 — 내일 결제되는 모든 구독(꾸준히 쓰는 구독 포함, 알림 설정과 무관). 이번 주기 점검에서 해지를 골랐으면 해지 확인. 같은 구독 · 결제일 D-1 알림이 이미 있으면
     * 보내지 않는다.
     */
    private Optional<Notification> billingReminderOf(Subscription subscription, SubscriptionCheck latestCheck,
                                                     Set<SentKey> sentKeys, LocalDate today, LocalDateTime sentAt) {
        LocalDate tomorrow = today.plusDays(1);
        if (!subscription.nextBillingOn(today).equals(tomorrow)) {
            return Optional.empty();
        }
        if (sentKeys.contains(new SentKey(subscription.getId(), NotificationType.BILLING_REMINDER, tomorrow))
            || sentKeys.contains(new SentKey(subscription.getId(), NotificationType.CANCEL_CONFIRM, tomorrow))) {
            return Optional.empty();
        }
        boolean canceledInCheck = isCheckedThisCycle(subscription, latestCheck, today)
            && latestCheck.getDecision() == CheckDecision.CANCEL;
        NotificationType type = canceledInCheck ? NotificationType.CANCEL_CONFIRM : NotificationType.BILLING_REMINDER;
        NotificationContent content = canceledInCheck
            ? NotificationContent.cancelConfirm(subscription.getName(), subscription.getAmount())
            : NotificationContent.billingReminder(subscription.getName(), subscription.getAmount());
        return Optional.of(Notification.create(subscription.getUser(), subscription, type, tomorrow, content.title(),
            content.body(), sentAt));
    }

    /**
     * 이번 결제 주기 점검 완료 = 가장 최근 점검의 점검 대상 결제일이 지금의 다음 결제일 (홈과 같은 기준).
     */
    private boolean isCheckedThisCycle(Subscription subscription, SubscriptionCheck latestCheck, LocalDate today) {
        return latestCheck != null && latestCheck.getTargetBillingOn().equals(subscription.nextBillingOn(today));
    }

    /**
     * 한 건 발송이 실패해도 나머지는 계속 보낸다. 저장한 알림은 그대로 둔다 (알림 목록에는 보임). FCM이 없는 토큰이라고 하면 회원 토큰 · 기기 종류를 비우고, 같은 회원의 다음 알림은 보내지 않는다.
     */
    private void send(Notification notification) {
        if (notification.getUser().getPushToken() == null) {
            return;
        }
        PushMessage message = new PushMessage(
            notification.getUser().getPushToken(),
            notification.getTitle(),
            notification.getBody(),
            Map.of(
                "type", notification.getType().name(),
                "subscriptionId", notification.getSubscription().getId().toString(),
                "notificationId", notification.getId().toString()));
        try {
            if (pushSender.send(message) == PushSendResult.INVALID_TOKEN) {
                notification.getUser().clearPushToken();
            }
        } catch (RuntimeException e) {
            log.warn("[푸시 발송 실패] notificationId={}", notification.getId(), e);
        }
    }

    /**
     * 중복 발송 확인 키 — UNIQUE (subscription_id, type, target_billing_on).
     */
    private record SentKey(UUID subscriptionId, NotificationType type, LocalDate targetBillingOn) {

        static SentKey of(Notification notification) {
            return new SentKey(notification.getSubscription().getId(), notification.getType(),
                notification.getTargetBillingOn());
        }
    }
}
