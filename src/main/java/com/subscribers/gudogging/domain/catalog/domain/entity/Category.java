package com.subscribers.gudogging.domain.catalog.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    VIDEO_OTT("영상·OTT"),
    MUSIC("음악"),
    SHOPPING_DELIVERY("쇼핑·배달 멤버십"),
    EDUCATION("교육·학습"),
    PRODUCTIVITY_AI("생산성·AI"),
    ENTERTAINMENT("엔터테인먼트"),
    ETC("기타");

    private final String displayName;
}
