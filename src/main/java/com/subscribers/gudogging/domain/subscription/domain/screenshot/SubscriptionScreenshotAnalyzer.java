package com.subscribers.gudogging.domain.subscription.domain.screenshot;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionScreenshotAnalyzer {

    /**
     * 스크린샷을 OCR 서버로 분석해 결제 예정 구독 후보를 화면 순서대로 돌려준다. images 순서는 스크롤 순서로 그대로 전달한다.
     * referenceDate는 연도가 생략된 날짜('11월 13일')의 연도를 채우는 기준일이다. 실패하면 ScreenshotAnalysisFailedException.
     */
    List<ScreenshotCandidate> analyzeScreenshots(List<ScreenshotImage> images, LocalDate referenceDate);
}
