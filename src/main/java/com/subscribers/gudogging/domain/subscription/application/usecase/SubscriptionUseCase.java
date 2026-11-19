package com.subscribers.gudogging.domain.subscription.application.usecase;

import com.subscribers.gudogging.domain.catalog.application.exception.ServiceNotFoundException;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCancelReasonGetService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.DeleteSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.ReactivateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionItemRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionSortType;
import com.subscribers.gudogging.domain.subscription.application.dto.request.UpdateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.CancelGuideResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionBillingResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCancelResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCreationResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDeletionResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDetailResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionListResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionReactivationResponse;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateServiceInRequestException;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateSubscriptionException;
import com.subscribers.gudogging.domain.subscription.application.exception.InvalidSubscriptionSortException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionAlreadyEndedException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotActiveException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotEndedException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotFoundException;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryCreateService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionCreateService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.entity.BaseEntity;
import java.time.LocalDate;
import java.util.Comparator;
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
    private final SubscriptionBillingHistoryGetService subscriptionBillingHistoryGetService;
    private final SubscriptionBillingHistoryCreateService subscriptionBillingHistoryCreateService;
    private final SubscriptionCheckGetService subscriptionCheckGetService;
    private final SubscriptionCheckCancelReasonGetService subscriptionCheckCancelReasonGetService;
    private final NotificationGetService notificationGetService;

    /**
     * 선택한 탭(status)의 구독을 정렬 기준(sort)대로 조회하고, 두 탭 개수와 탭의 월 환산 합계를 함께 반환한다.
     */
    public SubscriptionListResponse getSubscriptions(UUID userId, SubscriptionStatus status,
                                                     SubscriptionSortType sort) {
        SubscriptionStatus tab = status != null ? status : SubscriptionStatus.ACTIVE;
        SubscriptionSortType sortType = sort != null ? sort : defaultSortOf(tab);
        validateSort(tab, sortType);

        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        LocalDate today = LocalDate.now();

        Map<SubscriptionStatus, List<Subscription>> subscriptionsByStatus = subscriptionGetService
            .findSubscriptions(user).stream()
            .collect(Collectors.groupingBy(Subscription::getStatus));
        List<Subscription> activeSubscriptions = subscriptionsByStatus.getOrDefault(SubscriptionStatus.ACTIVE,
            List.of());
        List<Subscription> endedSubscriptions = subscriptionsByStatus.getOrDefault(SubscriptionStatus.ENDED, List.of());

        List<Subscription> tabSubscriptions = subscriptionsByStatus.getOrDefault(tab, List.of()).stream()
            .sorted(comparatorOf(tab, sortType, today))
            .toList();
        // 결제 조건 이력은 구독 종료 항목의 총 지출에만 쓰므로 그 탭에서만 한 번에 조회한다.
        Map<UUID, List<SubscriptionBillingHistory>> billingHistoriesBySubscriptionId =
            tab == SubscriptionStatus.ENDED ? findBillingHistoriesBySubscriptionId(tabSubscriptions) : Map.of();
        return SubscriptionListResponse.of(tabSubscriptions, billingHistoriesBySubscriptionId,
            activeSubscriptions.size(), endedSubscriptions.size(), today);
    }

    /**
     * 본인 구독 1건의 기본 정보와 결제 계산 값을 조회한다. baseDate(캘린더 C-03)가 있으면 그 날짜 기준으로 상태 · 다음 결제일을 계산한다.
     */
    public SubscriptionDetailResponse getSubscription(UUID userId, UUID subscriptionId, LocalDate baseDate) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        List<SubscriptionBillingHistory> billingHistories = subscriptionBillingHistoryGetService
            .findBillingHistoriesBySubscriptions(List.of(subscription));
        return SubscriptionDetailResponse.of(subscription, billingHistories, LocalDate.now(), baseDate);
    }

    /**
     * 본인 구독의 결제 경로(paymentMethod)와 서비스 해지 링크 유무에 따른 해지 안내를 조회한다. 구독 상태(ACTIVE/ENDED)는 검사하지 않는다.
     */
    public CancelGuideResponse getCancelGuide(UUID userId, UUID subscriptionId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        return CancelGuideResponse.from(subscription);
    }

    /**
     * 해지 완료 확인 모달(X-06)에서 구독 중인 구독을 구독 종료로 바꾸고 해지일을 오늘로 저장한다.
     */
    @Transactional
    public SubscriptionCancelResponse cancelSubscription(UUID userId, UUID subscriptionId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        if (subscription.getStatus() == SubscriptionStatus.ENDED) {
            throw new SubscriptionAlreadyEndedException();
        }

        subscription.cancel(LocalDate.now());
        return SubscriptionCancelResponse.from(subscription);
    }

    /**
     * 구독 종료 구독을 같은 행 그대로 구독 중으로 되살린다 (M-05). 요청한 구독 정보로 덮어쓰고(서비스명은 직접 입력 서비스만), 다음 결제일을 결제 기준일로,
     * 시작일을 오늘로 다시 잡는다. 점검 기록은 유지한다. 사전 등록 서비스는 같은 서비스를 이미 구독 중이면 재등록할 수 없다.
     */
    @Transactional
    public SubscriptionReactivationResponse reactivateSubscription(UUID userId, UUID subscriptionId,
                                                                   ReactivateSubscriptionRequest request) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        if (subscription.getStatus() != SubscriptionStatus.ENDED) {
            throw new SubscriptionNotEndedException();
        }
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        if (serviceCatalog != null
            && subscriptionGetService.findActiveServiceIds(user).contains(serviceCatalog.getId())) {
            throw new DuplicateSubscriptionException();
        }

        subscription.reactivate(request.category(), request.amount(), request.billingCycleValue(),
            request.billingCycleUnit(), request.nextBillingDate(), request.paymentMethod(), LocalDate.now());
        if (serviceCatalog == null && request.serviceName() != null) {
            subscription.updateCustomName(request.serviceName().strip());
        }
        return SubscriptionReactivationResponse.from(subscription);
    }

    /**
     * 사전 등록 서비스와 직접 입력 서비스를 한 번에 등록한다(registration_source = 요청 source). 하나라도 등록할 수 없으면 아무것도 저장하지 않는다.
     */
    @Transactional
    public SubscriptionCreationResponse createSubscriptions(UUID userId, CreateSubscriptionsRequest request) {
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
            .map(item -> toSubscription(user, item, request.source(), servicesById))
            .toList();

        return SubscriptionCreationResponse.from(subscriptionCreateService.createSubscriptions(subscriptions));
    }

    /**
     * 구독 중인 구독의 결제 정보를 수정한다. 서비스명은 직접 입력 서비스만 바꾸고, 사전 등록 서비스는 요청 serviceName을 무시한다. 다음 결제일은 결제 기준일(billing_anchor_on)로
     * 저장하고, 수정 전 조건으로 결제한 적이 있으면 그 조건을 결제 조건 이력으로 남겨 지난 결제를 유지한다 (ERD 4.4.1, 6.2).
     */
    @Transactional
    public SubscriptionBillingResponse updateSubscription(UUID userId, UUID subscriptionId,
                                                          UpdateSubscriptionRequest request) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionNotActiveException();
        }

        LocalDate today = LocalDate.now();
        subscription.updatePaymentInfo(request.category(), request.amount(), request.billingCycleValue(),
                request.billingCycleUnit(), request.nextBillingDate(), request.paymentMethod(), today)
            .ifPresent(subscriptionBillingHistoryCreateService::createBillingHistory);
        if (subscription.getServiceCatalog() == null && request.serviceName() != null) {
            subscription.updateCustomName(request.serviceName().strip());
        }

        return SubscriptionBillingResponse.from(subscription, today);
    }

    /**
     * 선택한 구독과 그 점검 기록 · 해지 이유 · 알림 · 결제 조건 이력을 Soft Delete 한다 (ERD 8장). 하나라도 삭제할 수 없으면 아무것도 삭제하지 않는다.
     */
    @Transactional
    public SubscriptionDeletionResponse deleteSubscriptions(UUID userId, DeleteSubscriptionsRequest request) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        Set<UUID> subscriptionIds = Set.copyOf(request.subscriptionIds());
        List<Subscription> subscriptions = subscriptionGetService.findSubscriptionsByIds(user, subscriptionIds);
        if (subscriptions.size() != subscriptionIds.size()) {
            throw new SubscriptionNotFoundException();
        }

        List<SubscriptionCheck> subscriptionChecks = subscriptionCheckGetService.findChecksBySubscriptions(
            subscriptions);
        if (!subscriptionChecks.isEmpty()) {
            subscriptionCheckCancelReasonGetService.findCancelReasonsByChecks(subscriptionChecks)
                .forEach(BaseEntity::softDelete);
        }
        subscriptionChecks.forEach(BaseEntity::softDelete);
        notificationGetService.findNotificationsBySubscriptions(subscriptions)
            .forEach(BaseEntity::softDelete);
        subscriptionBillingHistoryGetService.findBillingHistoriesBySubscriptions(subscriptions)
            .forEach(BaseEntity::softDelete);
        subscriptions.forEach(BaseEntity::softDelete);

        return SubscriptionDeletionResponse.from(subscriptions);
    }

    private SubscriptionSortType defaultSortOf(SubscriptionStatus tab) {
        return tab == SubscriptionStatus.ACTIVE ? SubscriptionSortType.NEXT_BILLING_DATE
            : SubscriptionSortType.CANCELED_AT;
    }

    private void validateSort(SubscriptionStatus tab, SubscriptionSortType sortType) {
        boolean activeWithCanceledAt = tab == SubscriptionStatus.ACTIVE && sortType == SubscriptionSortType.CANCELED_AT;
        boolean endedWithNextBillingDate =
            tab == SubscriptionStatus.ENDED && sortType == SubscriptionSortType.NEXT_BILLING_DATE;
        if (activeWithCanceledAt || endedWithNextBillingDate) {
            throw new InvalidSubscriptionSortException();
        }
    }

    private Comparator<Subscription> comparatorOf(SubscriptionStatus tab, SubscriptionSortType sortType,
                                                  LocalDate today) {
        Comparator<Subscription> byNextBillingDate = Comparator.comparing(
            subscription -> subscription.nextBillingOn(today));
        Comparator<Subscription> byAmountDesc = Comparator.comparingInt(Subscription::getAmount).reversed();
        Comparator<Subscription> byCanceledOnDesc = Comparator.comparing(Subscription::getCanceledOn).reversed();
        Comparator<Subscription> byMonthlyAmountDesc = Comparator.comparingLong(Subscription::monthlyAmount).reversed();

        return switch (sortType) {
            case NEXT_BILLING_DATE -> byNextBillingDate.thenComparing(byAmountDesc);
            case CANCELED_AT -> byCanceledOnDesc.thenComparing(byMonthlyAmountDesc);
            case AMOUNT_DESC -> tab == SubscriptionStatus.ACTIVE
                ? byAmountDesc.thenComparing(byNextBillingDate)
                : byMonthlyAmountDesc.thenComparing(byCanceledOnDesc);
        };
    }

    private Map<UUID, List<SubscriptionBillingHistory>> findBillingHistoriesBySubscriptionId(
        List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return Map.of();
        }
        return subscriptionBillingHistoryGetService.findBillingHistoriesBySubscriptions(subscriptions).stream()
            .collect(Collectors.groupingBy(billingHistory -> billingHistory.getSubscription().getId()));
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

    private Subscription toSubscription(User user, SubscriptionItemRequest item, RegistrationSource source,
                                        Map<UUID, ServiceCatalog> servicesById) {
        if (item.serviceId() != null) {
            return Subscription.createFromCatalog(user, servicesById.get(item.serviceId()), item.category(),
                item.amount(), item.billingCycleValue(), item.billingCycleUnit(), item.nextBillingDate(),
                item.paymentMethod(), source);
        }
        return Subscription.createCustom(user, item.serviceName().strip(), item.category(),
            item.amount(), item.billingCycleValue(), item.billingCycleUnit(), item.nextBillingDate(),
            item.paymentMethod(), source);
    }
}
