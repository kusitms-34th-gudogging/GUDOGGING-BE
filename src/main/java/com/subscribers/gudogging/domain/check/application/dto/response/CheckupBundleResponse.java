package com.subscribers.gudogging.domain.check.application.dto.response;

import java.util.List;

/**
 * 절약 제안 번들 (K-80 '같은 {서비스명}, 더 싸게'). 번들 데이터 기준이 정해지면 채운다 — 현재는 결과 조회에서 빈 배열로 내려준다.
 *
 * @param priceType FIXED(월 N원, 금액 확정) / MAX(최대 N원, 카드 할인 등)
 */
public record CheckupBundleResponse(
    String name,
    List<String> includes,
    List<String> tags,
    long monthlyPrice,
    long monthlySaving,
    String condition,
    String providerType,
    String priceType
) {
}
