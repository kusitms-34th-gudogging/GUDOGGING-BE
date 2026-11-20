package com.subscribers.gudogging.domain.subscription.application.dto.request;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraftStep;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 직접 등록 흐름의 A-04 목록(items)과 마지막 화면(lastStep). 기존 임시저장을 통째로 덮어쓴다.
 */
public record SaveSubscriptionDraftRequest(
    @NotNull SubscriptionDraftStep lastStep,
    @NotEmpty @Size(max = 50) List<@Valid SubscriptionDraftItemRequest> items
) {
}
