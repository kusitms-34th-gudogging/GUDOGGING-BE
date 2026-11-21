package com.subscribers.gudogging.domain.catalog.domain.bundle;

/**
 * 번들 추천 노출 방식 (시트 '추천 노출 방식').
 */
public enum BundleRecommendType {
    /** 확정 절약 (결합 · 할인 상품) — '월 N원' */
    FIXED,
    /** 최대 절약 (카드) — '최대 N원' */
    MAX,
    /** 혜택 소개 (통신 요금제) — 계산 없이 태그2 혜택 표시 */
    BENEFIT,
    /** 제외 (혜택 전용 · 비교할 서비스 없음) — 노출 안 함 */
    EXCLUDED
}
