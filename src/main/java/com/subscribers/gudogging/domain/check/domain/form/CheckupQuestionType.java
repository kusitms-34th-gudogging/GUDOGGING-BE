package com.subscribers.gudogging.domain.check.domain.form;

/**
 * 점검 문항 종류 — 이용도(가중치 40) · 만족도(25) · 다음 달 계획(35) (ERD 6.4).
 */
public enum CheckupQuestionType {
    USAGE,
    SATISFACTION,
    PLAN
}
