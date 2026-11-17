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

    @Column(name = "cancel_guide", columnDefinition = "TEXT")
    private String cancelGuide;

    @Column(name = "cancel_notice", length = 255)
    private String cancelNotice;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public static ServiceCatalog create(
        String name,
        String nameEn,
        String searchKeywords,
        Category category,
        String logoUrl,
        int sortOrder
    ) {
        return new ServiceCatalog(name, nameEn, searchKeywords, category, logoUrl,
            null, null, null, null, sortOrder, true);
    }

}
