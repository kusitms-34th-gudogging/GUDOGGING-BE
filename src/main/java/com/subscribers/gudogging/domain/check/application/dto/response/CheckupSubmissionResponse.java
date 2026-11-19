package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import java.util.UUID;

/**
 * 점검 제출 결과. 결과 화면(K-80)은 이 checkupId로 결과 조회 API를 호출한다.
 */
public record CheckupSubmissionResponse(
    UUID checkupId
) {

    public static CheckupSubmissionResponse from(SubscriptionCheck subscriptionCheck) {
        return new CheckupSubmissionResponse(subscriptionCheck.getId());
    }
}
