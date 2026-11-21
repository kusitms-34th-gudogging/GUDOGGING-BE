package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.form.CheckupForm;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import java.time.LocalDate;

/**
 * 지난 점검 원본 기록 1건 — 평균 기간(이번 달 포함 최근 3개월)의 점검. 응답 문구는 점검 당시 카테고리의 선택지 문구다.
 */
public record PastCheckupResponse(
    LocalDate checkedAt,
    String usageAnswer,
    String satisfactionAnswer,
    String planAnswer,
    CheckDecision decision
) {

    public static PastCheckupResponse from(SubscriptionCheck check) {
        CheckupForm form = CheckupForm.of(check.getCategory());
        return new PastCheckupResponse(
            check.getCreatedAt().toLocalDate(),
            form.answerOf(CheckupQuestionType.USAGE, check.getUsageScore()),
            form.answerOf(CheckupQuestionType.SATISFACTION, check.getSatisfactionScore()),
            form.answerOf(CheckupQuestionType.PLAN, check.getPlanScore()),
            check.getDecision()
        );
    }
}
