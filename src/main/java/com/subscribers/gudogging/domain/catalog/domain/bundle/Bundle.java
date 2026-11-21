package com.subscribers.gudogging.domain.catalog.domain.bundle;

import java.util.List;
import java.util.Map;

/**
 * 번들 상품 (시트 '번들 상품 목록'). 포함 서비스는 요금제 이름(services.name)으로 들고 있다. DB 없이 seed/bundles_seed.tsv에서 읽는다.
 *
 * @param monthlyPrice   상품 월 가격 (카드는 null)
 * @param includesText   화면 '포함 내용' 표시 문구
 * @param appliedPrices  요금제별로 상품 가격이 다른 경우 요금제 이름 → 월 금액
 */
public record Bundle(
    String name,
    BundleRecommendType recommendType,
    BundleProviderType providerType,
    Integer monthlyPrice,
    String includesText,
    String condition,
    String tag1,
    String tag2,
    List<String> fixedServices,
    List<String> choiceAServices,
    List<String> choiceBServices,
    Map<String, Integer> appliedPrices,
    List<BundleDiscount> discounts
) {

    public Bundle {
        fixedServices = List.copyOf(fixedServices);
        choiceAServices = List.copyOf(choiceAServices);
        choiceBServices = List.copyOf(choiceBServices);
        appliedPrices = Map.copyOf(appliedPrices);
        discounts = List.copyOf(discounts);
    }

    /**
     * 고정 포함 · 택1 A · 택1 B 중 하나라도 이 요금제가 있으면 true.
     */
    public boolean includes(String serviceName) {
        return fixedServices.contains(serviceName)
            || choiceAServices.contains(serviceName)
            || choiceBServices.contains(serviceName);
    }
}
