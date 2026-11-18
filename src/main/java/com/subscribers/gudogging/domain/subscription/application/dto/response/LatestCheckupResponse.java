package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.CancelReason;
import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 가장 최근 점검 1건 — M-05 '마지막 점검', C-03 최근 점검.
 * 응답 구조만 정의한다. 값을 채우는 정적 팩토리는 구독 점검 후속 스펙에서 추가한다.
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
}
