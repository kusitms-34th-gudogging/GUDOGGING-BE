package com.subscribers.gudogging.domain.catalog.application.usecase;

import com.subscribers.gudogging.domain.catalog.application.dto.request.ServiceSeedRow;
import com.subscribers.gudogging.domain.catalog.application.dto.response.ServiceListResponse;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.search.ServiceSearchKeyword;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogCreateService;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogUseCase {

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final ServiceCatalogGetService serviceCatalogGetService;
    private final ServiceCatalogCreateService serviceCatalogCreateService;

    /**
     * 회원이 이미 구독 중(ACTIVE)인 서비스를 제외한 노출 서비스 중에서 검색어로 매칭·정렬한다. 검색어가 비어 있으면 전체 목록을 반환한다.
     */
    public ServiceListResponse getServices(UUID userId, String keyword) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        Set<UUID> registeredServiceIds = subscriptionGetService.findActiveServiceIds(user);
        List<ServiceCatalog> candidates = serviceCatalogGetService.findActiveServices().stream()
            .filter(service -> !registeredServiceIds.contains(service.getId()))
            .toList();

        return ServiceListResponse.from(ServiceSearchKeyword.from(keyword).search(candidates));
    }

    /**
     * 시드 CSV를 이름 기준으로 services에 동기화한다. 이름이 없으면 추가하고, 있으면 CSV 컬럼(category, logo_url, cancel_url, sort_order)만 갱신한다.
     * CSV에 없는 컬럼과 CSV에 없는 서비스는 건드리지 않는다.
     */
    @Transactional
    public void seedServices(List<ServiceSeedRow> rows) {
        Map<String, ServiceCatalog> servicesByName = new HashMap<>();
        for (ServiceCatalog service : serviceCatalogGetService.findAllServices()) {
            if (servicesByName.putIfAbsent(service.getName(), service) != null) {
                log.warn("services에 같은 이름의 서비스가 여러 개 있어 첫 번째 행만 동기화합니다. name={}", service.getName());
            }
        }

        List<ServiceCatalog> newServices = new ArrayList<>();
        int updatedCount = 0;
        for (ServiceSeedRow row : rows) {
            ServiceCatalog existing = servicesByName.get(row.name());
            if (existing == null) {
                ServiceCatalog created = ServiceCatalog.create(row.name(), null, null, row.category(), row.logoUrl(), null,
                    row.cancelUrl(), row.sortOrder());
                newServices.add(created);
                servicesByName.put(row.name(), created);
            } else {
                existing.updateFromSeed(row.category(), row.logoUrl(), row.cancelUrl(), row.sortOrder());
                updatedCount++;
            }
        }

        if (!newServices.isEmpty()) {
            serviceCatalogCreateService.saveAllServices(newServices);
        }
        log.info("서비스 시드 동기화: 추가 {}건, 갱신 {}건", newServices.size(), updatedCount);
    }
}
