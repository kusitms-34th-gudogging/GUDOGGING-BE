package com.subscribers.gudogging.domain.subscription.application.usecase;

import com.subscribers.gudogging.domain.catalog.application.exception.ServiceNotFoundException;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import com.subscribers.gudogging.domain.check.domain.result.CheckAverage;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCancelReasonGetService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.application.dto.request.ChangeSubscriptionSteadyRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.DeleteSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.ReactivateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SaveSubscriptionDraftRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionDraftItemRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionItemRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionSortType;
import com.subscribers.gudogging.domain.subscription.application.dto.request.UpdateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.CancelGuideResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionBillingResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCancelResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCreationResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDeletionResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDetailResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDraftResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDraftSaveResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionListResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionReactivationResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionSteadyResponse;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateServiceInRequestException;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateSubscriptionException;
import com.subscribers.gudogging.domain.subscription.application.exception.InvalidSubscriptionSortException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionAlreadyEndedException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotActiveException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotEndedException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotFoundException;
import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraftItem;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryCreateService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionCreateService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionDraftService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
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
    private final SubscriptionDraftService subscriptionDraftService;

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
        LocalDate today = LocalDate.now();
        Optional<SubscriptionCheck> latestCheck = subscriptionCheckGetService.findLatestCheck(subscription);
        List<SubscriptionCheckCancelReason> latestCancelReasons = latestCheck
            .map(check -> subscriptionCheckCancelReasonGetService.findCancelReasonsByChecks(List.of(check)))
            .orElse(List.of());
        List<SubscriptionCheck> recentChecks = subscriptionCheckGetService.findChecksCreatedSince(subscription,
            CheckAverage.periodFromOf(today).atStartOfDay());
        return SubscriptionDetailResponse.of(subscription, billingHistories, latestCheck, latestCancelReasons,
            recentChecks, today, baseDate);
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
     * 구독 종료 구독을 같은 행 그대로 구독 중으로 되살린다 (M-05). 요청한 구독 정보로 덮어쓰고(서비스명은 직접 입력 서비스만), 다음 결제일을 결제 기준일로, 시작일을 오늘로 다시 잡는다. 점검
     * 기록은 유지한다. 사전 등록 서비스는 같은 서비스를 이미 구독 중이면 재등록할 수 없다.
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
     * 구독 상세(M-03)의 '꾸준히 쓰는 구독' 토글 값을 저장한다. 구독 중인 구독만 바꿀 수 있다. 점검 알림 · 홈 점검 유도 제외는 발송 · 조회 시점에 이 값으로 판단한다.
     */
    @Transactional
    public SubscriptionSteadyResponse changeSubscriptionSteady(UUID userId, UUID subscriptionId,
                                                               ChangeSubscriptionSteadyRequest request) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionNotActiveException();
        }

        subscription.changeSteadyUse(request.isSteady());
        return SubscriptionSteadyResponse.from(subscription);
    }

    /**
     * 사전 등록 서비스와 직접 입력 서비스를 한 번에 등록한다(registration_source = 요청 source). 하나라도 등록할 수 없으면 아무것도 저장하지 않는다. 임시저장은 등록이 커밋된 뒤
     * 지우고, 삭제에 실패해도 등록 결과는 그대로 둔다.
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

        List<Subscription> createdSubscriptions = subscriptionCreateService.createSubscriptions(subscriptions);
        deleteDraftAfterCommit(userId);
        return SubscriptionCreationResponse.from(createdSubscriptions);
    }

    /**
     * 직접 등록 흐름의 A-04 목록과 마지막 화면을 회원당 1건으로 덮어써 임시저장한다 (Redis, DB 쓰기 없음).
     */
    @Transactional
    public SubscriptionDraftSaveResponse saveSubscriptionDraft(UUID userId, SaveSubscriptionDraftRequest request) {
        userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        List<UUID> serviceIds = request.items().stream()
            .map(SubscriptionDraftItemRequest::serviceId)
            .filter(Objects::nonNull)
            .toList();
        if (new HashSet<>(serviceIds).size() != serviceIds.size()) {
            throw new DuplicateServiceInRequestException();
        }

        List<SubscriptionDraftItem> items = request.items().stream()
            .map(this::toDraftItem)
            .toList();
        SubscriptionDraft subscriptionDraft = SubscriptionDraft.of(userId, request.lastStep(),
            request.editingItemIndex(), items, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        subscriptionDraftService.saveDraft(subscriptionDraft);

        return SubscriptionDraftSaveResponse.from(subscriptionDraft);
    }

    /**
     * 회원의 구독 등록 임시저장을 조회한다 (Redis). 없으면 null. 사전 등록 서비스 항목에는 조회 시점의 서비스 로고를 함께 담는다.
     */
    public SubscriptionDraftResponse getSubscriptionDraft(UUID userId) {
        userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        return subscriptionDraftService.findDraft(userId)
            .map(subscriptionDraft -> SubscriptionDraftResponse.of(subscriptionDraft,
                findLogoUrlsByServiceId(subscriptionDraft)))
            .orElse(null);
    }

    /**
     * 임시저장한 사전 등록 서비스의 로고를 조회한다. 비활성 · 삭제 서비스와 로고가 없는 서비스는 넣지 않는다 (응답 logoUrl = null). 구독 등록이 DB에 커밋된 뒤 임시저장(Redis)을
     * 지운다. 커밋 전에 지우면 커밋이 실패했을 때 임시저장만 사라지고, Redis 예외가 트랜잭션 안에서 터지면 등록까지 롤백되기 때문이다. 삭제 실패는 로그만 남긴다 (남은 임시저장은 다음 임시저장 ·
     * 삭제 요청 때 정리됨).
     */
    private void deleteDraftAfterCommit(UUID userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    subscriptionDraftService.deleteDraft(userId);
                } catch (RuntimeException e) {
                    log.warn("구독 등록 후 임시저장 삭제 실패: userId={}, error={}", userId, e.getClass().getSimpleName());
                }
            }
        });
    }

    private Map<UUID, String> findLogoUrlsByServiceId(SubscriptionDraft subscriptionDraft) {
        Set<UUID> serviceIds = subscriptionDraft.items().stream()
            .map(SubscriptionDraftItem::serviceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return serviceCatalogGetService.findActiveServicesByIds(serviceIds).stream()
            .filter(service -> service.getLogoUrl() != null)
            .collect(Collectors.toMap(ServiceCatalog::getId, ServiceCatalog::getLogoUrl));
    }

    /**
     * 회원의 구독 등록 임시저장을 삭제한다 (Redis). 임시저장이 없어도 성공한다.
     */
    @Transactional
    public void deleteSubscriptionDraft(UUID userId) {
        userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        subscriptionDraftService.deleteDraft(userId);
    }

    private SubscriptionDraftItem toDraftItem(SubscriptionDraftItemRequest item) {
        return SubscriptionDraftItem.of(item.serviceId(), item.serviceName().strip(), item.category(), item.amount(),
            item.billingCycleValue(), item.billingCycleUnit(), item.nextBillingDate(), item.paymentMethod());
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
