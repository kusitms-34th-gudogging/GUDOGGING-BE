package com.subscribers.gudogging.domain.catalog.domain.bundle;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;

/**
 * 카드 할인 한 줄 — 할인율(percent) 또는 고정 할인액(amount, 원) 중 하나. target이 CATEGORY면 category에 해당하는 포함 서비스에만 적용.
 */
public record BundleDiscount(BundleDiscountTarget target, Category category, Integer percent, Integer amount) {
}
