package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import java.time.LocalDate;

/**
 * 지난 점검 평균 — M-03 '지난 점검'.
 * 응답 구조만 정의한다. 값을 채우는 정적 팩토리는 구독 점검 후속 스펙에서 추가한다.
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
}
