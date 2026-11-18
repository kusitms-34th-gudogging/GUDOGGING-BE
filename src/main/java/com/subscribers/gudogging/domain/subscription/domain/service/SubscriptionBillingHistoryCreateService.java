package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionBillingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingHistoryCreateService {

    private final SubscriptionBillingHistoryRepository subscriptionBillingHistoryRepository;

    public SubscriptionBillingHistory createBillingHistory(SubscriptionBillingHistory billingHistory) {
        return subscriptionBillingHistoryRepository.save(billingHistory);
    }
}
