package com.subscribers.gudogging.domain.check.domain.form;

import java.util.List;

/**
 * 서비스명 · 조사 · 금액 · 날짜를 채운 점검 문항 하나. options는 1점(부정)부터 5점(긍정) 순이다.
 */
public record CheckupQuestion(
    CheckupQuestionType type,
    String label,
    String hint,
    List<String> options
) {
}
