package com.subscribers.gudogging.domain.check.domain.result;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 활용도(%) 계산 (ERD 6.4). 반올림((이용도−1)÷4×40 + (만족도−1)÷4×25 + (계획−1)÷4×35), 계획이 1점이면 최대 59%.
 */
public final class CheckScore {

    private static final int USAGE_WEIGHT = 40;
    private static final int SATISFACTION_WEIGHT = 25;
    private static final int PLAN_WEIGHT = 35;
    private static final int SCORE_STEPS = 4;
    private static final int LOWEST_PLAN_SCORE = 1;
    private static final int LOWEST_PLAN_MAX_PERCENT = 59;

    private CheckScore() {
    }

    public static int percentOf(int usageScore, int satisfactionScore, int planScore) {
        int weightedSum = (usageScore - 1) * USAGE_WEIGHT
            + (satisfactionScore - 1) * SATISFACTION_WEIGHT
            + (planScore - 1) * PLAN_WEIGHT;
        int percent = BigDecimal.valueOf(weightedSum)
            .divide(BigDecimal.valueOf(SCORE_STEPS), 0, RoundingMode.HALF_UP)
            .intValueExact();
        return planScore == LOWEST_PLAN_SCORE ? Math.min(percent, LOWEST_PLAN_MAX_PERCENT) : percent;
    }
}
