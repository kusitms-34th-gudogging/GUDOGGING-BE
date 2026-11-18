package com.subscribers.gudogging.domain.subscription.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * serviceId가 있으면 사전 등록 서비스(serviceName 무시), null이면 직접 입력 서비스(serviceName 필수).
 */
public record SubscriptionItemRequest(
    UUID serviceId,
    @Size(max = 30) String serviceName,
    @NotNull Category category,
    @NotNull @Positive Integer amount,
    @NotNull @Min(1) @Max(31) Integer billingCycleValue,
    @NotNull BillingCycleUnit billingCycleUnit,
    @NotNull @FutureOrPresent LocalDate nextBillingDate,
    @NotNull PaymentMethod paymentMethod
) {

    @JsonIgnore
    @AssertTrue(message = "직접 입력 서비스는 서비스 이름이 필요합니다.")
    public boolean isServiceNameValid() {
        return serviceId != null || (serviceName != null && !serviceName.isBlank());
    }
}
