package com.subscribers.gudogging.domain.check.domain.service;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import com.subscribers.gudogging.domain.check.domain.repository.SubscriptionCheckCancelReasonRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionCheckCancelReasonGetService {

    private final SubscriptionCheckCancelReasonRepository subscriptionCheckCancelReasonRepository;

    public List<SubscriptionCheckCancelReason> findCancelReasonsByChecks(
        Collection<SubscriptionCheck> subscriptionChecks
    ) {
        return subscriptionCheckCancelReasonRepository.findAllBySubscriptionCheckInAndDeletedFalse(subscriptionChecks);
    }
}
