package com.subscribers.gudogging.domain.home.application.dto.response;

import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.List;

/**
 * 홈 (H-01, H-02, H-03).
 */
public record HomeResponse(
    HomeSummaryResponse summary,
    List<HomeUpcomingPaymentResponse> upcomingPayments,
    HomeUsageResponse usage,
    boolean hasUnreadNotification
) {

    public static HomeResponse of(HomeSummaryResponse summary, List<HomeUpcomingPaymentResponse> upcomingPayments,
                                  HomeUsageResponse usage, boolean hasUnreadNotification) {
        return new HomeResponse(summary, upcomingPayments, usage, hasUnreadNotification);
    }

    /**
     * 사전 등록 서비스 로고. 직접 입력 구독은 null.
     */
    static String logoUrlOf(Subscription subscription) {
        return subscription.getServiceCatalog() == null ? null : subscription.getServiceCatalog().getLogoUrl();
    }
}
