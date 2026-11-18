package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateSubscriptionException;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionCreateService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 한 번에 저장하고 바로 반영(flush)한다. 동시 등록으로 같은 서비스의 ACTIVE 구독 고유 제약에 걸리면 DuplicateSubscriptionException을 던진다.
     */
    public List<Subscription> createSubscriptions(List<Subscription> subscriptions) {
        try {
            return subscriptionRepository.saveAllAndFlush(subscriptions);
        } catch (DataIntegrityViolationException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase(Locale.ROOT)
                    .contains(Subscription.ACTIVE_SERVICE_UNIQUE_CONSTRAINT)) {
                    throw new DuplicateSubscriptionException();
                }
            }
            throw e;
        }
    }
}
