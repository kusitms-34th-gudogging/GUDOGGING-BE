package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestion;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 점검 문항 하나. options는 score 1(부정) ~ 5(긍정) 순이다.
 */
public record CheckupQuestionResponse(
    CheckupQuestionType type,
    String label,
    String hint,
    List<CheckupOptionResponse> options
) {

    public static CheckupQuestionResponse from(CheckupQuestion question) {
        List<CheckupOptionResponse> options = IntStream.range(0, question.options().size())
            .mapToObj(index -> new CheckupOptionResponse(index + 1, question.options().get(index)))
            .toList();
        return new CheckupQuestionResponse(question.type(), question.label(), question.hint(), options);
    }
}
