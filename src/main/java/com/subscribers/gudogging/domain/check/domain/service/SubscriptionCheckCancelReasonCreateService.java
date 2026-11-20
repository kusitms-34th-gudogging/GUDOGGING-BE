package com.subscribers.gudogging.domain.check.domain.service;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import com.subscribers.gudogging.domain.check.domain.repository.SubscriptionCheckCancelReasonRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionCheckCancelReasonCreateService {

    private final SubscriptionCheckCancelReasonRepository subscriptionCheckCancelReasonRepository;

    public List<SubscriptionCheckCancelReason> createCancelReasons(List<SubscriptionCheckCancelReason> cancelReasons) {
        return subscriptionCheckCancelReasonRepository.saveAll(cancelReasons);
    }
}
