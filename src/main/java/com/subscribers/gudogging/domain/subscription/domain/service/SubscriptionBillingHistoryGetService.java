package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionBillingHistoryRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingHistoryGetService {

    private final SubscriptionBillingHistoryRepository subscriptionBillingHistoryRepository;

    public List<SubscriptionBillingHistory> findBillingHistoriesBySubscriptions(
        Collection<Subscription> subscriptions) {
        return subscriptionBillingHistoryRepository.findAllBySubscriptionInAndDeletedFalse(subscriptions);
    }
}
