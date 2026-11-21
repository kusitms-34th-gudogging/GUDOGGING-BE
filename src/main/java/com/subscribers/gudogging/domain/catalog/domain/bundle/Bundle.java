package com.subscribers.gudogging.domain.catalog.domain.bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 번들 상품 (시트 '번들 상품 목록'). 포함 서비스는 요금제 이름(services.name)으로 들고 있다. DB 없이 seed/bundles_seed.tsv에서 읽는다.
 *
 * @param monthlyPrice   상품 월 가격 (카드는 null)
 * @param appliedPrices  요금제별로 상품 가격이 다른 경우 요금제 이름 → 월 금액
 */
public record Bundle(
    String name,
    BundleRecommendType recommendType,
    BundleProviderType providerType,
    Integer monthlyPrice,
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

    private static final int MAX_NAMES = 2;

    /**
     * 카드 둘째 줄 '포함 내용' 한 줄 — 고정 포함은 ' + ', 택1은 첫 번째 그룹만 ' · ' + ' 택1'(각 최대 2개), 남는 서비스가 있으면 ' 등'.
     * 같은 서비스의 요금제가 여러 개면 서비스 이름(첫 단어) 하나로 줄인다. 예: 'Netflix · YouTube Premium 등 택1'
     */
    public String includesSummary() {
        List<String> choice = choiceAServices.isEmpty() ? choiceBServices : choiceAServices;
        boolean otherChoiceLeft = !choiceAServices.isEmpty() && !choiceBServices.isEmpty();
        List<String> parts = new ArrayList<>();
        if (!fixedServices.isEmpty()) {
            parts.add(joinNames(fixedServices, " + ", choice.isEmpty() && otherChoiceLeft));
        }
        if (!choice.isEmpty()) {
            parts.add(joinNames(choice, " · ", otherChoiceLeft) + " 택1");
        }
        return String.join(" + ", parts);
    }

    private static String joinNames(List<String> serviceNames, String separator, boolean moreLeft) {
        Map<String, List<String>> plansByService = new LinkedHashMap<>();
        for (String serviceName : serviceNames) {
            plansByService.computeIfAbsent(serviceName.split(" ")[0], key -> new ArrayList<>()).add(serviceName);
        }
        List<String> names = plansByService.entrySet().stream()
            .map(entry -> entry.getValue().size() == 1 ? entry.getValue().get(0) : entry.getKey())
            .toList();
        String joined = String.join(separator, names.subList(0, Math.min(MAX_NAMES, names.size())));
        return names.size() > MAX_NAMES || moreLeft ? joined + " 등" : joined;
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
