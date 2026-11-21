package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.bundle.BundleRecommendation;
import com.subscribers.gudogging.domain.check.domain.form.CheckupForm;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import com.subscribers.gudogging.domain.check.domain.result.CheckScore;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 점검 결과 (K-80). 응답 문구는 점검 당시 카테고리의 선택지 문구다. 절약 제안(bundles)은 BundleRecommender가 계산한 최대 4개.
 * 추천 문구 · 유형명 · 보조 문구는 클라이언트가 stage · mainFactor로 고른다.
 */
public record CheckupResultResponse(
    UUID checkupId,
    UUID subscriptionId,
    String serviceName,
    LocalDate nextBillingDate,
    long yearlyCost,
    int score,
    CheckResultStage stage,
    String usageAnswer,
    String satisfactionAnswer,
    String planAnswer,
    String memo,
    List<CheckupBundleResponse> bundles,
    CheckupQuestionType mainFactor,
    CategorySpendingResponse categorySpending
) {

    public static CheckupResultResponse of(SubscriptionCheck check, List<Subscription> subscriptions,
                                           List<BundleRecommendation> bundles, LocalDate today) {
        Subscription subscription = check.getSubscription();
        CheckupForm form = CheckupForm.of(check.getCategory());
        boolean active = subscription.getStatus() == SubscriptionStatus.ACTIVE;
        return new CheckupResultResponse(
            check.getId(),
            subscription.getId(),
            subscription.getName(),
            active ? subscription.nextBillingOn(today) : null,
            subscription.yearlyAmount(),
            check.getScorePercent(),
            check.getStage(),
            form.answerOf(CheckupQuestionType.USAGE, check.getUsageScore()),
            form.answerOf(CheckupQuestionType.SATISFACTION, check.getSatisfactionScore()),
            form.answerOf(CheckupQuestionType.PLAN, check.getPlanScore()),
            check.getMemo(),
            bundles.stream().map(CheckupBundleResponse::from).toList(),
            CheckScore.mainFactorOf(check.getUsageScore(), check.getSatisfactionScore(), check.getPlanScore(),
                check.getScorePercent()),
            CategorySpendingResponse.of(check.getCategory(), subscription, subscriptions)
        );
    }
}
