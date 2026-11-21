package com.subscribers.gudogging.domain.catalog.domain.bundle;

/**
 * 카드 할인 적용 대상 — 포함 서비스 전체, 고정 포함 서비스, 택1 그룹 A · B, 또는 카테고리.
 */
public enum BundleDiscountTarget {
    ALL,
    FIXED,
    A,
    B,
    CATEGORY
}
