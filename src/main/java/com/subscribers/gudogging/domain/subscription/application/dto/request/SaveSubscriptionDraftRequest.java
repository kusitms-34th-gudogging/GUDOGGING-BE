package com.subscribers.gudogging.domain.subscription.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraftStep;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 직접 등록 흐름의 목록(items)과 마지막 화면(lastStep), 구독 정보 입력 화면(A-03 / A-03s)에서 입력 중이던 항목 순번(editingItemIndex). 기존 임시저장을 통째로 덮어쓴다.
 * editingItemIndex는 lastStep이 INPUT_SUBSCRIPTION일 때만 보내고, null이면 새 직접 입력(A-03s)이다.
 */
public record SaveSubscriptionDraftRequest(
    @NotNull SubscriptionDraftStep lastStep,
    Integer editingItemIndex,
    @NotEmpty @Size(max = 50) List<@Valid SubscriptionDraftItemRequest> items
) {

    /**
     * lastStep이 INPUT_SUBSCRIPTION이 아니면 editingItemIndex는 null, INPUT_SUBSCRIPTION이면 null 또는 items 범위 안의 순번. items가
     * null이면 @NotEmpty가 따로 검사한다.
     */
    @JsonIgnore
    @AssertTrue(message = "editingItemIndex는 구독 정보 입력 화면에서만 items 범위 안의 순번으로 보낼 수 있습니다.")
    public boolean isEditingItemIndexValid() {
        if (editingItemIndex == null) {
            return true;
        }
        if (lastStep != SubscriptionDraftStep.INPUT_SUBSCRIPTION) {
            return false;
        }
        return items == null || (editingItemIndex >= 0 && editingItemIndex < items.size());
    }
}
