package com.subscribers.gudogging.domain.catalog.application.dto.request;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;

/**
 * services_seed.csv 한 행 (name, category, logo_url, cancel_url, sort_order). 빈 logo_url · cancel_url은 null.
 */
public record ServiceSeedRow(
    String name,
    Category category,
    String logoUrl,
    String cancelUrl,
    int sortOrder
) {
}
