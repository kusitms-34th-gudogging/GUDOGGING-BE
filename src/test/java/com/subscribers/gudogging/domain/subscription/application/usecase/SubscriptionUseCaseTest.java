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
import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionItemRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionResponse;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateServiceInRequestException;
import com.subscribers.gudogging.domain.subscription.application.exception.DuplicateSubscriptionException;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionErrorCode;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    @InjectMocks
    private SubscriptionUseCase subscriptionUseCase;

    @Captor
    private ArgumentCaptor<List<Subscription>> subscriptionsCaptor;

    private static ServiceCatalog service(String name, Category category, String logoUrl) {
        ServiceCatalog service = ServiceCatalog.create(name, null, null, category, logoUrl, 1);
        // id는 JPA 저장 시 생성되므로, 요청 serviceId와의 매칭 검증을 위해 테스트에서만 지정한다.
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        return service;
    }

    @Nested
    @DisplayName("createSubscriptions")
    class CreateSubscriptions {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate nextBillingOn = LocalDate.now().plusDays(7);

        private final ServiceCatalog netflix = service("넷플릭스", Category.VIDEO_OTT, "https://logo.example.com/netflix.png");
        private final ServiceCatalog melon = service("멜론", Category.MUSIC, "https://logo.example.com/melon.png");

        private SubscriptionItemRequest catalogItem(ServiceCatalog service, String name) {
            return new SubscriptionItemRequest(service.getId(), name, service.getCategory(), 13500, 1,
                BillingCycleUnit.MONTH, nextBillingOn, PaymentMethod.WEB_DIRECT);
        }

        private SubscriptionItemRequest customItem(String name) {
            return new SubscriptionItemRequest(null, name, Category.ETC, 50000, 1,
                BillingCycleUnit.MONTH, nextBillingOn, PaymentMethod.WEB_DIRECT);
        }

        private CreateSubscriptionsRequest request(SubscriptionItemRequest... items) {
            return new CreateSubscriptionsRequest(List.of(items));
        }

        private void givenUser() {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
        }

        private void givenSaveReturnsArgument() {
            given(subscriptionCreateService.createSubscriptions(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("사전 등록 서비스와 직접 입력 서비스를 함께 등록하면 각각 createFromCatalog/createCustom으로 만들어 한 번에 저장하고, 요청 순서대로 응답한다")
        void createsCatalogAndCustomSubscriptionsInRequestOrder() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId(), melon.getId())))
                .willReturn(List.of(melon, netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            List<SubscriptionResponse> responses = subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null), customItem("동네 헬스장"), catalogItem(melon, null)));

            // then
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            List<Subscription> saved = subscriptionsCaptor.getValue();
            assertThat(saved).hasSize(3);
            assertThat(saved).extracting(Subscription::getServiceCatalog).containsExactly(netflix, null, melon);
            assertThat(saved).extracting(Subscription::getUser).containsOnly(user);

            assertThat(responses).extracting(SubscriptionResponse::name).containsExactly("넷플릭스", "동네 헬스장", "멜론");
            assertThat(responses).extracting(SubscriptionResponse::serviceId)
                .containsExactly(netflix.getId(), null, melon.getId());
        }

        @Test
        @DisplayName("사전 등록 서비스는 name을 services.name 스냅샷으로, logoUrl을 서비스 로고로 응답하며 요청의 name은 무시한다")
        void usesCatalogNameAndLogoForCatalogService() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            List<SubscriptionResponse> responses = subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, "내 넷플릭스")));

            // then
            SubscriptionResponse response = responses.getFirst();
            assertThat(response.name()).isEqualTo("넷플릭스");
            assertThat(response.logoUrl()).isEqualTo("https://logo.example.com/netflix.png");
            assertThat(response.category()).isEqualTo(Category.VIDEO_OTT);
            assertThat(response.amount()).isEqualTo(13500);
            assertThat(response.billingCycleValue()).isEqualTo(1);
            assertThat(response.billingCycleUnit()).isEqualTo(BillingCycleUnit.MONTH);
            assertThat(response.nextBillingOn()).isEqualTo(nextBillingOn);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.WEB_DIRECT);
        }

        @Test
        @DisplayName("저장된 구독은 status = ACTIVE, registrationSource = MANUAL, billingAnchorOn = 요청 nextBillingOn이다")
        void savesActiveManualSubscriptionWithBillingAnchor() {
            // given
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            subscriptionUseCase.createSubscriptions(userId, request(catalogItem(netflix, null), customItem("동네 헬스장")));

            // then
            verify(subscriptionCreateService).createSubscriptions(subscriptionsCaptor.capture());
            assertThat(subscriptionsCaptor.getValue()).allSatisfy(subscription -> {
                assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                assertThat(subscription.getRegistrationSource()).isEqualTo(RegistrationSource.MANUAL);
                assertThat(subscription.getBillingAnchorOn()).isEqualTo(nextBillingOn);
                assertThat(subscription.getCanceledOn()).isNull();
                assertThat(subscription.isSteadyUse()).isFalse();
            });
        }

        @Test
        @DisplayName("직접 입력 서비스만 있으면 서비스 조회 없이 serviceId = null, logoUrl = null로 저장하고 이름은 앞뒤 공백을 제거한다")
        void createsCustomOnlyWithoutServiceLookup() {
            // given
            givenUser();
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            givenSaveReturnsArgument();

            // when
            List<SubscriptionResponse> responses = subscriptionUseCase.createSubscriptions(userId,
                request(customItem("  동네 헬스장  ")));

            // then
            verifyNoInteractions(serviceCatalogGetService);
            SubscriptionResponse response = responses.getFirst();
            assertThat(response.serviceId()).isNull();
            assertThat(response.logoUrl()).isNull();
            assertThat(response.name()).isEqualTo("동네 헬스장");
            assertThat(response.category()).isEqualTo(Category.ETC);
        }

        @Test
        @DisplayName("같은 서비스의 ENDED 구독만 있으면 새 구독으로 등록한다")
        void createsWhenOnlyEndedSubscriptionExists() {
            // given — ACTIVE 서비스 ID 목록에는 ENDED 구독이 포함되지 않는다
            givenUser();
            given(serviceCatalogGetService.findActiveServicesByIds(Set.of(netflix.getId()))).willReturn(List.of(netflix));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of(melon.getId()));
            givenSaveReturnsArgument();

            // when
            List<SubscriptionResponse> responses = subscriptionUseCase.createSubscriptions(userId,
                request(catalogItem(netflix, null)));

            // then
            assertThat(responses).extracting(SubscriptionResponse::serviceId).containsExactly(netflix.getId());
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
}
