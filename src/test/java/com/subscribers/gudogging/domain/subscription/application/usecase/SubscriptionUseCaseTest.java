package com.subscribers.gudogging.domain.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.catalog.application.exception.CatalogErrorCode;
import com.subscribers.gudogging.domain.catalog.application.exception.ServiceNotFoundException;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.check.domain.entity.CancelReason;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCancelReasonGetService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.notification.domain.entity.Notification;
import com.subscribers.gudogging.domain.notification.domain.entity.NotificationType;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.DeleteSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionItemRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionSortType;
import com.subscribers.gudogging.domain.subscription.application.dto.request.UpdateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionBillingResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCreationResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDeletionResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDetailResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionListResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionSummaryResponse;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateServiceInRequestException;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateSubscriptionException;
import com.subscribers.gudogging.domain.subscription.application.exception.InvalidSubscriptionSortException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionErrorCode;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotActiveException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotFoundException;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionCreateService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionUseCase")
class SubscriptionUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @Mock
    private ServiceCatalogGetService serviceCatalogGetService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private SubscriptionCreateService subscriptionCreateService;

    @Mock
    private SubscriptionCheckGetService subscriptionCheckGetService;

    @Mock
    private SubscriptionCheckCancelReasonGetService subscriptionCheckCancelReasonGetService;

    @Mock
    private NotificationGetService notificationGetService;

    @InjectMocks
    private SubscriptionUseCase subscriptionUseCase;

    @Captor
    private ArgumentCaptor<List<Subscription>> subscriptionsCaptor;

    private static ServiceCatalog service(String name, Category category, String logoUrl) {
        ServiceCatalog service = ServiceCatalog.create(name, null, null, category, logoUrl, null, 1);
        // id는 JPA 저장 시 생성되므로, 요청 serviceId와의 매칭 검증을 위해 테스트에서만 지정한다.
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        return service;
    }

    @Nested
    @DisplayName("createSubscriptions")
    class CreateSubscriptions {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate nextBillingDate = LocalDate.now().plusDays(7);

        private final ServiceCatalog netflix = service("넷플릭스", Category.VIDEO_OTT,
            "https://logo.example.com/netflix.png");
        private final ServiceCatalog melon = service("멜론", Category.MUSIC, "https://logo.example.com/melon.png");

        private SubscriptionItemRequest catalogItem(ServiceCatalog service, String serviceName) {
            return new SubscriptionItemRequest(service.getId(), serviceName, service.getCategory(), 13500, 1,
                BillingCycleUnit.MONTH, nextBillingDate, PaymentMethod.WEB_DIRECT);
        }

        private SubscriptionItemRequest customItem(String serviceName) {
            return new SubscriptionItemRequest(null, serviceName, Category.ETC, 50000, 1,
                BillingCycleUnit.MONTH, nextBillingDate, PaymentMethod.WEB_DIRECT);
        }

        private CreateSubscriptionsRequest request(SubscriptionItemRequest... items) {
            return request(RegistrationSource.MANUAL, items);
        }

        private CreateSubscriptionsRequest request(RegistrationSource source, SubscriptionItemRequest... items) {
            return new CreateSubscriptionsRequest(source, List.of(items));
        }

        private void givenUser() {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
        }

        private void givenSaveReturnsArgument() {
            given(subscriptionCreateService.createSubscriptions(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("사전 등록 서비스와 직접 입력 서비스를 함께 등록하면 각각 createFromCatalog/createCustom으로 만들어 한 번에 저장하고, 등록 개수와 함께 요청 순서대로 응답한다")
        void createsCatalogAndCustomSubscriptionsInRequestOrder() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId(), melon.getId())))
                .willReturn(List.of(melon, netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            SubscriptionCreationResponse response = subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null), customItem("동네 헬스장"), catalogItem(melon, null)));

            // then
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            List<Subscription> saved = subscriptionsCaptor.getValue();
            assertThat(saved).hasSize(3);
            assertThat(saved).extracting(Subscription::getServiceCatalog).containsExactly(netflix, null, melon);
            assertThat(saved).extracting(Subscription::getUser).containsOnly(user);

            assertThat(response.createdCount()).isEqualTo(3);
            assertThat(response.subscriptions()).extracting(SubscriptionResponse::serviceName)
                .containsExactly("넷플릭스", "동네 헬스장", "멜론");
            assertThat(response.subscriptions()).extracting(SubscriptionResponse::logoUrl)
                .containsExactly(netflix.getLogoUrl(), null, melon.getLogoUrl());
        }

        @Test
        @DisplayName("사전 등록 서비스는 serviceName을 services.name 스냅샷으로, logoUrl을 서비스 로고로 응답하며 요청의 serviceName은 무시한다")
        void usesCatalogNameAndLogoForCatalogService() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(
                List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            SubscriptionCreationResponse creation = subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, "내 넷플릭스")));

            // then
            SubscriptionResponse response = creation.subscriptions().getFirst();
            assertThat(response.serviceName()).isEqualTo("넷플릭스");
            assertThat(response.logoUrl()).isEqualTo("https://logo.example.com/netflix.png");
            assertThat(response.amount()).isEqualTo(13500);
            assertThat(response.billingCycleValue()).isEqualTo(1);
            assertThat(response.billingCycleUnit()).isEqualTo(BillingCycleUnit.MONTH);
            assertThat(response.nextBillingDate()).isEqualTo(nextBillingDate);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.WEB_DIRECT);
        }

        @Test
        @DisplayName("저장된 구독은 status = ACTIVE, billingAnchorOn = 요청 nextBillingDate이다")
        void savesActiveManualSubscriptionWithBillingAnchor() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(
                List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            subscriptionUseCase.createSubscriptions(userId, request(catalogItem(netflix, null), customItem("동네 헬스장")));

            // then
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            assertThat(subscriptionsCaptor.getValue()).allSatisfy(subscription -> {
                assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                assertThat(subscription.getBillingAnchorOn()).isEqualTo(nextBillingDate);
                assertThat(subscription.getCanceledOn()).isNull();
                assertThat(subscription.isSteadyUse()).isFalse();
            });
        }

        @Test
        @DisplayName("registrationSource는 요청 source로 저장한다 (예: SCREENSHOT)")
        void savesRegistrationSourceFromRequest() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(
                List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            subscriptionUseCase.createSubscriptions(userId,
                request(RegistrationSource.SCREENSHOT, catalogItem(netflix, null), customItem("동네 헬스장")));

            // then
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            assertThat(subscriptionsCaptor.getValue()).extracting(Subscription::getRegistrationSource)
                .containsOnly(RegistrationSource.SCREENSHOT);
        }

        @Test
        @DisplayName("직접 입력 서비스만 있으면 서비스 조회 없이 저장하고 logoUrl = null, 이름은 앞뒤 공백을 제거해 응답한다")
        void createsCustomOnlyWithoutServiceLookup() {
            // given
            givenUser();
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            SubscriptionCreationResponse creation = subscriptionUseCase.createSubscriptions(userId,
                request(customItem("  동네 헬스장  ")));

            // then
            verifyNoInteractions(serviceCatalogGetService);
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            Subscription saved = subscriptionsCaptor.getValue().getFirst();
            assertThat(saved.getServiceCatalog()).isNull();
            assertThat(saved.getCategory()).isEqualTo(Category.ETC);
            SubscriptionResponse response = creation.subscriptions().getFirst();
            assertThat(response.logoUrl()).isNull();
            assertThat(response.serviceName()).isEqualTo("동네 헬스장");
        }

        @Test
        @DisplayName("같은 서비스의 ENDED 구독만 있으면 새 구독으로 등록한다")
        void createsWhenOnlyEndedSubscriptionExists() {
            // given — ACTIVE 서비스 ID 목록에는 ENDED 구독이 포함되지 않는다
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(
                List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of(melon.getId()));
            givenSaveReturnsArgument();

            // when
            SubscriptionCreationResponse response = subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null)));

            // then
            assertThat(response.createdCount()).isEqualTo(1);
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            assertThat(subscriptionsCaptor.getValue()).extracting(Subscription::getServiceCatalog)
                .containsExactly(netflix);
        }

        @Test
        @DisplayName("같은 serviceId가 요청에 두 번 있으면 DUPLICATE_SERVICE_IN_REQUEST 예외를 던지고 저장하지 않는다")
        void throwsWhenServiceDuplicatedInRequest() {
            // given
            givenUser();

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null), customItem("동네 헬스장"), catalogItem(netflix, null))))
                .isInstanceOf(DuplicateServiceInRequestException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.DUPLICATE_SERVICE_IN_REQUEST);
            verifyNoInteractions(serviceCatalogGetService, subscriptionGetService, subscriptionCreateService);
        }

        @Test
        @DisplayName("없거나 비활성인 serviceId가 있으면 SERVICE_NOT_FOUND 예외를 던지고 저장하지 않는다")
        void throwsWhenServiceNotFound() {
            // given — melon은 비활성이라 조회되지 않음
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId(), melon.getId())))
                .willReturn(List.of(netflix));

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null), catalogItem(melon, null))))
                .isInstanceOf(ServiceNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CatalogErrorCode.SERVICE_NOT_FOUND);
            verifyNoInteractions(subscriptionCreateService);
        }

        @Test
        @DisplayName("이미 ACTIVE로 구독 중인 서비스가 하나라도 있으면 DUPLICATE_SUBSCRIPTION 예외를 던지고 아무것도 저장하지 않는다")
        void throwsWhenAlreadySubscribed() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId(), melon.getId())))
                .willReturn(List.of(netflix, melon));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of(melon.getId()));

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null), customItem("동네 헬스장"), catalogItem(melon, null))))
                .isInstanceOf(DuplicateSubscriptionException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.DUPLICATE_SUBSCRIPTION);
            verify(subscriptionCreateService, never()).createSubscriptions(any());
        }

        @Test
        @DisplayName("동시 등록으로 중복 조회를 통과했어도 저장할 때 같은 서비스의 ACTIVE 구독과 충돌하면 DUPLICATE_SUBSCRIPTION 예외를 던진다")
        void throwsWhenActiveSubscriptionConflictsOnSave() {
            // given — 다른 요청이 먼저 저장해 DB 고유 제약(uk_subscriptions_user_active_service)에 걸린 상황
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId())))
                .willReturn(List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            given(subscriptionCreateService.createSubscriptions(any()))
                .willThrow(new DuplicateSubscriptionException());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null), customItem("동네 헬스장"))))
                .isInstanceOf(DuplicateSubscriptionException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.DUPLICATE_SUBSCRIPTION);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 저장하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null))))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(serviceCatalogGetService, subscriptionGetService, subscriptionCreateService);
        }
    }

    @Nested
    @DisplayName("getSubscriptions")
    class GetSubscriptions {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();

        private Subscription active(String name, int amount, int cycleValue, BillingCycleUnit cycleUnit,
                                    LocalDate billingAnchorOn) {
            Subscription subscription = Subscription.createCustom(user, name, Category.ETC, amount, cycleValue,
                cycleUnit, billingAnchorOn, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            return persisted(subscription, today.minusMonths(1));
        }

        private Subscription ended(String name, int amount, int cycleValue, BillingCycleUnit cycleUnit,
                                   LocalDate billingAnchorOn, LocalDate startedOn, LocalDate canceledOn) {
            Subscription subscription = Subscription.createCustom(user, name, Category.ETC, amount, cycleValue,
                cycleUnit, billingAnchorOn, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            subscription.cancel(canceledOn);
            return persisted(subscription, startedOn);
        }

        // id · createdAt은 JPA 저장 시(@PrePersist) 채워지므로 테스트에서만 지정한다.
        private Subscription persisted(Subscription subscription, LocalDate startedOn) {
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", startedOn.atStartOfDay());
            return subscription;
        }

        private void givenSubscriptions(Subscription... subscriptions) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscriptions(user)).willReturn(List.of(subscriptions));
        }

        private List<String> namesOf(SubscriptionListResponse response) {
            return response.subscriptions().stream().map(SubscriptionSummaryResponse::serviceName).toList();
        }

        private Map<String, SubscriptionSummaryResponse> byName(SubscriptionListResponse response) {
            return response.subscriptions().stream()
                .collect(Collectors.toMap(SubscriptionSummaryResponse::serviceName, Function.identity()));
        }

        @Test
        @DisplayName("status · sort가 없으면 구독 중 탭을 다음 결제일 순으로 반환하고, 두 탭 개수와 구독 중 월 환산 합계를 함께 반환한다")
        void returnsActiveTabByDefault() {
            // given
            Subscription later = active("유튜브 프리미엄", 14900, 1, BillingCycleUnit.MONTH, today.plusDays(10));
            Subscription sooner = active("넷플릭스", 13500, 1, BillingCycleUnit.MONTH, today.plusDays(3));
            Subscription canceled = ended("왓챠", 7900, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 16));
            givenSubscriptions(later, sooner, canceled);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId, null, null);

            // then
            assertThat(namesOf(response)).containsExactly("넷플릭스", "유튜브 프리미엄");
            assertThat(response.count()).isEqualTo(2);
            assertThat(response.activeCount()).isEqualTo(2);
            assertThat(response.endedCount()).isEqualTo(1);
            assertThat(response.monthlyTotal()).isEqualTo(13500 + 14900);
            SubscriptionSummaryResponse first = response.subscriptions().getFirst();
            assertThat(first.nextBillingDate()).isEqualTo(today.plusDays(3));
            assertThat(first.dDay()).isEqualTo(3);
            assertThat(first.canceledAt()).isNull();
            assertThat(first.totalPaidAmount()).isNull();
        }

        @Test
        @DisplayName("구독 중 탭은 다음 결제일 오름차순, 같으면 금액 높은 순으로 정렬한다")
        void sortsActiveByNextBillingDateThenAmount() {
            // given
            Subscription cheap = active("저가", 5000, 1, BillingCycleUnit.MONTH, today.plusDays(5));
            Subscription expensive = active("고가", 15000, 1, BillingCycleUnit.MONTH, today.plusDays(5));
            Subscription soonest = active("가장 먼저", 1000, 1, BillingCycleUnit.MONTH, today.plusDays(2));
            givenSubscriptions(cheap, expensive, soonest);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ACTIVE, SubscriptionSortType.NEXT_BILLING_DATE);

            // then
            assertThat(namesOf(response)).containsExactly("가장 먼저", "고가", "저가");
        }

        @Test
        @DisplayName("구독 종료 탭(sort 없음)은 해지일 최근 순으로 정렬하고 월 환산 합계를 '절약' 금액으로 반환한다")
        void sortsEndedByCanceledDateAndReturnsSaving() {
            // given
            LocalDate anchor = LocalDate.of(2026, 1, 15);
            LocalDate startedOn = LocalDate.of(2026, 1, 1);
            Subscription oldest = ended("오래전 해지", 9000, 1, BillingCycleUnit.MONTH, anchor, startedOn,
                LocalDate.of(2026, 10, 1));
            Subscription recentCheap = ended("최근 해지 저가", 10000, 1, BillingCycleUnit.MONTH, anchor, startedOn,
                LocalDate.of(2026, 11, 1));
            Subscription recentExpensive = ended("최근 해지 고가", 30000, 1, BillingCycleUnit.MONTH, anchor, startedOn,
                LocalDate.of(2026, 11, 1));
            Subscription stillActive = active("구독 중", 5000, 1, BillingCycleUnit.MONTH, today.plusDays(1));
            givenSubscriptions(oldest, recentCheap, recentExpensive, stillActive);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ENDED, null);

            // then
            assertThat(namesOf(response)).containsExactly("최근 해지 고가", "최근 해지 저가", "오래전 해지");
            assertThat(response.count()).isEqualTo(3);
            assertThat(response.activeCount()).isEqualTo(1);
            assertThat(response.endedCount()).isEqualTo(3);
            assertThat(response.monthlyTotal()).isEqualTo(9000 + 10000 + 30000);
        }

        @Test
        @DisplayName("AMOUNT_DESC면 구독 중은 결제 금액, 구독 종료는 월 환산 금액이 높은 순으로 정렬한다")
        void sortsByAmountDescPerTab() {
            // given
            Subscription activeSmall = active("구독 중 소액", 10000, 1, BillingCycleUnit.MONTH, today.plusDays(1));
            Subscription activeLarge = active("구독 중 고액", 30000, 1, BillingCycleUnit.MONTH, today.plusDays(20));
            // 연 120,000원(월 10,000원) vs 월 20,000원 — 결제 금액은 연 구독이 크지만 월 환산은 월 구독이 크다
            Subscription endedYearly = ended("연 구독", 120000, 1, BillingCycleUnit.YEAR,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 1));
            Subscription endedMonthly = ended("월 구독", 20000, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 1));
            givenSubscriptions(activeSmall, activeLarge, endedYearly, endedMonthly);

            // when
            SubscriptionListResponse activeResponse = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ACTIVE, SubscriptionSortType.AMOUNT_DESC);
            SubscriptionListResponse endedResponse = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ENDED, SubscriptionSortType.AMOUNT_DESC);

            // then
            assertThat(namesOf(activeResponse)).containsExactly("구독 중 고액", "구독 중 소액");
            assertThat(namesOf(endedResponse)).containsExactly("월 구독", "연 구독");
        }

        @Test
        @DisplayName("기준일이 지난 구독은 오늘 이후 첫 결제일과 D-n을 반환하고, 오늘이 결제일이면 D-0이다")
        void returnsNextBillingDateFromPastAnchor() {
            // given — 매주 결제: 기준일 10일 전 → 다음 결제일은 4일 뒤, 기준일 14일 전 → 오늘
            Subscription fourDaysLeft = active("4일 남음", 5000, 1, BillingCycleUnit.WEEK, today.minusDays(10));
            Subscription billingToday = active("오늘 결제", 5000, 1, BillingCycleUnit.WEEK, today.minusDays(14));
            givenSubscriptions(fourDaysLeft, billingToday);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId, null, null);

            // then
            Map<String, SubscriptionSummaryResponse> responses = byName(response);
            assertThat(responses.get("4일 남음").nextBillingDate()).isEqualTo(today.plusDays(4));
            assertThat(responses.get("4일 남음").dDay()).isEqualTo(4);
            assertThat(responses.get("오늘 결제").nextBillingDate()).isEqualTo(today);
            assertThat(responses.get("오늘 결제").dDay()).isZero();
            assertThat(namesOf(response)).containsExactly("오늘 결제", "4일 남음");
        }

        @Test
        @DisplayName("월말 기준일(1월 31일) 매월 구독은 2월 말일, 3월 31일 순으로 결제일을 계산한다")
        void keepsMonthEndBillingDate() {
            // given — 결제일 1/31 → 2/28 → 3/31. 3/28로 밀렸다면 3/30 해지 구독도 3번 결제로 계산된다
            LocalDate anchor = LocalDate.of(2026, 1, 31);
            LocalDate startedOn = LocalDate.of(2026, 1, 1);
            Subscription canceledBeforeMonthEnd = ended("3월 30일 해지", 10000, 1, BillingCycleUnit.MONTH, anchor,
                startedOn, LocalDate.of(2026, 3, 30));
            Subscription canceledOnMonthEnd = ended("3월 31일 해지", 10000, 1, BillingCycleUnit.MONTH, anchor,
                startedOn, LocalDate.of(2026, 3, 31));
            givenSubscriptions(canceledBeforeMonthEnd, canceledOnMonthEnd);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ENDED, null);

            // then
            Map<String, SubscriptionSummaryResponse> responses = byName(response);
            assertThat(responses.get("3월 30일 해지").totalPaidAmount()).isEqualTo(20000);
            assertThat(responses.get("3월 31일 해지").totalPaidAmount()).isEqualTo(30000);
        }

        @Test
        @DisplayName("결제 주기별(DAY · WEEK · MONTH n · YEAR) 월 환산 금액을 원 단위 반올림해 합산한다")
        void convertsMonthlyAmountPerBillingCycle() {
            // given
            Subscription daily = active("매일", 1000, 1, BillingCycleUnit.DAY, today.plusDays(1));
            Subscription weekly = active("매주", 10000, 1, BillingCycleUnit.WEEK, today.plusDays(1));
            Subscription quarterly = active("3개월", 30000, 3, BillingCycleUnit.MONTH, today.plusDays(1));
            Subscription yearly = active("매년", 100000, 1, BillingCycleUnit.YEAR, today.plusDays(1));
            givenSubscriptions(daily, weekly, quarterly, yearly);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId, null, null);

            // then
            Map<String, SubscriptionSummaryResponse> responses = byName(response);
            assertThat(responses.get("매일").monthlyAmount()).isEqualTo(30417);    // 1,000 × 365 / 12 = 30,416.67
            assertThat(responses.get("매주").monthlyAmount()).isEqualTo(43333);    // 10,000 × 52 / 12 = 43,333.33
            assertThat(responses.get("3개월").monthlyAmount()).isEqualTo(10000);   // 30,000 / 3
            assertThat(responses.get("매년").monthlyAmount()).isEqualTo(8333);     // 100,000 / 12 = 8,333.33
            assertThat(response.monthlyTotal()).isEqualTo(30417 + 43333 + 10000 + 8333);
        }

        @Test
        @DisplayName("월 환산 금액 · 합계 · 총 지출이 int 범위를 넘어도 목록 조회에 성공하고 long 금액으로 반환한다")
        void returnsAmountsBeyondIntRange() {
            // given — 매일 1억 원 → 월 환산 3,041,666,667원 (int 최대 2,147,483,647 초과)
            Subscription daily = active("매일 1억", 100_000_000, 1, BillingCycleUnit.DAY, today.plusDays(1));
            Subscription monthly = active("매월 20억", 2_000_000_000, 1, BillingCycleUnit.MONTH, today.plusDays(1));
            // 결제일 1/10, 2/10, 3/10, 4/10 → 4번 × 20억 = 80억
            Subscription canceled = ended("종료 20억", 2_000_000_000, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 4, 10));
            givenSubscriptions(daily, monthly, canceled);

            // when
            SubscriptionListResponse activeTab = subscriptionUseCase.getSubscriptions(userId, null, null);
            SubscriptionListResponse endedTab = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ENDED, null);

            // then
            assertThat(byName(activeTab).get("매일 1억").monthlyAmount()).isEqualTo(3_041_666_667L);
            assertThat(activeTab.monthlyTotal()).isEqualTo(3_041_666_667L + 2_000_000_000L);
            assertThat(endedTab.subscriptions().getFirst().totalPaidAmount()).isEqualTo(8_000_000_000L);
        }

        @Test
        @DisplayName("구독 종료 항목은 해지일과 등록일부터 해지일까지(양끝 포함) 결제 횟수 × 금액을 총 지출로 반환하고 다음 결제일 · D-n은 null이다")
        void returnsTotalPaidAmountForEnded() {
            // given — 결제일 1/10, 2/10, 3/10, 4/10 (등록일 1/10 · 해지일 4/10 포함) → 4번
            Subscription canceled = ended("왓챠", 13500, 1, BillingCycleUnit.MONTH, LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 4, 10));
            givenSubscriptions(canceled);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ENDED, null);

            // then
            SubscriptionSummaryResponse item = response.subscriptions().getFirst();
            assertThat(item.canceledAt()).isEqualTo(LocalDate.of(2026, 4, 10));
            assertThat(item.totalPaidAmount()).isEqualTo(4 * 13500);
            assertThat(item.nextBillingDate()).isNull();
            assertThat(item.dDay()).isNull();
        }

        @Test
        @DisplayName("직접 입력 서비스는 logoUrl을 null로 반환한다")
        void returnsNullLogoForCustomService() {
            // given
            ServiceCatalog netflix = service("넷플릭스", Category.VIDEO_OTT, "https://logo.example.com/netflix.png");
            Subscription catalogSubscription = persisted(Subscription.createFromCatalog(user, netflix,
                Category.VIDEO_OTT, 13500, 1, BillingCycleUnit.MONTH, today.plusDays(1), PaymentMethod.APP_STORE,
                RegistrationSource.MANUAL), today.minusMonths(1));
            Subscription customSubscription = active("동네 헬스장", 50000, 1, BillingCycleUnit.MONTH,
                today.plusDays(2));
            givenSubscriptions(catalogSubscription, customSubscription);

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId, null, null);

            // then
            Map<String, SubscriptionSummaryResponse> responses = byName(response);
            assertThat(responses.get("넷플릭스").logoUrl()).isEqualTo("https://logo.example.com/netflix.png");
            assertThat(responses.get("동네 헬스장").logoUrl()).isNull();
        }

        @Test
        @DisplayName("구독이 없으면 개수 0, 합계 0, 빈 목록을 반환한다")
        void returnsEmptyWhenNoSubscriptions() {
            // given
            givenSubscriptions();

            // when
            SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId, null, null);

            // then
            assertThat(response.count()).isZero();
            assertThat(response.activeCount()).isZero();
            assertThat(response.endedCount()).isZero();
            assertThat(response.monthlyTotal()).isZero();
            assertThat(response.subscriptions()).isEmpty();
        }

        @Test
        @DisplayName("구독 중 탭에 CANCELED_AT, 구독 종료 탭에 NEXT_BILLING_DATE 정렬을 요청하면 INVALID_SUBSCRIPTION_SORT 예외를 던진다")
        void throwsWhenSortDoesNotMatchStatus() {
            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ACTIVE, SubscriptionSortType.CANCELED_AT))
                .isInstanceOf(InvalidSubscriptionSortException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.INVALID_SUBSCRIPTION_SORT);
            assertThatThrownBy(() -> subscriptionUseCase.getSubscriptions(userId,
                SubscriptionStatus.ENDED, SubscriptionSortType.NEXT_BILLING_DATE))
                .isInstanceOf(InvalidSubscriptionSortException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.INVALID_SUBSCRIPTION_SORT);
            verifyNoInteractions(userGetService, subscriptionGetService);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.getSubscriptions(userId, null, null))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }
    }

    @Nested
    @DisplayName("getSubscription")
    class GetSubscription {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();

        // 구독 종료 픽스처: 2026-01-01 등록, 매월 15일 결제, 2026-10-16 해지 → 1/15 ~ 10/15 결제 10회
        private final LocalDate endedAnchor = LocalDate.of(2026, 1, 15);
        private final LocalDate endedStartedOn = LocalDate.of(2026, 1, 1);
        private final LocalDate endedCanceledOn = LocalDate.of(2026, 10, 16);

        private Subscription activeFromCatalog(ServiceCatalog service, LocalDate billingAnchorOn,
                                               LocalDate startedOn) {
            Subscription subscription = Subscription.createFromCatalog(user, service, service.getCategory(), 13500,
                1, BillingCycleUnit.MONTH, billingAnchorOn, PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            return persisted(subscription, startedOn);
        }

        private Subscription endedCustom() {
            Subscription subscription = Subscription.createCustom(user, "왓챠", Category.VIDEO_OTT, 7900, 1,
                BillingCycleUnit.MONTH, endedAnchor, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            subscription.cancel(endedCanceledOn);
            return persisted(subscription, endedStartedOn);
        }

        // id · createdAt은 JPA 저장 시(@PrePersist) 채워지므로 테스트에서만 지정한다.
        private Subscription persisted(Subscription subscription, LocalDate startedOn) {
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", startedOn.atStartOfDay());
            return subscription;
        }

        private void givenSubscription(Subscription subscription) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscription.getId()))
                .willReturn(Optional.of(subscription));
        }

        @Test
        @DisplayName("구독 중이면 결제 금액 · 연 환산 · 다음 결제일 · D-n · 구독 시작일 · 지금까지 결제 횟수와 금액을 반환하고 해지일은 null이다")
        void returnsActiveSubscriptionDetail() {
            // given — 2개월 전 등록, 5일 뒤가 결제일인 월 구독 → 등록 후 결제일 2번 지남
            ServiceCatalog netflix = service("넷플릭스", Category.VIDEO_OTT, "https://logo.example.com/netflix.png");
            LocalDate startedOn = today.minusMonths(2);
            Subscription subscription = activeFromCatalog(netflix, today.plusDays(5), startedOn);
            givenSubscription(subscription);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), null);

            // then
            assertThat(response.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(response.serviceName()).isEqualTo("넷플릭스");
            assertThat(response.logoUrl()).isEqualTo("https://logo.example.com/netflix.png");
            assertThat(response.category()).isEqualTo(Category.VIDEO_OTT);
            assertThat(response.amount()).isEqualTo(13500);
            assertThat(response.billingCycleValue()).isEqualTo(1);
            assertThat(response.billingCycleUnit()).isEqualTo(BillingCycleUnit.MONTH);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.APP_STORE);
            assertThat(response.isSteady()).isFalse();
            assertThat(response.yearlyAmount()).isEqualTo(13500 * 12);
            assertThat(response.nextBillingDate()).isEqualTo(today.plusDays(5));
            assertThat(response.dDay()).isEqualTo(5);
            assertThat(response.startedAt()).isEqualTo(startedOn);
            assertThat(response.paidCount()).isEqualTo(2);
            assertThat(response.totalPaidAmount()).isEqualTo(13500 * 2);
            assertThat(response.canceledAt()).isNull();
        }

        @Test
        @DisplayName("구독 종료면 해지일 · 해지일까지 결제 금액을 반환하고 다음 결제일 · D-n은 null이다")
        void returnsEndedSubscriptionDetail() {
            // given
            Subscription subscription = endedCustom();
            givenSubscription(subscription);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), null);

            // then
            assertThat(response.status()).isEqualTo(SubscriptionStatus.ENDED);
            assertThat(response.canceledAt()).isEqualTo(endedCanceledOn);
            assertThat(response.paidCount()).isEqualTo(10);
            assertThat(response.totalPaidAmount()).isEqualTo(7900 * 10);
            assertThat(response.nextBillingDate()).isNull();
            assertThat(response.dDay()).isNull();
        }

        @Test
        @DisplayName("baseDate를 주면 그 날짜 이후 첫 결제일과, 등록일 ~ 해지일 범위로 판단한 상태를 반환한다")
        void calculatesWithBaseDate() {
            // given — 구독 종료 구독의 이용 기간(1/1 ~ 10/16) 안의 날짜
            Subscription subscription = endedCustom();
            givenSubscription(subscription);
            LocalDate baseDate = LocalDate.of(2026, 6, 20);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), baseDate);

            // then — 지금까지 결제 횟수 · 금액은 baseDate와 무관하게 해지일까지
            assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(response.nextBillingDate()).isEqualTo(LocalDate.of(2026, 7, 15));
            assertThat(response.dDay()).isEqualTo(25);
            assertThat(response.canceledAt()).isEqualTo(endedCanceledOn);
            assertThat(response.paidCount()).isEqualTo(10);
            assertThat(response.totalPaidAmount()).isEqualTo(7900 * 10);
        }

        @Test
        @DisplayName("baseDate가 등록일 이전이면 구독 중이어도 ENDED 상태로 반환한다")
        void returnsEndedWhenBaseDateBeforeStart() {
            // given
            ServiceCatalog netflix = service("넷플릭스", Category.VIDEO_OTT, "https://logo.example.com/netflix.png");
            LocalDate startedOn = today.minusMonths(2);
            Subscription subscription = activeFromCatalog(netflix, today.plusDays(5), startedOn);
            givenSubscription(subscription);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), startedOn.minusDays(1));

            // then
            assertThat(response.status()).isEqualTo(SubscriptionStatus.ENDED);
        }

        @Test
        @DisplayName("직접 입력 서비스는 logoUrl을 null로 반환한다")
        void returnsNullLogoForCustomService() {
            // given
            Subscription subscription = endedCustom();
            givenSubscription(subscription);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), null);

            // then
            assertThat(response.serviceName()).isEqualTo("왓챠");
            assertThat(response.logoUrl()).isNull();
        }

        @Test
        @DisplayName("연 환산 금액 · 지금까지 결제 금액이 int 범위를 넘어도 상세 조회에 성공하고 long 금액으로 반환한다")
        void returnsDetailAmountsBeyondIntRange() {
            // given — 매월 20억 원, 1/15 ~ 10/15 결제 10번
            Subscription subscription = Subscription.createCustom(user, "고액 구독", Category.ETC, 2_000_000_000, 1,
                BillingCycleUnit.MONTH, endedAnchor, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            subscription.cancel(endedCanceledOn);
            persisted(subscription, endedStartedOn);
            givenSubscription(subscription);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), null);

            // then
            assertThat(response.yearlyAmount()).isEqualTo(24_000_000_000L);
            assertThat(response.paidCount()).isEqualTo(10);
            assertThat(response.totalPaidAmount()).isEqualTo(20_000_000_000L);
        }

        @Test
        @DisplayName("점검 필드는 latestCheckup · pastCheckupAverage를 null로, pastCheckups를 빈 목록으로 반환한다")
        void returnsEmptyCheckupFields() {
            // given
            Subscription subscription = endedCustom();
            givenSubscription(subscription);

            // when
            SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId,
                subscription.getId(), null);

            // then
            assertThat(response.latestCheckup()).isNull();
            assertThat(response.pastCheckups()).isEmpty();
            assertThat(response.pastCheckupAverage()).isNull();
        }

        @Test
        @DisplayName("없거나 다른 회원의 구독이면 SUBSCRIPTION_NOT_FOUND 예외를 던진다")
        void throwsWhenSubscriptionNotFound() {
            // given
            UUID subscriptionId = UUID.randomUUID();
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscriptionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.getSubscription(userId, subscriptionId, null))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.getSubscription(userId, UUID.randomUUID(), null))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }
    }

    @Nested
    @DisplayName("updateSubscription")
    class UpdateSubscription {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();
        private final LocalDate originalAnchor = today.plusDays(3);

        private Subscription catalogSubscription() {
            ServiceCatalog netflix = service("넷플릭스", Category.VIDEO_OTT, "https://logo.example.com/netflix.png");
            Subscription subscription = Subscription.createFromCatalog(user, netflix, Category.VIDEO_OTT, 13500, 1,
                BillingCycleUnit.MONTH, originalAnchor, PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            return persisted(subscription);
        }

        private Subscription customSubscription(String name) {
            Subscription subscription = Subscription.createCustom(user, name, Category.ETC, 5000, 1,
                BillingCycleUnit.MONTH, originalAnchor, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            return persisted(subscription);
        }

        // id는 JPA 저장 시 생성되므로 요청 ID와의 매칭을 위해 테스트에서만 지정한다.
        private Subscription persisted(Subscription subscription) {
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            return subscription;
        }

        // 연 1회 120,000원 → 월 환산 10,000원 → 연 환산 120,000원
        private UpdateSubscriptionRequest request(String serviceName) {
            return new UpdateSubscriptionRequest(serviceName, Category.PRODUCTIVITY_AI, 120000, 1,
                BillingCycleUnit.YEAR, today.plusDays(10), PaymentMethod.GOOGLE_PLAY);
        }

        private void givenSubscription(Subscription subscription) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscription.getId()))
                .willReturn(Optional.of(subscription));
        }

        @Test
        @DisplayName("구독 중이면 카테고리 · 금액 · 결제 주기 · 결제한 곳을 바꾸고 다음 결제일을 결제 기준일로 저장한 뒤 재계산한 연 환산 · 다음 결제일을 반환한다")
        void updatesPaymentInfo() {
            // given
            Subscription subscription = catalogSubscription();
            givenSubscription(subscription);

            // when
            SubscriptionBillingResponse response = subscriptionUseCase.updateSubscription(userId,
                subscription.getId(), request(null));

            // then
            assertThat(subscription.getCategory()).isEqualTo(Category.PRODUCTIVITY_AI);
            assertThat(subscription.getAmount()).isEqualTo(120000);
            assertThat(subscription.getBillingCycleValue()).isEqualTo(1);
            assertThat(subscription.getBillingCycleUnit()).isEqualTo(BillingCycleUnit.YEAR);
            assertThat(subscription.getBillingAnchorOn()).isEqualTo(today.plusDays(10));
            assertThat(subscription.getPaymentMethod()).isEqualTo(PaymentMethod.GOOGLE_PLAY);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(response.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(response.yearlyAmount()).isEqualTo(120000);
            assertThat(response.nextBillingDate()).isEqualTo(today.plusDays(10));
        }

        @Test
        @DisplayName("직접 입력 서비스는 요청한 서비스명을 앞뒤 공백을 제거해 저장한다")
        void updatesCustomNameStripped() {
            // given
            Subscription subscription = customSubscription("헬스장");
            givenSubscription(subscription);

            // when
            subscriptionUseCase.updateSubscription(userId, subscription.getId(), request("  동네 헬스장  "));

            // then
            assertThat(subscription.getName()).isEqualTo("동네 헬스장");
        }

        @Test
        @DisplayName("직접 입력 서비스에 서비스명을 보내지 않으면 기존 이름을 유지한다")
        void keepsCustomNameWhenNull() {
            // given
            Subscription subscription = customSubscription("헬스장");
            givenSubscription(subscription);

            // when
            subscriptionUseCase.updateSubscription(userId, subscription.getId(), request(null));

            // then
            assertThat(subscription.getName()).isEqualTo("헬스장");
            assertThat(subscription.getAmount()).isEqualTo(120000);
        }

        @Test
        @DisplayName("연 환산 금액이 int 범위를 넘어도 수정에 성공하고 long 금액으로 반환한다")
        void returnsYearlyAmountBeyondIntRange() {
            // given — 매월 2억 원 → 연 환산 24억 원 (int 최대 2,147,483,647 초과)
            Subscription subscription = catalogSubscription();
            givenSubscription(subscription);
            UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(null, Category.ETC, 200_000_000, 1,
                BillingCycleUnit.MONTH, today.plusDays(10), PaymentMethod.WEB_DIRECT);

            // when
            SubscriptionBillingResponse response = subscriptionUseCase.updateSubscription(userId,
                subscription.getId(), request);

            // then
            assertThat(subscription.getAmount()).isEqualTo(200_000_000);
            assertThat(response.yearlyAmount()).isEqualTo(2_400_000_000L);
        }

        @Test
        @DisplayName("사전 등록 서비스는 요청한 서비스명을 무시하고 서비스명 스냅샷을 유지한다")
        void ignoresServiceNameForCatalogService() {
            // given
            Subscription subscription = catalogSubscription();
            givenSubscription(subscription);

            // when
            subscriptionUseCase.updateSubscription(userId, subscription.getId(), request("다른 이름"));

            // then
            assertThat(subscription.getName()).isEqualTo("넷플릭스");
            assertThat(subscription.getAmount()).isEqualTo(120000);
        }

        @Test
        @DisplayName("구독 종료된 구독이면 SUBSCRIPTION_NOT_ACTIVE 예외를 던지고 값을 바꾸지 않는다")
        void throwsWhenSubscriptionEnded() {
            // given
            Subscription subscription = customSubscription("헬스장");
            subscription.cancel(today.minusDays(1));
            givenSubscription(subscription);

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.updateSubscription(userId, subscription.getId(),
                request("동네 헬스장")))
                .isInstanceOf(SubscriptionNotActiveException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
            assertThat(subscription.getName()).isEqualTo("헬스장");
            assertThat(subscription.getAmount()).isEqualTo(5000);
            assertThat(subscription.getBillingAnchorOn()).isEqualTo(originalAnchor);
        }

        @Test
        @DisplayName("없거나 다른 회원의 구독이면 SUBSCRIPTION_NOT_FOUND 예외를 던진다")
        void throwsWhenSubscriptionNotFound() {
            // given
            UUID subscriptionId = UUID.randomUUID();
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscriptionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.updateSubscription(userId, subscriptionId, request(null)))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.updateSubscription(userId, UUID.randomUUID(),
                request(null)))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }
    }

    @Nested
    @DisplayName("deleteSubscriptions")
    class DeleteSubscriptions {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate billingOn = LocalDate.of(2026, 12, 1);

        private Subscription subscription(String name) {
            Subscription subscription = Subscription.createCustom(user, name, Category.ETC, 10000, 1,
                BillingCycleUnit.MONTH, billingOn, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            // id는 JPA 저장 시 생성되므로, 요청 ID와의 매칭 검증을 위해 테스트에서만 지정한다.
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            return subscription;
        }

        private SubscriptionCheck check(Subscription subscription) {
            return SubscriptionCheck.create(subscription, billingOn, Category.ETC, 3, 3, 3, null);
        }

        private Notification notification(Subscription subscription) {
            return Notification.create(user, subscription, NotificationType.BILLING_REMINDER, billingOn,
                "결제 1일 전", subscription.getName() + " 결제 예정", LocalDateTime.of(2026, 11, 30, 9, 0));
        }

        private DeleteSubscriptionsRequest request(UUID... subscriptionIds) {
            return new DeleteSubscriptionsRequest(List.of(subscriptionIds));
        }

        private void givenUser() {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
        }

        @Test
        @DisplayName("선택한 구독과 그 점검 기록 · 해지 이유 · 알림을 모두 Soft Delete 하고 삭제 개수를 반환한다")
        void softDeletesSubscriptionsWithRelatedData() {
            // given
            Subscription netflix = subscription("넷플릭스");
            Subscription melon = subscription("멜론");
            SubscriptionCheck netflixCheck = check(netflix);
            SubscriptionCheckCancelReason cancelReason = SubscriptionCheckCancelReason.create(netflixCheck,
                CancelReason.PRICE_BURDEN);
            Notification melonNotification = notification(melon);
            givenUser();
            given(subscriptionGetService.findSubscriptionsByIds(user, Set.of(netflix.getId(), melon.getId())))
                .willReturn(List.of(netflix, melon));
            given(subscriptionCheckGetService.findChecksBySubscriptions(List.of(netflix, melon)))
                .willReturn(List.of(netflixCheck));
            given(subscriptionCheckCancelReasonGetService.findCancelReasonsByChecks(List.of(netflixCheck)))
                .willReturn(List.of(cancelReason));
            given(notificationGetService.findNotificationsBySubscriptions(List.of(netflix, melon)))
                .willReturn(List.of(melonNotification));

            // when
            SubscriptionDeletionResponse response = subscriptionUseCase.deleteSubscriptions(userId,
                request(netflix.getId(), melon.getId()));

            // then
            assertThat(response.deletedCount()).isEqualTo(2);
            assertThat(List.of(netflix, melon)).allMatch(Subscription::isDeleted);
            assertThat(netflixCheck.isDeleted()).isTrue();
            assertThat(cancelReason.isDeleted()).isTrue();
            assertThat(melonNotification.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("구독 종료(ENDED) 구독도 삭제한다")
        void deletesEndedSubscription() {
            // given
            Subscription watcha = subscription("왓챠");
            watcha.cancel(LocalDate.of(2026, 10, 16));
            givenUser();
            given(subscriptionGetService.findSubscriptionsByIds(user, Set.of(watcha.getId())))
                .willReturn(List.of(watcha));
            given(subscriptionCheckGetService.findChecksBySubscriptions(List.of(watcha))).willReturn(List.of());
            given(notificationGetService.findNotificationsBySubscriptions(List.of(watcha))).willReturn(List.of());

            // when
            SubscriptionDeletionResponse response = subscriptionUseCase.deleteSubscriptions(userId,
                request(watcha.getId()));

            // then
            assertThat(response.deletedCount()).isEqualTo(1);
            assertThat(watcha.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("점검 기록이 없으면 해지 이유를 조회하지 않고 구독과 알림만 삭제한다")
        void skipsCancelReasonsWhenNoChecks() {
            // given
            Subscription netflix = subscription("넷플릭스");
            Notification netflixNotification = notification(netflix);
            givenUser();
            given(subscriptionGetService.findSubscriptionsByIds(user, Set.of(netflix.getId())))
                .willReturn(List.of(netflix));
            given(subscriptionCheckGetService.findChecksBySubscriptions(List.of(netflix))).willReturn(List.of());
            given(notificationGetService.findNotificationsBySubscriptions(List.of(netflix)))
                .willReturn(List.of(netflixNotification));

            // when
            subscriptionUseCase.deleteSubscriptions(userId, request(netflix.getId()));

            // then
            verifyNoInteractions(subscriptionCheckCancelReasonGetService);
            assertThat(netflix.isDeleted()).isTrue();
            assertThat(netflixNotification.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("요청에 같은 ID가 여러 번 있어도 한 번만 조회해 삭제하고 삭제 개수는 중복을 제외한다")
        void deduplicatesRequestedIds() {
            // given
            Subscription netflix = subscription("넷플릭스");
            givenUser();
            given(subscriptionGetService.findSubscriptionsByIds(user, Set.of(netflix.getId())))
                .willReturn(List.of(netflix));
            given(subscriptionCheckGetService.findChecksBySubscriptions(List.of(netflix))).willReturn(List.of());
            given(notificationGetService.findNotificationsBySubscriptions(List.of(netflix))).willReturn(List.of());

            // when
            SubscriptionDeletionResponse response = subscriptionUseCase.deleteSubscriptions(userId,
                request(netflix.getId(), netflix.getId()));

            // then
            assertThat(response.deletedCount()).isEqualTo(1);
            assertThat(netflix.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("없거나 다른 회원의 구독이 하나라도 있으면 SUBSCRIPTION_NOT_FOUND 예외를 던지고 아무것도 삭제하지 않는다")
        void throwsWhenAnySubscriptionNotFound() {
            // given — 요청 2건 중 본인 구독 1건만 조회됨 (다른 하나는 없거나 다른 회원 구독)
            Subscription netflix = subscription("넷플릭스");
            UUID othersSubscriptionId = UUID.randomUUID();
            givenUser();
            given(subscriptionGetService.findSubscriptionsByIds(user, Set.of(netflix.getId(), othersSubscriptionId)))
                .willReturn(List.of(netflix));

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.deleteSubscriptions(userId,
                request(netflix.getId(), othersSubscriptionId)))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
            assertThat(netflix.isDeleted()).isFalse();
            verifyNoInteractions(subscriptionCheckGetService, subscriptionCheckCancelReasonGetService,
                notificationGetService);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 아무것도 삭제하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionUseCase.deleteSubscriptions(userId, request(UUID.randomUUID())))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService, subscriptionCheckGetService,
                subscriptionCheckCancelReasonGetService, notificationGetService);
        }
    }
}
