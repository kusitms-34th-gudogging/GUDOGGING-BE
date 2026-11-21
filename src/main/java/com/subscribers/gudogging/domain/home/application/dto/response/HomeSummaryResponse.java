package com.subscribers.gudogging.domain.home.application.dto.response;

import java.util.List;

/**
 * 내 구독 정보 (H-02). 구독 중 구독 기준 — 결제 예정액은 오늘~이번 달 말일, 연 금액은 올해 1~12월 예상 결제 합계.
 */
public record HomeSummaryResponse(
    int expectedAmount,
    int yearlyAmount,
    int subscriptionCount,
    List<String> logos
) {

    public static HomeSummaryResponse of(int expectedAmount, int yearlyAmount, int subscriptionCount,
                                         List<String> logos) {
        return new HomeSummaryResponse(expectedAmount, yearlyAmount, subscriptionCount, logos);
    }
}
