package com.subscribers.gudogging.domain.check.domain.result;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import java.time.LocalDate;
import java.util.List;

/**
 * 최근 3개월 점검 응답 평균 (M-03 '지난 점검', ERD 6.4). 문항별 점수는 평균을 반올림한 점수, 활용도는 소수 평균으로 계산한 값이다. 이용도 선택지 문구에 쓸 카테고리는 기간 내 가장
 * 최근 점검의 카테고리다.
 */
public record CheckAverage(
    Category category,
    int usageScore,
    int satisfactionScore,
    int planScore,
    int scorePercent,
    CheckResultStage stage
) {

    private static final int PERIOD_MONTHS_BEFORE = 2;

    /**
     * 평균 기간 시작일 — 이번 달을 포함한 최근 3개월의 첫날 (예: 10/21 → 8/1).
     */
    public static LocalDate periodFromOf(LocalDate today) {
        return today.withDayOfMonth(1).minusMonths(PERIOD_MONTHS_BEFORE);
    }

    /**
     * @param checks 평균 기간의 점검 1건 이상, 최신순. 1건이면 그 기록 그대로 쓴다.
     */
    public static CheckAverage of(List<SubscriptionCheck> checks) {
        SubscriptionCheck latest = checks.getFirst();
        if (checks.size() == 1) {
            return new CheckAverage(latest.getCategory(), latest.getUsageScore(), latest.getSatisfactionScore(),
                latest.getPlanScore(), latest.getScorePercent(), latest.getStage());
        }
        int scorePercent = CheckScore.percentOfAverage(checks);
        return new CheckAverage(
            latest.getCategory(),
            CheckScore.roundedAverageOf(checks, SubscriptionCheck::getUsageScore),
            CheckScore.roundedAverageOf(checks, SubscriptionCheck::getSatisfactionScore),
            CheckScore.roundedAverageOf(checks, SubscriptionCheck::getPlanScore),
            scorePercent,
            CheckResultStage.of(scorePercent)
        );
    }
}
