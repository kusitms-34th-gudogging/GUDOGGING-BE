package com.subscribers.gudogging.domain.check.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 같은 카테고리 지출 (K-80 '{카테고리} 구독'). 같은 카테고리의 구독 중 구독을 월 환산 금액으로 합산한다.
 * 점검한 구독은 구독 종료 상태여도 맨 위에 포함하고, 나머지는 월 환산 금액이 큰 순(같으면 서비스명 순)이다. 5개를 넘으면 상위 4개 + '그 외 n'.
 */
public record CategorySpendingResponse(
    Category category,
    long monthlyTotal,
    int subscriptionCount,
    List<CategorySpendingItemResponse> items,
    int otherCount
) {

    private static final int MAX_FULL_ITEMS = 5;
    private static final int TRUNCATED_ITEMS = 4;

    public static CategorySpendingResponse of(Category category, Subscription checkedSubscription,
                                              List<Subscription> subscriptions) {
        List<Subscription> others = subscriptions.stream()
            .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(subscription -> subscription.getCategory() == category)
            .filter(subscription -> !subscription.getId().equals(checkedSubscription.getId()))
            .sorted(Comparator.comparingLong(Subscription::monthlyAmount).reversed()
                .thenComparing(Subscription::getName))
            .toList();
        List<Subscription> ordered = Stream.concat(Stream.of(checkedSubscription), others.stream()).toList();

        long monthlyTotal = ordered.stream()
            .mapToLong(Subscription::monthlyAmount)
            .sum();
        int itemCount = ordered.size() > MAX_FULL_ITEMS ? TRUNCATED_ITEMS : ordered.size();
        List<CategorySpendingItemResponse> items = ordered.stream()
            .limit(itemCount)
            .map(CategorySpendingItemResponse::from)
            .toList();
        return new CategorySpendingResponse(category, monthlyTotal, ordered.size(), items,
            ordered.size() - itemCount);
    }
}
