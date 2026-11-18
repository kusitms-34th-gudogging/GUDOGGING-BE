package com.subscribers.gudogging.domain.catalog.domain.entity;

import com.subscribers.gudogging.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "services",
    indexes = {
        @Index(name = "idx_services_name", columnList = "name"),
        @Index(name = "idx_services_category", columnList = "category")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceCatalog extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "name_en", length = 50)
    private String nameEn;

    @Column(name = "search_keywords", length = 255)
    private String searchKeywords;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "object_particle", length = 1)
    private String objectParticle;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * 시드 CSV 값으로 갱신한다. CSV에 있는 컬럼(category, logo_url, cancel_url, sort_order)만 바꾸고 나머지(name_en, search_keywords,
     * object_particle, is_active)는 건드리지 않는다.
     */
    public void updateFromSeed(Category category, String logoUrl, String cancelUrl, int sortOrder) {
        this.category = category;
        this.logoUrl = logoUrl;
        this.cancelUrl = cancelUrl;
        this.sortOrder = sortOrder;
    }

    public static ServiceCatalog create(
        String name,
        String nameEn,
        String searchKeywords,
        Category category,
        String logoUrl,
        int sortOrder
    ) {
        return create(name, nameEn, searchKeywords, category, logoUrl, null, sortOrder);
    }

    public static ServiceCatalog create(
        String name,
        String nameEn,
        String searchKeywords,
        Category category,
        String logoUrl,
        String cancelUrl,
        int sortOrder
    ) {
        return new ServiceCatalog(name, nameEn, searchKeywords, category, logoUrl,
            null, cancelUrl, sortOrder, true);
    }

}
