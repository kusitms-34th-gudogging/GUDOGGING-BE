package com.subscribers.gudogging.domain.subscription.application.dto.request;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 재등록 화면(M-05 '재등록하기' → 기존 정보가 채워진 구독 정보 입력)은 전체 값을 보낸다. 필드 정의와 검증은 구독 정보 수정(UpdateSubscriptionRequest)과 같다 —
 * serviceName만 선택이며 직접 입력 서비스는 null이면 기존 이름 유지, 사전 등록 서비스는 무시한다.
 */
public record ReactivateSubscriptionRequest(
    @Size(max = 30) @Pattern(regexp = ".*\\S.*", message = "서비스 이름은 공백만 입력할 수 없습니다.") String serviceName,
    @NotNull Category category,
    @NotNull @Positive Integer amount,
    @NotNull @Min(1) @Max(31) Integer billingCycleValue,
    @NotNull BillingCycleUnit billingCycleUnit,
    @NotNull @FutureOrPresent LocalDate nextBillingDate,
    @NotNull PaymentMethod paymentMethod
) {
}
