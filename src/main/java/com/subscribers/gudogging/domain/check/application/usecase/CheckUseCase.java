package com.subscribers.gudogging.domain.check.application.usecase;

import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.exception.CheckSubscriptionNotActiveException;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotFoundException;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckUseCase {

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionCheckGetService subscriptionCheckGetService;

    /**
     * 구독 카테고리에 맞는 점검 문항 3개와 지난 메모를 조회한다 (K-11~K-70). 구독 종료 구독은 점검할 수 없고, 꾸준히 쓰는 구독은 직접 점검할 수 있다.
     */
    public CheckupFormResponse getCheckupForm(UUID userId, UUID subscriptionId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new CheckSubscriptionNotActiveException();
        }

        SubscriptionCheck latestCheck = subscriptionCheckGetService.findLatestCheck(subscription).orElse(null);
        return CheckupFormResponse.of(subscription, latestCheck, LocalDate.now());
    }
}
