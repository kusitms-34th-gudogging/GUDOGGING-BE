package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionCreateService {

    private final SubscriptionRepository subscriptionRepository;

    public List<Subscription> createSubscriptions(List<Subscription> subscriptions) {
        return subscriptionRepository.saveAll(subscriptions);
    }
}
