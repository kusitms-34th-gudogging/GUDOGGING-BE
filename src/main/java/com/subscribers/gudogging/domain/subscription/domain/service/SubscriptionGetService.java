package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionRepository;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    public List<Subscription> findSubscriptionsByIds(User user, Collection<UUID> subscriptionIds) {
        return subscriptionRepository.findAllByIdInAndUserAndDeletedFalse(subscriptionIds, user);
    }

    public Optional<Subscription> findSubscription(User user, UUID subscriptionId) {
        return subscriptionRepository.findWithServiceByIdAndUser(subscriptionId, user);
    }

    /**
     * 푸시 토큰이 있는 회원(탈퇴 제외)의 구독 중 구독 — 푸시 발송 배치 대상. 회원 · 서비스를 함께 조회한다.
     */
    public List<Subscription> findActiveSubscriptionsOfPushUsers() {
        return subscriptionRepository.findAllActiveWithUserAndServiceOfPushUsers();
    }
}
