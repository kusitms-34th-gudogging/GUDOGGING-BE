package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionRepository;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionGetService {

    private final SubscriptionRepository subscriptionRepository;

    public Set<UUID> findActiveServiceIds(User user) {
        return Set.copyOf(subscriptionRepository.findActiveServiceIdsByUser(user));
    }

    public List<Subscription> findSubscriptions(User user) {
        return subscriptionRepository.findAllWithServiceByUser(user);
    }
}
