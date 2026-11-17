package com.subscribers.gudogging.domain.subscription.application.usecase;

import com.subscribers.gudogging.domain.catalog.application.exception.ServiceNotFoundException;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionItemRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionResponse;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateServiceInRequestException;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateSubscriptionException;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionCreateService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionUseCase {

    private final UserGetService userGetService;
    private final ServiceCatalogGetService serviceCatalogGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionCreateService subscriptionCreateService;

    /**
     * 사전 등록 서비스와 직접 입력 서비스를 한 번에 등록한다(MANUAL). 하나라도 등록할 수 없으면 아무것도 저장하지 않는다.
     */
    @Transactional
    public List<SubscriptionResponse> createSubscriptions(UUID userId, CreateSubscriptionsRequest request) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        List<UUID> serviceIds = request.subscriptions().stream()
            .map(SubscriptionItemRequest::serviceId)
            .filter(Objects::nonNull)
            .toList();
        Set<UUID> uniqueServiceIds = new HashSet<>(serviceIds);
        if (uniqueServiceIds.size() != serviceIds.size()) {
            throw new DuplicateServiceInRequestException();
        }

        Map<UUID, ServiceCatalog> servicesById = findServicesById(uniqueServiceIds);

        Set<UUID> activeServiceIds = subscriptionGetService.findActiveServiceIds(user);
        if (uniqueServiceIds.stream().anyMatch(activeServiceIds::contains)) {
            throw new DuplicateSubscriptionException();
        }

        List<Subscription> subscriptions = request.subscriptions().stream()
            .map(item -> toSubscription(user, item, servicesById))
            .toList();

        return subscriptionCreateService.createSubscriptions(subscriptions).stream()
            .map(SubscriptionResponse::from)
            .toList();
    }

    private Map<UUID, ServiceCatalog> findServicesById(Set<UUID> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        List<ServiceCatalog> services = serviceCatalogGetService.findActiveServicesByIds(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new ServiceNotFoundException();
        }
        return services.stream()
            .collect(Collectors.toMap(ServiceCatalog::getId, Function.identity()));
    }

    private Subscription toSubscription(User user, SubscriptionItemRequest item,
                                        Map<UUID, ServiceCatalog> servicesById) {
        if (item.serviceId() != null) {
            return Subscription.createFromCatalog(user, servicesById.get(item.serviceId()), item.category(),
                item.amount(), item.billingCycleValue(), item.billingCycleUnit(), item.nextBillingOn(),
                item.paymentMethod(), RegistrationSource.MANUAL);
        }
        return Subscription.createCustom(user, item.name().strip(), item.category(),
            item.amount(), item.billingCycleValue(), item.billingCycleUnit(), item.nextBillingOn(),
            item.paymentMethod(), RegistrationSource.MANUAL);
    }
}
