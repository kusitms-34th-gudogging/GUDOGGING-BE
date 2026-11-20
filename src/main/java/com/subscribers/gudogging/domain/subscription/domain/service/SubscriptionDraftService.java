package com.subscribers.gudogging.domain.subscription.domain.service;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionDraftRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionDraftService {

    private final SubscriptionDraftRepository subscriptionDraftRepository;

    /**
     * 회원의 임시저장을 새 내용으로 통째로 덮어쓴다 (회원당 1건).
     */
    public void saveDraft(SubscriptionDraft subscriptionDraft) {
        subscriptionDraftRepository.save(subscriptionDraft);
    }

    /**
     * 회원의 임시저장을 조회한다. 없으면 빈 Optional.
     */
    public Optional<SubscriptionDraft> findDraft(UUID userId) {
        return subscriptionDraftRepository.findByUserId(userId);
    }

    /**
     * 회원의 임시저장을 지운다. 없어도 예외 없이 끝난다.
     */
    public void deleteDraft(UUID userId) {
        subscriptionDraftRepository.deleteByUserId(userId);
    }
}
