package com.subscribers.gudogging.domain.notification.application.scheduler;

import com.subscribers.gudogging.domain.notification.application.usecase.NotificationUseCase;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 09:00(한국 시간) D-3 점검 알림 · D-1 결제 알림 · 해지 확인 발송.
 */
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationUseCase notificationUseCase;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendScheduledNotifications() {
        notificationUseCase.sendScheduledNotifications(LocalDate.now());
    }
}
