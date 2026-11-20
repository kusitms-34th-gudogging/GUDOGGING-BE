package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.form.CheckupForm;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.time.LocalDate;
import java.util.List;

/**
 * 구독 점검 문항 (K-11~K-70). 문구는 카테고리별 상수에 서비스명 · 조사 · 금액 · 날짜를 채운 완성 문장이다.
 * amount는 월 환산 금액이고, 계획 문항 보조 문구의 '다음 결제 · ○원'만 실제 1회 결제 금액을 쓴다.
 */
public record CheckupFormResponse(
    Category category,
    List<CheckupQuestionResponse> questions,
    String lastMemo,
    String serviceName,
    long amount,
    long yearlyAmount,
    LocalDate nextBillingDate,
    String memoPlaceholder,
    List<String> memoChips
) {

    public static CheckupFormResponse of(Subscription subscription, SubscriptionCheck latestCheck, LocalDate today) {
        CheckupForm form = CheckupForm.of(subscription.getCategory());
        long monthlyAmount = subscription.monthlyAmount();
        long yearlyAmount = subscription.yearlyAmount();
        LocalDate nextBillingDate = subscription.nextBillingOn(today);
        List<CheckupQuestionResponse> questions = form.questions(subscription.getName(),
                subscription.objectParticle(), monthlyAmount, yearlyAmount, nextBillingDate, subscription.getAmount())
            .stream()
            .map(CheckupQuestionResponse::from)
            .toList();
        return new CheckupFormResponse(
            subscription.getCategory(),
            questions,
            latestCheck != null ? latestCheck.getMemo() : null,
            subscription.getName(),
            monthlyAmount,
            yearlyAmount,
            nextBillingDate,
            form.memoPlaceholder(),
            form.memoChips()
        );
    }
}
