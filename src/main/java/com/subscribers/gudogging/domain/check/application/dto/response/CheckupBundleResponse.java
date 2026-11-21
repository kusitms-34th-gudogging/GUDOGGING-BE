package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleRecommendType;
import com.subscribers.gudogging.domain.check.domain.bundle.BundleRecommendation;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 절약 제안 카드 (화면설계서 K-80 '번들 추천 카드'). 큰 숫자 = monthlySaving(BENEFIT은 tags[1] 혜택), 막대 = savingRate, 아래 문구 = monthlyPrice.
 *
 * @param includes  카드 둘째 줄 포함 내용 한 줄 (Bundle.includesSummary — 고정 ' + ', 택1 ' · … 등 택1')
 * @param tags      [태그1 제공사 · 상품군, 태그2 핵심 혜택] — 빈 태그는 뺌
 * @param priceType FIXED(확정 절약 '월 N원') / MAX(카드 '최대 N원') / BENEFIT(통신 요금제)
 */
public record CheckupBundleResponse(
    String name,
    String includes,
    List<String> tags,
    BundleRecommendType priceType,
    Long monthlySaving,
    Integer savingRate,
    Integer monthlyPrice
) {

    public static CheckupBundleResponse from(BundleRecommendation recommendation) {
        Bundle bundle = recommendation.bundle();
        return new CheckupBundleResponse(
            bundle.name(),
            bundle.includesSummary(),
            Stream.of(bundle.tag1(), bundle.tag2()).filter(Objects::nonNull).toList(),
            bundle.recommendType(),
            recommendation.saving(),
            recommendation.rate(),
            recommendation.price()
        );
    }
}
