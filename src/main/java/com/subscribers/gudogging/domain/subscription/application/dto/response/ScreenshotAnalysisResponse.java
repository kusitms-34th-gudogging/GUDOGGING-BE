package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.ScreenshotCandidate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 스크린샷 분석 결과 (A-09). candidates가 비어 있으면 클라이언트가 분석 실패 화면(A-08e)으로 이동한다.
 */
public record ScreenshotAnalysisResponse(
    List<ScreenshotCandidateResponse> candidates
) {

    /**
     * candidates 순서(화면 순서)를 유지한다. servicesById에는 모든 후보의 서비스가 들어 있어야 한다.
     */
    public static ScreenshotAnalysisResponse from(List<ScreenshotCandidate> candidates,
                                                  Map<UUID, ServiceCatalog> servicesById) {
        return new ScreenshotAnalysisResponse(candidates.stream()
            .map(candidate -> ScreenshotCandidateResponse.from(candidate, servicesById.get(candidate.serviceId())))
            .toList());
    }
}
