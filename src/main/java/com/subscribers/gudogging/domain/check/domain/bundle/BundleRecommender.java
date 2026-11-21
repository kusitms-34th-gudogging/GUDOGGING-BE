package com.subscribers.gudogging.domain.check.domain.bundle;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleDiscount;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleDiscountTarget;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleProviderType;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleRecommendType;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 점검 결과 절약 제안 계산 (화면설계서 CHK-008 설명 4번). 포함 서비스 금액은 회원이 그 요금제로 등록한 구독의 월 환산 금액이고, 구독하지 않았거나 서비스 목록에 없는 포함
 * 서비스는 0원이다.
 */
public final class BundleRecommender {

    private static final int MAX_BUNDLES = 4;
    private static final int MAX_PER_PROVIDER = 2;
    private static final int PERCENT = 100;

    private BundleRecommender() {
    }

    /**
     * @param candidates    지금 점검한 요금제가 포함된 번들 (시트 순서)
     * @param subscriptions 금액을 더할 회원 구독 (구독 중 + 점검한 구독)
     */
    public static List<BundleRecommendation> recommend(List<Bundle> candidates, List<Subscription> subscriptions) {
        Map<String, Subscription> subscriptionsByService = subscriptions.stream()
            .filter(subscription -> subscription.getServiceCatalog() != null)
            .collect(Collectors.toMap(subscription -> subscription.getServiceCatalog().getName(), Function.identity(),
                (first, second) -> first, LinkedHashMap::new));

        List<BundleRecommendation> recommendations = candidates.stream()
            .map(bundle -> recommendationOf(bundle, subscriptionsByService))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparingInt((BundleRecommendation recommendation) -> typeOrder(recommendation))
                .thenComparing(recommendation -> recommendation.saving() == null ? 0 : -recommendation.saving()))
            .toList();
        return limit(recommendations);
    }

    private static Optional<BundleRecommendation> recommendationOf(Bundle bundle,
                                                                 Map<String, Subscription> subscriptionsByService) {
        return switch (bundle.recommendType()) {
            case FIXED -> fixedSaving(bundle, subscriptionsByService);
            case MAX -> maxSaving(bundle, subscriptionsByService);
            case BENEFIT -> Optional.of(new BundleRecommendation(bundle, null, null, bundle.monthlyPrice()));
            case EXCLUDED -> Optional.empty();
        };
    }

    /**
     * 확정 절약 — (고정 포함 합 + 택1 그룹마다 가장 비싼 1개) − 상품 가격, 0원 이하면 추천하지 않음. 서비스별 적용가가 있으면 고른 요금제의 적용가가 상품 가격.
     */
    private static Optional<BundleRecommendation> fixedSaving(Bundle bundle,
                                                            Map<String, Subscription> subscriptionsByService) {
        List<Subscription> paidSubscriptions = includedSubscriptions(bundle, subscriptionsByService);
        long paid = sumOf(paidSubscriptions);
        if (paid == 0) {
            return Optional.empty();
        }
        Integer price = paidSubscriptions.stream()
            .filter(subscription -> bundle.appliedPrices().containsKey(subscription.getServiceCatalog().getName()))
            .max(Comparator.comparingLong(Subscription::monthlyAmount))
            .map(subscription -> bundle.appliedPrices().get(subscription.getServiceCatalog().getName()))
            .orElse(bundle.monthlyPrice());
        if (price == null) {
            return Optional.empty();
        }
        long saving = paid - price;
        if (saving <= 0) {
            return Optional.empty();
        }
        return Optional.of(new BundleRecommendation(bundle, saving, (int) (saving * PERCENT / paid), price));
    }

    /**
     * 최대 절약(카드) — 할인 대상 구독마다 할인율(내림) 또는 min(할인액, 금액)을 더함. 택1 그룹은 가장 비싼 1개에만 적용.
     */
    private static Optional<BundleRecommendation> maxSaving(Bundle bundle,
                                                          Map<String, Subscription> subscriptionsByService) {
        Map<BundleDiscountTarget, List<Subscription>> groups = new EnumMap<>(BundleDiscountTarget.class);
        groups.put(BundleDiscountTarget.FIXED, subscribed(bundle.fixedServices(), subscriptionsByService));
        groups.put(BundleDiscountTarget.A, mostExpensive(bundle.choiceAServices(), subscriptionsByService).stream()
            .toList());
        groups.put(BundleDiscountTarget.B, mostExpensive(bundle.choiceBServices(), subscriptionsByService).stream()
            .toList());
        List<Subscription> all = Stream.of(BundleDiscountTarget.FIXED, BundleDiscountTarget.A, BundleDiscountTarget.B)
            .flatMap(target -> groups.get(target).stream())
            .toList();

        Set<Subscription> discounted = new LinkedHashSet<>();
        Set<Integer> appliedPercents = new LinkedHashSet<>();
        boolean amountApplied = false;
        long saving = 0;
        long targetAmount = 0;
        for (BundleDiscount discount : bundle.discounts()) {
            List<Subscription> targets = switch (discount.target()) {
                case ALL -> all;
                case FIXED, A, B -> groups.get(discount.target());
                case CATEGORY -> all.stream()
                    .filter(subscription -> subscription.getServiceCatalog().getCategory() == discount.category())
                    .toList();
            };
            for (Subscription subscription : targets) {
                if (!discounted.add(subscription)) {
                    continue;
                }
                long amount = subscription.monthlyAmount();
                targetAmount += amount;
                if (discount.percent() != null) {
                    saving += amount * discount.percent() / PERCENT;
                    appliedPercents.add(discount.percent());
                } else {
                    saving += Math.min(discount.amount(), amount);
                    amountApplied = true;
                }
            }
        }
        if (saving <= 0) {
            return Optional.empty();
        }
        int rate = !amountApplied && appliedPercents.size() == 1
            ? appliedPercents.iterator().next()
            : (int) (saving * PERCENT / targetAmount);
        return Optional.of(new BundleRecommendation(bundle, saving, rate, null));
    }

    private static List<Subscription> includedSubscriptions(Bundle bundle,
                                                            Map<String, Subscription> subscriptionsByService) {
        List<Subscription> included = new ArrayList<>(subscribed(bundle.fixedServices(), subscriptionsByService));
        mostExpensive(bundle.choiceAServices(), subscriptionsByService).ifPresent(included::add);
        mostExpensive(bundle.choiceBServices(), subscriptionsByService).ifPresent(included::add);
        return included;
    }

    private static List<Subscription> subscribed(List<String> serviceNames,
                                                 Map<String, Subscription> subscriptionsByService) {
        return serviceNames.stream()
            .map(subscriptionsByService::get)
            .filter(subscription -> subscription != null)
            .distinct()
            .toList();
    }

    private static Optional<Subscription> mostExpensive(List<String> serviceNames,
                                                        Map<String, Subscription> subscriptionsByService) {
        return subscribed(serviceNames, subscriptionsByService).stream()
            .max(Comparator.comparingLong(Subscription::monthlyAmount));
    }

    private static long sumOf(List<Subscription> subscriptions) {
        return subscriptions.stream().mapToLong(Subscription::monthlyAmount).sum();
    }

    /**
     * 확정 절약 → 카드 → 통신 요금제.
     */
    private static int typeOrder(BundleRecommendation recommendation) {
        BundleRecommendType type = recommendation.bundle().recommendType();
        return switch (type) {
            case FIXED -> 0;
            case MAX -> 1;
            default -> 2;
        };
    }

    /**
     * 앞에서부터 담되 같은 제공사 유형 최대 2개, 전체 최대 4개.
     */
    private static List<BundleRecommendation> limit(List<BundleRecommendation> recommendations) {
        Map<BundleProviderType, Integer> countByProvider = new EnumMap<>(BundleProviderType.class);
        List<BundleRecommendation> limited = new ArrayList<>();
        for (BundleRecommendation recommendation : recommendations) {
            if (limited.size() == MAX_BUNDLES) {
                break;
            }
            BundleProviderType provider = recommendation.bundle().providerType();
            int count = countByProvider.getOrDefault(provider, 0);
            if (count < MAX_PER_PROVIDER) {
                limited.add(recommendation);
                countByProvider.put(provider, count + 1);
            }
        }
        return limited;
    }
}
