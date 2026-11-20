package com.subscribers.gudogging.domain.subscription.domain.draft;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 회원당 1건인 구독 등록 임시저장. 직접 등록 흐름의 목록(items)과 마지막 화면 · 입력 중이던 항목을 그대로 담는다 (JPA 엔티티 아님, Redis 저장). editingItemIndex는
 * lastStep이 INPUT_SUBSCRIPTION일 때 입력 · 수정 중이던 items 순번이고, null이면 새 직접 입력(A-03s)이다.
 */
public record SubscriptionDraft(
    UUID userId,
    SubscriptionDraftStep lastStep,
    Integer editingItemIndex,
    List<SubscriptionDraftItem> items,
    LocalDateTime savedAt
) {

    public static SubscriptionDraft of(UUID userId, SubscriptionDraftStep lastStep, Integer editingItemIndex,
                                       List<SubscriptionDraftItem> items, LocalDateTime savedAt) {
        return new SubscriptionDraft(userId, lastStep, editingItemIndex, List.copyOf(items), savedAt);
    }
}
