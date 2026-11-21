package com.subscribers.gudogging.domain.home.application.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 잘 안 쓰는 구독 (H-02, H-03). monthlyAmount · yearlyAmount는 미리보기가 아니라 대상 구독 전체 기준.
 */
public record HomeUsageResponse(
    boolean hasCheckup,
    UUID nudgeSubscriptionId,
    int unusedCount,
    int monthlyAmount,
    int yearlyAmount,
    List<HomeUsageItemResponse> items
) {

    public static HomeUsageResponse of(boolean hasCheckup, UUID nudgeSubscriptionId, int unusedCount,
                                       int monthlyAmount, int yearlyAmount, List<HomeUsageItemResponse> items) {
        return new HomeUsageResponse(hasCheckup, nudgeSubscriptionId, unusedCount, monthlyAmount, yearlyAmount,
            items);
    }
}
