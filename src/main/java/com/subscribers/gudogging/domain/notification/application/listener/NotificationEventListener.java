package com.subscribers.gudogging.domain.notification.application.listener;

import com.subscribers.gudogging.domain.notification.application.usecase.NotificationUseCase;
import com.subscribers.gudogging.domain.subscription.domain.event.SubscriptionBillingChangedEvent;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 구독 등록 · 수정 · 재등록이 커밋된 뒤 알림을 즉시 발송한다 (NOTIFICATION-008). 구독 쪽은 이미 커밋됐으므로 실패해도 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationUseCase notificationUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionBillingChanged(SubscriptionBillingChangedEvent event) {
        try {
            notificationUseCase.sendInstantNotifications(event, LocalDate.now());
        } catch (RuntimeException e) {
            log.warn("[알림 즉시 발송 실패] userId={}, subscriptionIds={}", event.userId(), event.subscriptionIds(), e);
        }
    }
}
