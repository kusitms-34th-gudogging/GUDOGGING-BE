package com.subscribers.gudogging.domain.check.domain.result;

import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
    private static final int HIGHEST_SCORE = 5;
    private static final int KEEP_SIDE_MIN_PERCENT = 60;
    /**
     * 동점일 때 우선순위: 계획 > 이용도 > 만족도.
     */
    private static final List<CheckupQuestionType> TIE_BREAK_ORDER =
        List.of(CheckupQuestionType.PLAN, CheckupQuestionType.USAGE, CheckupQuestionType.SATISFACTION);

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

    /**
     * 결과에 가장 큰 영향을 준 응답 (K-80 보조 문구 선택용). 활용도 60 이상은 가중 점수 (응답−1)×가중치, 60 미만은 가중 감점 (5−응답)×가중치가
     * 가장 큰 문항이다. 동점이면 계획 > 이용도 > 만족도, 계획이 1점이면 계획으로 고정한다.
     */
    public static CheckupQuestionType mainFactorOf(int usageScore, int satisfactionScore, int planScore,
                                                   int scorePercent) {
        if (planScore == LOWEST_PLAN_SCORE) {
            return CheckupQuestionType.PLAN;
        }
        boolean keepSide = scorePercent >= KEEP_SIDE_MIN_PERCENT;
        CheckupQuestionType mainFactor = null;
        int maxImpact = -1;
        for (CheckupQuestionType type : TIE_BREAK_ORDER) {
            int score = switch (type) {
                case USAGE -> usageScore;
                case SATISFACTION -> satisfactionScore;
                case PLAN -> planScore;
            };
            int impact = (keepSide ? score - 1 : HIGHEST_SCORE - score) * weightOf(type);
            if (impact > maxImpact) {
                maxImpact = impact;
                mainFactor = type;
            }
        }
        return mainFactor;
    }

    private static int weightOf(CheckupQuestionType type) {
        return switch (type) {
            case USAGE -> USAGE_WEIGHT;
            case SATISFACTION -> SATISFACTION_WEIGHT;
            case PLAN -> PLAN_WEIGHT;
        };
    }
}
