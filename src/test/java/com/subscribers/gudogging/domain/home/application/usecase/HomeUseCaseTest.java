package com.subscribers.gudogging.domain.home.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeResponse;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeUpcomingPaymentResponse;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeUsageItemResponse;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeUseCase")
class HomeUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private SubscriptionBillingHistoryGetService subscriptionBillingHistoryGetService;

    @Mock
    private SubscriptionCheckGetService subscriptionCheckGetService;

    @Mock
    private NotificationGetService notificationGetService;

    @InjectMocks
    private HomeUseCase homeUseCase;

    @Nested
    @DisplayName("getHome")
    class GetHome {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();
        private final YearMonth thisMonth = YearMonth.from(today);

        @Test
        @DisplayName("summary: 오늘~말일 결제 예정액, 구독 중 개수, 결제일 가까운 순 로고(직접 입력 제외)를 반환하고 구독 종료 구독은 뺀다")
        void returnsSummaryOfActiveSubscriptions() {
            // given
            Subscription dueToday = monthly("넷플릭스", 13500, today);
            Subscription dueNextMonth = monthly("유튜브 프리미엄", 14900, thisMonth.plusMonths(1).atDay(1));
            Subscription custom = custom("동네 헬스장", 50000, today);
            Subscription ended = monthly("멜론", 10900, today);
            ended.cancel(today.minusDays(1));
            givenSubscriptions(dueNextMonth, custom, dueToday, ended);

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.summary().expectedAmount()).isEqualTo(13500 + 50000);
            assertThat(response.summary().subscriptionCount()).isEqualTo(3);
            assertThat(response.summary().logos())
                .containsExactly(logoOf("넷플릭스"), logoOf("유튜브 프리미엄"));
        }

        @Test
        @DisplayName("summary.yearlyAmount: 등록 전 기간도 1월 1일부터 같은 주기로 펼쳐 올해 1~12월 결제 합계를 낸다")
        void returnsYearlyAmountIncludingBeforeRegistration() {
            // given
            Subscription netflix = monthly("넷플릭스", 13500, today.plusDays(3));
            Subscription yearly = subscription("쿠팡 와우", 120000, 1, BillingCycleUnit.YEAR, today.plusDays(10), true);
            givenSubscriptions(netflix, yearly);

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.summary().yearlyAmount()).isEqualTo(13500 * 12 + 120000);
        }

        @Test
        @DisplayName("summary.yearlyAmount: 결제 조건을 수정한 구독은 수정 전 구간을 그때 금액으로 계산한다")
        void returnsYearlyAmountWithBillingHistory() {
            // given
            Year thisYear = Year.from(today);
            Subscription subscription = monthly("넷플릭스", 17000, thisYear.atMonth(7).atDay(5));
            ReflectionTestUtils.setField(subscription, "startedOn", thisYear.atDay(1));
            SubscriptionBillingHistory before = SubscriptionBillingHistory.create(subscription, 13500, 1,
                BillingCycleUnit.MONTH, thisYear.atMonth(1).atDay(5), thisYear.atMonth(6).atDay(20));
            givenSubscriptions(subscription);
            given(subscriptionBillingHistoryGetService.findBillingHistoriesBySubscriptions(anyCollection()))
                .willReturn(List.of(before));

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.summary().yearlyAmount()).isEqualTo(13500 * 6 + 17000 * 6);
        }

        @Test
        @DisplayName("summary.logos: 로고는 최대 10개다")
        void limitsLogosToTen() {
            // given
            List<Subscription> subscriptions = new ArrayList<>();
            IntStream.range(0, 11).forEach(i -> subscriptions.add(monthly("서비스" + i, 1000, today.plusDays(i))));
            givenSubscriptions(subscriptions.toArray(Subscription[]::new));

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.summary().logos()).hasSize(10).doesNotContain(logoOf("서비스10"));
        }

        @Test
        @DisplayName("upcomingPayments: 이번 주기에 점검 안 한 구독을 결제일 가까운 순 → 금액 큰 순으로 최대 5개 반환하고 dDay를 계산한다")
        void returnsUpcomingPaymentsNotCheckedThisCycle() {
            // given
            Subscription cheap = monthly("왓챠", 7900, today.plusDays(1));
            Subscription expensive = monthly("넷플릭스", 13500, today.plusDays(1));
            Subscription later = custom("동네 헬스장", 50000, today.plusDays(2));
            List<Subscription> rest = IntStream.range(0, 3)
                .mapToObj(i -> monthly("서비스" + i, 1000, today.plusDays(3 + i)))
                .toList();
            List<Subscription> all = new ArrayList<>(List.of(later, cheap, expensive));
            all.addAll(rest);
            givenSubscriptions(all.toArray(Subscription[]::new));

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.upcomingPayments())
                .extracting(HomeUpcomingPaymentResponse::serviceName)
                .containsExactly("넷플릭스", "왓챠", "동네 헬스장", "서비스0", "서비스1");
            HomeUpcomingPaymentResponse first = response.upcomingPayments().get(0);
            assertThat(first.subscriptionId()).isEqualTo(expensive.getId());
            assertThat(first.logoUrl()).isEqualTo(logoOf("넷플릭스"));
            assertThat(first.amount()).isEqualTo(13500);
            assertThat(first.nextBillingDate()).isEqualTo(today.plusDays(1));
            assertThat(first.dDay()).isEqualTo(1);
            assertThat(first.needsCheckup()).isTrue();
            assertThat(response.upcomingPayments().get(2).logoUrl()).isNull();
        }

        @Test
        @DisplayName("upcomingPayments: 이번 주기에 점검한 구독과 꾸준히 쓰는 구독은 빼고, 지난 주기에만 점검한 구독은 다시 넣는다")
        void excludesCheckedThisCycleAndSteadySubscriptions() {
            // given
            Subscription checkedThisCycle = monthly("넷플릭스", 13500, today.plusDays(1));
            Subscription checkedLastCycle = monthly("왓챠", 7900, today.plusDays(2));
            Subscription steady = monthly("유튜브 프리미엄", 14900, today.plusDays(3));
            steady.changeSteadyUse(true);
            givenSubscriptions(checkedThisCycle, checkedLastCycle, steady);
            givenChecks(
                check(checkedThisCycle, today.plusDays(1), 90, 1),
                check(checkedLastCycle, today.plusDays(2).minusMonths(1), 90, 1));

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.upcomingPayments())
                .extracting(HomeUpcomingPaymentResponse::serviceName)
                .containsExactly("왓챠");
        }

        @Test
        @DisplayName("usage: 최근 점검 활용도 60% 미만 · 유지하기 안 고른 구독의 개수 · 이번 달 결제 합계 · 연 금액과 활용도 낮은 순 3개를 반환한다")
        void returnsUnusedSubscriptions() {
            // given
            Subscription score59 = monthly("넷플릭스", 13500, today);
            Subscription score45 = monthly("왓챠", 7900, today);
            Subscription score20 = monthly("티빙", 13900, today);
            Subscription score10 = monthly("웨이브", 10900, today);
            Subscription score60 = monthly("디즈니플러스", 9900, today);
            Subscription keep = monthly("쿠팡플레이", 7890, today);
            Subscription steady = monthly("유튜브 프리미엄", 14900, today);
            steady.changeSteadyUse(true);
            SubscriptionCheck keepCheck = check(keep, today, 30, 1);
            keepCheck.keep(LocalDateTime.now());
            SubscriptionCheck canceledCheck = check(score59, today, 59, 1);
            canceledCheck.cancel(null, LocalDateTime.now());
            givenSubscriptions(score59, score45, score20, score10, score60, keep, steady);
            givenChecks(canceledCheck, check(score45, today, 45, 1),
                check(score20, today.minusMonths(1), 80, 1), check(score20, today, 20, 2),
                check(score10, today, 10, 1), check(score60, today, 60, 1), keepCheck,
                check(steady, today, 5, 1));

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            int targetsAmount = 13500 + 7900 + 13900 + 10900;
            assertThat(response.usage().hasCheckup()).isTrue();
            assertThat(response.usage().unusedCount()).isEqualTo(4);
            assertThat(response.usage().monthlyAmount()).isEqualTo(targetsAmount);
            assertThat(response.usage().yearlyAmount()).isEqualTo(targetsAmount * 12);
            assertThat(response.usage().items())
                .extracting(HomeUsageItemResponse::serviceName, HomeUsageItemResponse::scorePercent,
                    HomeUsageItemResponse::amount)
                .containsExactly(tuple("웨이브", 10, 10900), tuple("티빙", 20, 13900), tuple("왓챠", 45, 7900));
        }

        @Test
        @DisplayName("usage.nudgeSubscriptionId: 꾸준히 쓰는 구독을 뺀 구독 중 결제일이 가장 가까운 구독이다")
        void returnsNudgeSubscriptionExcludingSteady() {
            // given
            Subscription steady = monthly("유튜브 프리미엄", 14900, today.plusDays(1));
            steady.changeSteadyUse(true);
            Subscription nearest = monthly("넷플릭스", 13500, today.plusDays(2));
            Subscription later = monthly("왓챠", 7900, today.plusDays(5));
            givenSubscriptions(later, steady, nearest);

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.usage().nudgeSubscriptionId()).isEqualTo(nearest.getId());
            assertThat(response.usage().hasCheckup()).isFalse();
            assertThat(response.usage().unusedCount()).isZero();
        }

        @Test
        @DisplayName("usage.hasCheckup: 구독 종료 구독의 점검 기록도 점검 기록으로 본다")
        void returnsHasCheckupForEndedSubscription() {
            // given
            Subscription ended = monthly("멜론", 10900, today.plusDays(1));
            ended.cancel(today);
            givenSubscriptions(ended);
            givenChecks(check(ended, today.plusDays(1), 30, 1));

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.usage().hasCheckup()).isTrue();
            assertThat(response.usage().unusedCount()).isZero();
            assertThat(response.upcomingPayments()).isEmpty();
        }

        @Test
        @DisplayName("구독이 없으면 금액 · 개수는 0, 목록은 비어 있고 nudgeSubscriptionId는 null이다 (H-01)")
        void returnsEmptyHomeWhenNoSubscriptions() {
            // given
            givenSubscriptions();

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.summary().expectedAmount()).isZero();
            assertThat(response.summary().yearlyAmount()).isZero();
            assertThat(response.summary().subscriptionCount()).isZero();
            assertThat(response.summary().logos()).isEmpty();
            assertThat(response.upcomingPayments()).isEmpty();
            assertThat(response.usage().nudgeSubscriptionId()).isNull();
            assertThat(response.usage().hasCheckup()).isFalse();
            assertThat(response.usage().items()).isEmpty();
            verifyNoInteractions(subscriptionBillingHistoryGetService, subscriptionCheckGetService);
        }

        @Test
        @DisplayName("hasUnreadNotification: 읽지 않은 알림 여부를 그대로 반환한다")
        void returnsHasUnreadNotification() {
            // given
            givenSubscriptions();
            given(notificationGetService.hasUnreadNotification(eq(user), any(LocalDateTime.class))).willReturn(true);

            // when
            HomeResponse response = homeUseCase.getHome(userId);

            // then
            assertThat(response.hasUnreadNotification()).isTrue();
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 구독을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> homeUseCase.getHome(userId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }

        private void givenSubscriptions(Subscription... subscriptions) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscriptions(user)).willReturn(List.of(subscriptions));
        }

        private void givenChecks(SubscriptionCheck... checks) {
            given(subscriptionCheckGetService.findChecksBySubscriptions(anyCollection())).willReturn(List.of(checks));
        }

        private SubscriptionCheck check(Subscription subscription, LocalDate targetBillingOn, int scorePercent,
                                        int order) {
            SubscriptionCheck check = SubscriptionCheck.create(subscription, targetBillingOn,
                subscription.getCategory(), 3, 3, 3, scorePercent, CheckResultStage.of(scorePercent), null);
            ReflectionTestUtils.setField(check, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(check, "createdAt", today.atStartOfDay().minusDays(10).plusHours(order));
            return check;
        }

        private Subscription monthly(String name, int amount, LocalDate billingAnchorOn) {
            return subscription(name, amount, 1, BillingCycleUnit.MONTH, billingAnchorOn, true);
        }

        private Subscription custom(String name, int amount, LocalDate billingAnchorOn) {
            return subscription(name, amount, 1, BillingCycleUnit.MONTH, billingAnchorOn, false);
        }

        private Subscription subscription(String name, int amount, int cycleValue, BillingCycleUnit cycleUnit,
                                          LocalDate billingAnchorOn, boolean fromCatalog) {
            Subscription subscription;
            if (fromCatalog) {
                ServiceCatalog service = ServiceCatalog.create(name, null, null, Category.VIDEO_OTT, logoOf(name),
                    null, 1);
                ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
                subscription = Subscription.createFromCatalog(user, service, Category.VIDEO_OTT, amount, cycleValue,
                    cycleUnit, billingAnchorOn, PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            } else {
                subscription = Subscription.createCustom(user, name, Category.ETC, amount, cycleValue, cycleUnit,
                    billingAnchorOn, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            }
            LocalDate startedOn = today.minusMonths(1);
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", startedOn.atStartOfDay());
            ReflectionTestUtils.setField(subscription, "startedOn", startedOn);
            return subscription;
        }

        private String logoOf(String name) {
            return "https://logo/" + name + ".png";
        }
    }
}
