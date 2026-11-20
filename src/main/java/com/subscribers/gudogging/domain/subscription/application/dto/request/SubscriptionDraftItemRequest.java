package com.subscribers.gudogging.domain.subscription.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * serviceId가 있으면 사전 등록 서비스, null이면 직접 입력 서비스. 결제 정보(category · amount · billingCycleValue · billingCycleUnit ·
 * nextBillingDate · paymentMethod)는 사전 등록 서비스면 모두 null(미입력) 또는 모두 입력, 직접 입력 서비스면 모두 입력 (Figma A-03 '일부만 입력한 상태로 임시 저장
 * 불가'). 다음 결제일은 임시저장이라 과거 날짜도 허용하고, 등록할 때 검사한다.
 */
public record SubscriptionDraftItemRequest(
    UUID serviceId,
    @NotBlank @Size(max = 50) String serviceName,
    Category category,
    @Positive Integer amount,
    @Min(1) @Max(31) Integer billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod
) {

    private static final int MAX_CUSTOM_SERVICE_NAME_LENGTH = 30;

    @JsonIgnore
    @AssertTrue(message = "직접 입력 서비스의 서비스 이름은 30자 이하여야 합니다.")
    public boolean isCustomServiceNameValid() {
        return serviceId != null || serviceName == null
            || serviceName.strip().length() <= MAX_CUSTOM_SERVICE_NAME_LENGTH;
    }

    /**
     * 사전 등록 서비스만 검사한다. 직접 입력 서비스는 isCustomPaymentInfoComplete에서 검사해 에러가 한 번만 나가게 한다.
     */
    @JsonIgnore
    @AssertTrue(message = "결제 정보는 모두 입력하거나 모두 비워 두어야 합니다.")
    public boolean isPaymentInfoAllOrNone() {
        if (serviceId == null) {
            return true;
        }
        long enteredCount = countEnteredPaymentInfo();
        return enteredCount == 0 || enteredCount == paymentInfo().size();
    }

    @JsonIgnore
    @AssertTrue(message = "직접 입력 서비스는 결제 정보를 모두 입력해야 합니다.")
    public boolean isCustomPaymentInfoComplete() {
        return serviceId != null || countEnteredPaymentInfo() == paymentInfo().size();
    }

    /**
     * 결제 정보 필드 목록. 필드를 추가 · 삭제하면 여기만 고친다 (null 포함이라 Arrays.asList).
     */
    private List<Object> paymentInfo() {
        return Arrays.asList(category, amount, billingCycleValue, billingCycleUnit, nextBillingDate, paymentMethod);
    }

    private long countEnteredPaymentInfo() {
        return paymentInfo().stream()
            .filter(Objects::nonNull)
            .count();
    }
}
