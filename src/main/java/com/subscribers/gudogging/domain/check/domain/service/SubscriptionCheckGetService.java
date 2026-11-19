package com.subscribers.gudogging.domain.check.domain.service;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.repository.SubscriptionCheckRepository;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionCheckGetService {

    private final SubscriptionCheckRepository subscriptionCheckRepository;

    public List<SubscriptionCheck> findChecksBySubscriptions(Collection<Subscription> subscriptions) {
        return subscriptionCheckRepository.findAllBySubscriptionInAndDeletedFalse(subscriptions);
    }

    /**
     * 구독의 가장 최근 점검 (삭제 제외).
     */
    public Optional<SubscriptionCheck> findLatestCheck(Subscription subscription) {
        return subscriptionCheckRepository.findFirstBySubscriptionAndDeletedFalseOrderByCreatedAtDesc(subscription);
    }
}
