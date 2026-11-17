package com.subscribers.gudogging.domain.catalog.application.usecase;

import com.subscribers.gudogging.domain.catalog.application.dto.response.ServiceResponse;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.search.ServiceSearchKeyword;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogUseCase {

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final ServiceCatalogGetService serviceCatalogGetService;

    /**
     * 회원이 이미 구독 중(ACTIVE)인 서비스를 제외한 노출 서비스 중에서 검색어로 매칭·정렬한다. 검색어가 비어 있으면 전체 목록을 반환한다.
     */
    public List<ServiceResponse> getServices(UUID userId, String keyword) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        Set<UUID> registeredServiceIds = subscriptionGetService.findActiveServiceIds(user);
        List<ServiceCatalog> candidates = serviceCatalogGetService.findActiveServices().stream()
            .filter(service -> !registeredServiceIds.contains(service.getId()))
            .toList();

        return ServiceSearchKeyword.from(keyword).search(candidates).stream()
            .map(ServiceResponse::from)
            .toList();
    }
}
