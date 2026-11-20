package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraftItem;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 임시저장한 구독 1건. 결제 정보는 저장값 그대로(미입력이면 null)이고, logoUrl은 조회 시점의 서비스 로고(null이면 기본 이미지)다.
 */
public record SubscriptionDraftItemResponse(
    UUID serviceId,
    String serviceName,
    String logoUrl,
    Category category,
    Integer amount,
    Integer billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod
) {

    public static SubscriptionDraftItemResponse of(SubscriptionDraftItem item, String logoUrl) {
        return new SubscriptionDraftItemResponse(
            item.serviceId(),
            item.serviceName(),
            logoUrl,
            item.category(),
            item.amount(),
            item.billingCycleValue(),
            item.billingCycleUnit(),
            item.nextBillingDate(),
            item.paymentMethod()
        );
    }
}
