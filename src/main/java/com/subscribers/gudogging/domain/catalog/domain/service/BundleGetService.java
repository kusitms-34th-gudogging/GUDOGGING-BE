package com.subscribers.gudogging.domain.catalog.domain.service;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleRecommendType;
import com.subscribers.gudogging.domain.catalog.domain.repository.BundleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BundleGetService {

    private final BundleRepository bundleRepository;

    /**
     * 이 요금제가 포함된 번들 (제외 유형 빼고) — 점검 결과 절약 제안 후보.
     */
    public List<Bundle> findBundlesIncluding(String serviceName) {
        return bundleRepository.findAll().stream()
            .filter(bundle -> bundle.recommendType() != BundleRecommendType.EXCLUDED)
            .filter(bundle -> bundle.includes(serviceName))
            .toList();
    }
}
