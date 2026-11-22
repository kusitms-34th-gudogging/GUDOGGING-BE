package com.subscribers.gudogging.domain.gmail.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.gmail.domain.GmailCandidate;
import com.subscribers.gudogging.domain.gmail.domain.GmailReviewField;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GmailCandidateResponse(
    UUID serviceId,
    String serviceName,
    String logoUrl,
    Category category,
    Integer amount,
    int billingCycleValue,
    BillingCycleUnit billingCycleUnit,
    LocalDate nextBillingDate,
    PaymentMethod paymentMethod,
    boolean needsReview,
    List<GmailReviewField> reviewFields
) {

    public static GmailCandidateResponse from(GmailCandidate candidate) {
        return new GmailCandidateResponse(
            candidate.service().getId(),
            candidate.service().getName(),
            candidate.service().getLogoUrl(),
            candidate.service().getCategory(),
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
