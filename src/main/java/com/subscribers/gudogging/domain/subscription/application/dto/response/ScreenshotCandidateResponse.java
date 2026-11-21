package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.ScreenshotCandidate;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.ScreenshotReviewField;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 스크린샷 분석 후보 1건 (A-09 항목, A-15 입력값). 서비스명·카테고리·로고는 DB 서비스 값, 결제 정보는 OCR 값이다.
 */
public record ScreenshotCandidateResponse(
    UUID serviceId,
    String serviceName,
    String logoUrl,
    Category category,
    Integer amount,
    Integer billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod,
    boolean needsReview,
    List<ScreenshotReviewField> reviewFields
) {

    public static ScreenshotCandidateResponse from(ScreenshotCandidate candidate, ServiceCatalog service) {
        return new ScreenshotCandidateResponse(
            service.getId(),
            service.getName(),
            service.getLogoUrl(),
            service.getCategory(),
            candidate.amount(),
            candidate.billingCycleValue(),
            candidate.billingCycleUnit(),
            candidate.nextBillingDate(),
            candidate.paymentMethod(),
            candidate.needsReview(),
            candidate.reviewFields()
        );
    }
}
