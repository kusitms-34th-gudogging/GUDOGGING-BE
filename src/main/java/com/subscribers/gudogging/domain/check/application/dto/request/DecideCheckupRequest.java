package com.subscribers.gudogging.domain.check.application.dto.request;

import com.subscribers.gudogging.domain.check.domain.entity.CancelReason;
import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 점검 결과(K-80) '유지하기' / 해지 이유(K-90) '해지 방법 보기' · '건너뛰기'. cancelReasons · cancelReasonText는 CANCEL일 때만 쓰고, 결정과 맞지 않는 값은
 * 무시한다. 카테고리별 해지 이유 문구는 클라이언트가 표시하고 서버는 코드만 받는다.
 */
public record DecideCheckupRequest(
    @NotNull CheckDecision decision,
    List<@NotNull CancelReason> cancelReasons,
    @Size(max = 100) String cancelReasonText
) {
}
