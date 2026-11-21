package com.subscribers.gudogging.domain.check.domain.bundle;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;

/**
 * 번들 추천 1건. saving · rate는 통신 요금제(BENEFIT)면 null, price는 카드(MAX)면 null.
 *
 * @param saving 큰 숫자 — 확정 절약 '월 N원' · 카드 '최대 N원'
 * @param rate   절약 막대 % (0~100)
 * @param price  아래 문구의 상품 월 가격 (서비스별 적용가가 있으면 그 가격)
 */
public record BundleRecommendation(Bundle bundle, Long saving, Integer rate, Integer price) {
}
