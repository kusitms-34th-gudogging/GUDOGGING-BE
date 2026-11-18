package com.subscribers.gudogging.domain.subscription.application.dto.request;

import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 직접 입력 · 스크린샷 · Gmail 등록 공통. source는 registration_source로 저장한다.
 */
public record CreateSubscriptionsRequest(
    @NotNull RegistrationSource source,
    @NotEmpty @Size(max = 50) List<@Valid SubscriptionItemRequest> subscriptions
) {
}
