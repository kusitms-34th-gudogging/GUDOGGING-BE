package com.subscribers.gudogging.domain.check.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 점검 4/4 '결과 보기' — 이용도 · 만족도 · 계획 응답 하나씩과 메모(선택). type 중복 · 누락은 UseCase에서 검사한다.
 */
public record SubmitCheckupRequest(
    @NotNull @Size(min = 3, max = 3) List<@NotNull @Valid CheckupAnswerRequest> answers,
    @Size(max = 100) String memo
) {
}
