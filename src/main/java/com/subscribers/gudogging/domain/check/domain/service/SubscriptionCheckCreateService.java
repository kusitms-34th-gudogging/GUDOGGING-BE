package com.subscribers.gudogging.domain.check.domain.service;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.repository.SubscriptionCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionCheckCreateService {

    private final SubscriptionCheckRepository subscriptionCheckRepository;

    public SubscriptionCheck createCheck(SubscriptionCheck subscriptionCheck) {
        return subscriptionCheckRepository.save(subscriptionCheck);
    }
}
