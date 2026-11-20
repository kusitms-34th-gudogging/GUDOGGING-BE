package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraftStep;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 불러온 구독 등록 임시저장 (A-01s '불러오기'). 클라이언트는 lastStep · editingItemIndex로 등록을 중단한 화면을 복원한다.
 */
public record SubscriptionDraftResponse(
    LocalDateTime savedAt,
    SubscriptionDraftStep lastStep,
    Integer editingItemIndex,
    List<SubscriptionDraftItemResponse> items
) {

    /**
     * logoUrlsByServiceId에 없는 서비스(직접 입력 · 로고 미등록 · 비활성 · 삭제)는 logoUrl을 null로 내려준다.
     */
    public static SubscriptionDraftResponse of(SubscriptionDraft subscriptionDraft,
                                               Map<UUID, String> logoUrlsByServiceId) {
        List<SubscriptionDraftItemResponse> items = subscriptionDraft.items().stream()
            .map(item -> SubscriptionDraftItemResponse.of(item,
                item.serviceId() != null ? logoUrlsByServiceId.get(item.serviceId()) : null))
            .toList();
        return new SubscriptionDraftResponse(
            subscriptionDraft.savedAt(),
            subscriptionDraft.lastStep(),
            subscriptionDraft.editingItemIndex(),
            items
        );
    }
}
