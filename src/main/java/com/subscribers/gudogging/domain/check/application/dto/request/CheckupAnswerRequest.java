package com.subscribers.gudogging.domain.check.application.dto.request;

import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckupAnswerRequest(
    @NotNull CheckupQuestionType type,
    @NotNull @Min(1) @Max(5) Integer score
) {
}
