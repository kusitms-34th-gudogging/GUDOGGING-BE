package com.subscribers.gudogging.domain.subscription.domain.draft;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 회원당 1건인 구독 등록 임시저장. 직접 등록 흐름의 A-04 목록과 마지막 화면을 그대로 담는다 (JPA 엔티티 아님, Redis 저장).
 */
public record SubscriptionDraft(
    UUID userId,
    SubscriptionDraftStep lastStep,
    List<SubscriptionDraftItem> items,
    LocalDateTime savedAt
) {

    public static SubscriptionDraft of(UUID userId, SubscriptionDraftStep lastStep, List<SubscriptionDraftItem> items,
                                       LocalDateTime savedAt) {
        return new SubscriptionDraft(userId, lastStep, List.copyOf(items), savedAt);
    }
}
