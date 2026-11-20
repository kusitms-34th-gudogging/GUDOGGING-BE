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

    /**
     * 점검의 해지 이유 전부 (Soft Delete된 행 포함). UNIQUE(subscription_check_id, reason)가 삭제된 행까지 검사하므로 재결정 시 되살릴 행을 찾는 데 쓴다.
     */
    public List<SubscriptionCheckCancelReason> findAllCancelReasonsIncludingDeleted(SubscriptionCheck subscriptionCheck) {
        return subscriptionCheckCancelReasonRepository.findAllBySubscriptionCheck(subscriptionCheck);
    }
}
