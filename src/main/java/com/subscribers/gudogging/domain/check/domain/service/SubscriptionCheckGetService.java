package com.subscribers.gudogging.domain.check.domain.service;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.repository.SubscriptionCheckRepository;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    /**
     * 구독의 점검 중 from 이후(포함)에 한 점검 (삭제 제외), 최신순.
     */
    public List<SubscriptionCheck> findChecksCreatedSince(Subscription subscription, LocalDateTime from) {
        return subscriptionCheckRepository
            .findAllBySubscriptionAndCreatedAtGreaterThanEqualAndDeletedFalseOrderByCreatedAtDesc(subscription, from);
    }

    /**
     * 회원 본인 구독의 점검 (점검 · 구독 모두 삭제 제외). 구독 · 서비스를 함께 조회한다.
     */
    public Optional<SubscriptionCheck> findCheck(User user, UUID checkupId) {
        return subscriptionCheckRepository.findWithSubscriptionByIdAndUser(checkupId, user);
    }
}
