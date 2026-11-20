package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.form.CheckupForm;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import com.subscribers.gudogging.domain.check.domain.result.CheckAverage;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import java.time.LocalDate;

/**
 * 지난 점검 평균 — M-03 '지난 점검'. 이번 달 포함 최근 3개월 점검의 문항별 평균을 반올림한 점수의 선택지 문구와, 평균으로 계산한 활용도 · 단계.
 */
public record PastCheckupAverageResponse(
    LocalDate periodFrom,
    LocalDate periodTo,
    String usageAnswer,
    String satisfactionAnswer,
    String planAnswer,
    int score,
    CheckResultStage stage
) {

    public static PastCheckupAverageResponse of(CheckAverage average, LocalDate periodFrom, LocalDate periodTo) {
        CheckupForm form = CheckupForm.of(average.category());
        return new PastCheckupAverageResponse(
            periodFrom,
            periodTo,
            form.answerOf(CheckupQuestionType.USAGE, average.usageScore()),
            form.answerOf(CheckupQuestionType.SATISFACTION, average.satisfactionScore()),
            form.answerOf(CheckupQuestionType.PLAN, average.planScore()),
            average.scorePercent(),
            average.stage()
        );
    }
}
