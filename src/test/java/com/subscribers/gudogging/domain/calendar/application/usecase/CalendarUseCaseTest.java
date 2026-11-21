package com.subscribers.gudogging.domain.calendar.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.calendar.application.dto.response.CalendarDayResponse;
import com.subscribers.gudogging.domain.calendar.application.dto.response.CalendarPaymentResponse;
import com.subscribers.gudogging.domain.calendar.application.dto.response.CalendarResponse;
import com.subscribers.gudogging.domain.calendar.application.exception.CalendarErrorCode;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarUseCase")
class CalendarUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private SubscriptionBillingHistoryGetService subscriptionBillingHistoryGetService;

    @InjectMocks
    private CalendarUseCase calendarUseCase;

    @Nested
    @DisplayName("getMonthlyCalendar")
    class GetMonthlyCalendar {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");

        @Test
        @DisplayName("조회 월의 결제일을 날짜별로 묶고 날짜별 합계를 반환한다")
        void groupsPaymentsByDate() {
            // given
            Subscription netflix = catalogSubscription("넷플릭스", 13500, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 10, 17), PaymentMethod.APP_STORE, LocalDate.of(2026, 9, 1));
            Subscription youtube = catalogSubscription("유튜브 프리미엄", 14900, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 10, 17), PaymentMethod.GOOGLE_PLAY, LocalDate.of(2026, 9, 1));
            Subscription notion = catalogSubscription("노션", 5000, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 10, 3), PaymentMethod.WEB_DIRECT, LocalDate.of(2026, 9, 1));
            givenSubscriptions(netflix, youtube, notion);

            // when
            CalendarResponse response = calendarUseCase.getMonthlyCalendar(userId, 2026, 10);

            // then
            assertThat(response.days())
                .extracting(CalendarDayResponse::date, CalendarDayResponse::totalAmount, day -> day.payments().size())
                .containsExactly(
                    tuple(LocalDate.of(2026, 10, 3), 5000, 1),
                    tuple(LocalDate.of(2026, 10, 17), 28400, 2));
            CalendarPaymentResponse payment = response.days().get(1).payments().get(0);
            assertThat(payment.subscriptionId()).isEqualTo(netflix.getId());
            assertThat(payment.serviceName()).isEqualTo("넷플릭스");
            assertThat(payment.logoUrl()).isEqualTo("https://logo/넷플릭스.png");
            assertThat(payment.amount()).isEqualTo(13500);
            assertThat(payment.billingCycleValue()).isEqualTo(1);
            assertThat(payment.billingCycleUnit()).isEqualTo(BillingCycleUnit.MONTH);
            assertThat(payment.paymentMethod()).isEqualTo(PaymentMethod.APP_STORE);
        }

        @Test
        @DisplayName("같은 날 결제는 금액 낮은 순, 금액이 같으면 서비스명 순으로 정렬하고 직접 입력 구독은 logoUrl이 null이다")
        void sortsPaymentsInDayByAmountThenName() {
            // given
            LocalDate billingOn = LocalDate.of(2026, 10, 17);
            LocalDate startedOn = LocalDate.of(2026, 9, 1);
            Subscription chatGpt = customSubscription("ChatGPT Plus", 29000, billingOn, startedOn);
            Subscription watcha = catalogSubscription("왓챠", 7900, 1, BillingCycleUnit.MONTH, billingOn,
                PaymentMethod.APP_STORE, startedOn);
            Subscription coupang = catalogSubscription("쿠팡플레이", 7900, 1, BillingCycleUnit.MONTH, billingOn,
                PaymentMethod.APP_STORE, startedOn);
            givenSubscriptions(chatGpt, watcha, coupang);

            // when
            CalendarResponse response = calendarUseCase.getMonthlyCalendar(userId, 2026, 10);

            // then
            assertThat(response.days()).hasSize(1);
            assertThat(response.days().get(0).payments())
                .extracting(CalendarPaymentResponse::serviceName, CalendarPaymentResponse::logoUrl)
                .containsExactly(
                    tuple("왓챠", "https://logo/왓챠.png"),
                    tuple("쿠팡플레이", "https://logo/쿠팡플레이.png"),
                    tuple("ChatGPT Plus", null));
        }

        @Test
        @DisplayName("미래 달은 결제 주기로 계산한 결제일을 반환한다 (월 · 주 · 연 주기)")
        void calculatesFutureBillingDatesByCycle() {
            // given
            LocalDate startedOn = LocalDate.of(2026, 3, 1);
            Subscription monthly = catalogSubscription("넷플릭스", 13500, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 10, 17), PaymentMethod.APP_STORE, startedOn);
            Subscription biweekly = catalogSubscription("밀리의 서재", 3000, 2, BillingCycleUnit.WEEK,
                LocalDate.of(2026, 10, 5), PaymentMethod.APP_STORE, startedOn);
            Subscription yearly = catalogSubscription("쿠팡 와우", 60000, 1, BillingCycleUnit.YEAR,
                LocalDate.of(2026, 3, 10), PaymentMethod.APP_STORE, startedOn);
            givenSubscriptions(monthly, biweekly, yearly);

            // when
            CalendarResponse response = calendarUseCase.getMonthlyCalendar(userId, 2027, 3);

            // then
            assertThat(response.days())
                .extracting(CalendarDayResponse::date)
                .containsExactly(
                    LocalDate.of(2027, 3, 8),
                    LocalDate.of(2027, 3, 10),
                    LocalDate.of(2027, 3, 17),
                    LocalDate.of(2027, 3, 22));
        }

        @Test
        @DisplayName("월말 기준일은 짧은 달에 말일로 계산하고 다음 달에는 기준일로 돌아간다")
        void calculatesMonthEndBillingDate() {
            // given
            Subscription subscription = catalogSubscription("넷플릭스", 13500, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2027, 1, 31), PaymentMethod.APP_STORE, LocalDate.of(2027, 1, 1));
            givenSubscriptions(subscription);

            // when
            CalendarResponse february = calendarUseCase.getMonthlyCalendar(userId, 2027, 2);
            CalendarResponse march = calendarUseCase.getMonthlyCalendar(userId, 2027, 3);

            // then
            assertThat(february.days()).extracting(CalendarDayResponse::date)
                .containsExactly(LocalDate.of(2027, 2, 28));
            assertThat(march.days()).extracting(CalendarDayResponse::date)
                .containsExactly(LocalDate.of(2027, 3, 31));
        }

        @Test
        @DisplayName("이용 시작일 전 결제는 반환하지 않는다 (재등록 전 결제 조건 이력 포함)")
        void excludesBillingsBeforeStartedOn() {
            // given
            Subscription subscription = catalogSubscription("넷플릭스", 13500, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 6, 10), PaymentMethod.APP_STORE, LocalDate.of(2026, 6, 1));
            SubscriptionBillingHistory beforeReactivation = SubscriptionBillingHistory.create(subscription, 9900, 1,
                BillingCycleUnit.MONTH, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 20));
            givenSubscriptions(subscription);
            given(subscriptionBillingHistoryGetService.findBillingHistoriesBySubscriptions(anyCollection()))
                .willReturn(List.of(beforeReactivation));

            // when
            CalendarResponse february = calendarUseCase.getMonthlyCalendar(userId, 2026, 2);
            CalendarResponse june = calendarUseCase.getMonthlyCalendar(userId, 2026, 6);

            // then
            assertThat(february.days()).isEmpty();
            assertThat(june.days()).extracting(CalendarDayResponse::date)
                .containsExactly(LocalDate.of(2026, 6, 10));
        }

        @Test
        @DisplayName("구독 종료 구독은 해지일 당일 결제까지 반환하고 이후 결제는 반환하지 않는다")
        void includesBillingsUntilCanceledOn() {
            // given
            Subscription subscription = catalogSubscription("밀리의 서재", 3000, 1, BillingCycleUnit.WEEK,
                LocalDate.of(2026, 11, 1), PaymentMethod.APP_STORE, LocalDate.of(2026, 10, 1));
            subscription.cancel(LocalDate.of(2026, 11, 15));
            givenSubscriptions(subscription);

            // when
            CalendarResponse november = calendarUseCase.getMonthlyCalendar(userId, 2026, 11);
            CalendarResponse december = calendarUseCase.getMonthlyCalendar(userId, 2026, 12);

            // then
            assertThat(november.days()).extracting(CalendarDayResponse::date)
                .containsExactly(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 15));
            assertThat(december.days()).isEmpty();
        }

        @Test
        @DisplayName("결제 조건을 수정한 구독은 수정 전 결제일에 수정 전 금액 · 주기를 반환한다")
        void usesBillingConditionOfEachDate() {
            // given
            Subscription subscription = catalogSubscription("넷플릭스", 17000, 1, BillingCycleUnit.MONTH,
                LocalDate.of(2026, 10, 5), PaymentMethod.APP_STORE, LocalDate.of(2026, 7, 1));
            SubscriptionBillingHistory before = SubscriptionBillingHistory.create(subscription, 13500, 1,
                BillingCycleUnit.MONTH, LocalDate.of(2026, 7, 5), LocalDate.of(2026, 9, 20));
            givenSubscriptions(subscription);
            given(subscriptionBillingHistoryGetService.findBillingHistoriesBySubscriptions(anyCollection()))
                .willReturn(List.of(before));

            // when
            CalendarResponse september = calendarUseCase.getMonthlyCalendar(userId, 2026, 9);
            CalendarResponse october = calendarUseCase.getMonthlyCalendar(userId, 2026, 10);

            // then
            assertThat(september.days().get(0).payments())
                .extracting(CalendarPaymentResponse::amount).containsExactly(13500);
            assertThat(september.days()).extracting(CalendarDayResponse::date)
                .containsExactly(LocalDate.of(2026, 9, 5));
            assertThat(october.days().get(0).payments())
                .extracting(CalendarPaymentResponse::amount).containsExactly(17000);
        }

        @Test
        @DisplayName("currentMonthTotal은 조회 월과 관계없이 오늘이 속한 달 1일~말일 합계다")
        void returnsCurrentMonthTotalRegardlessOfTargetMonth() {
            // given
            YearMonth thisMonth = YearMonth.now();
            Subscription subscription = catalogSubscription("넷플릭스", 13500, 1, BillingCycleUnit.MONTH,
                thisMonth.atDay(1), PaymentMethod.APP_STORE, thisMonth.atDay(1));
            givenSubscriptions(subscription);

            // when
            CalendarResponse response = calendarUseCase.getMonthlyCalendar(userId, 2024, 1);

            // then
            assertThat(response.days()).isEmpty();
            assertThat(response.currentMonthTotal()).isEqualTo(13500);
        }

        @Test
        @DisplayName("결제가 없으면 days는 빈 목록이고 이번 달 합계는 0이다")
        void returnsEmptyWhenNoSubscriptions() {
            // given
            givenSubscriptions();

            // when
            CalendarResponse response = calendarUseCase.getMonthlyCalendar(userId, 2026, 10);

            // then
            assertThat(response.days()).isEmpty();
            assertThat(response.currentMonthTotal()).isZero();
            verifyNoInteractions(subscriptionBillingHistoryGetService);
        }

        @Test
        @DisplayName("조회 가능 범위 경계(2024년 1월, 2028년 12월)는 조회할 수 있다")
        void allowsBoundaryMonths() {
            // given
            givenSubscriptions();

            // when
            CalendarResponse first = calendarUseCase.getMonthlyCalendar(userId, 2024, 1);
            CalendarResponse last = calendarUseCase.getMonthlyCalendar(userId, 2028, 12);

            // then
            assertThat(first.days()).isEmpty();
            assertThat(last.days()).isEmpty();
        }

        @ParameterizedTest(name = "{0}년 {1}월 → INVALID_CALENDAR_MONTH")
        @CsvSource({"2023, 12", "2029, 1", "2026, 0", "2026, 13"})
        @DisplayName("year가 2024~2028, month가 1~12 밖이면 INVALID_CALENDAR_MONTH 예외를 던지고 조회하지 않는다")
        void throwsWhenMonthOutOfRange(int year, int month) {
            // when & then
            assertThatThrownBy(() -> calendarUseCase.getMonthlyCalendar(userId, year, month))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CalendarErrorCode.INVALID_CALENDAR_MONTH);
            verifyNoInteractions(userGetService, subscriptionGetService);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 구독을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> calendarUseCase.getMonthlyCalendar(userId, 2026, 10))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }

        private void givenSubscriptions(Subscription... subscriptions) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscriptions(user)).willReturn(List.of(subscriptions));
        }

        private Subscription catalogSubscription(String name, int amount, int cycleValue, BillingCycleUnit cycleUnit,
                                                 LocalDate billingAnchorOn, PaymentMethod paymentMethod,
                                                 LocalDate startedOn) {
            ServiceCatalog service = ServiceCatalog.create(name, null, null, Category.VIDEO_OTT,
                "https://logo/" + name + ".png", null, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            Subscription subscription = Subscription.createFromCatalog(user, service, Category.VIDEO_OTT, amount,
                cycleValue, cycleUnit, billingAnchorOn, paymentMethod, RegistrationSource.MANUAL);
            return persisted(subscription, startedOn);
        }

        private Subscription customSubscription(String name, int amount, LocalDate billingAnchorOn,
                                                LocalDate startedOn) {
            Subscription subscription = Subscription.createCustom(user, name, Category.PRODUCTIVITY_AI, amount, 1,
                BillingCycleUnit.MONTH, billingAnchorOn, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            return persisted(subscription, startedOn);
        }

        private Subscription persisted(Subscription subscription, LocalDate startedOn) {
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", startedOn.atStartOfDay());
            ReflectionTestUtils.setField(subscription, "startedOn", startedOn);
            return subscription;
        }
    }
}
