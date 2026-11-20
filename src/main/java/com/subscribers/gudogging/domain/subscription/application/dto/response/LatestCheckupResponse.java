package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.CancelReason;
import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import com.subscribers.gudogging.domain.check.domain.form.CheckupForm;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 가장 최근 점검 1건 — M-05 '마지막 점검', C-03 '최근 점검'. 응답 문구는 점검 당시 카테고리의 선택지 문구이고, 해지 이유는 K-90에서 고른 선택지 코드(K-90 순서)와 직접 입력한 문장이다.
 */
public record LatestCheckupResponse(
    UUID checkupId,
    LocalDate checkedAt,
    CheckDecision decision,
    String usageAnswer,
    String satisfactionAnswer,
    String planAnswer,
    CheckResultStage stage,
    int score,
    String memo,
    List<CancelReason> cancelReasons,
    String cancelReasonText
) {

    public static LatestCheckupResponse of(SubscriptionCheck check, List<SubscriptionCheckCancelReason> cancelReasons) {
        CheckupForm form = CheckupForm.of(check.getCategory());
        return new LatestCheckupResponse(
            check.getId(),
            check.getCreatedAt().toLocalDate(),
            check.getDecision(),
            form.answerOf(CheckupQuestionType.USAGE, check.getUsageScore()),
            form.answerOf(CheckupQuestionType.SATISFACTION, check.getSatisfactionScore()),
            form.answerOf(CheckupQuestionType.PLAN, check.getPlanScore()),
            check.getStage(),
            check.getScorePercent(),
            check.getMemo(),
            cancelReasons.stream().map(SubscriptionCheckCancelReason::getReason).sorted().toList(),
            check.getCancelReasonText()
        );
    }
}
