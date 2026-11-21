package com.subscribers.gudogging.domain.check.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleDiscount;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleDiscountTarget;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleProviderType;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleRecommendType;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.BundleGetService;
import com.subscribers.gudogging.domain.check.application.dto.request.CheckupAnswerRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.DecideCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.SubmitCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.response.CategorySpendingItemResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CategorySpendingResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupBundleResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupDecisionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupOptionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupQuestionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupResultResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupSubmissionResponse;
import com.subscribers.gudogging.domain.check.application.exception.CheckErrorCode;
import com.subscribers.gudogging.domain.check.domain.entity.CancelReason;
import com.subscribers.gudogging.domain.check.domain.entity.CheckDecision;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheckCancelReason;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import com.subscribers.gudogging.domain.check.domain.result.CheckScore;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCancelReasonCreateService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCancelReasonGetService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCreateService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionErrorCode;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.RegistrationSource;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckUseCase")
class CheckUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @Mock
    private BundleGetService bundleGetService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private SubscriptionCheckGetService subscriptionCheckGetService;

    @Mock
    private SubscriptionCheckCreateService subscriptionCheckCreateService;

    @Mock
    private SubscriptionCheckCancelReasonGetService subscriptionCheckCancelReasonGetService;

    @Mock
    private SubscriptionCheckCancelReasonCreateService subscriptionCheckCancelReasonCreateService;

    @InjectMocks
    private CheckUseCase checkUseCase;

    @Captor
    private ArgumentCaptor<SubscriptionCheck> checkCaptor;

    @Captor
    private ArgumentCaptor<List<SubscriptionCheckCancelReason>> cancelReasonsCaptor;

    @Nested
    @DisplayName("getCheckupForm")
    class GetCheckupForm {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();
        private final LocalDate nextBillingDate = today.plusDays(10);

        private Subscription fromCatalog(String name, Category category, int amount, BillingCycleUnit cycleUnit) {
            ServiceCatalog service = ServiceCatalog.create(name, null, null, category,
                "https://logo.example.com/" + name + ".png", null, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            return persisted(Subscription.createFromCatalog(user, service, category, amount, 1, cycleUnit,
                nextBillingDate, PaymentMethod.APP_STORE, RegistrationSource.MANUAL));
        }

        private Subscription custom(String name, Category category) {
            return persisted(Subscription.createCustom(user, name, category, 9900, 1, BillingCycleUnit.MONTH,
                nextBillingDate, PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL));
        }

        // id · createdAt · startedOn은 JPA 저장 시(@PrePersist) 채워지므로 테스트에서만 지정한다.
        private Subscription persisted(Subscription subscription) {
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", today.minusMonths(2).atStartOfDay());
            ReflectionTestUtils.setField(subscription, "startedOn", today.minusMonths(2));
            return subscription;
        }

        private CheckupFormResponse formOf(Subscription subscription) {
            return formOf(subscription, null);
        }

        private CheckupFormResponse formOf(Subscription subscription, SubscriptionCheck latestCheck) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscription.getId()))
                .willReturn(Optional.of(subscription));
            given(subscriptionCheckGetService.findLatestCheck(subscription)).willReturn(
                Optional.ofNullable(latestCheck));
            return checkUseCase.getCheckupForm(userId, subscription.getId());
        }

        private CheckupQuestionResponse questionOf(CheckupFormResponse form, CheckupQuestionType type) {
            return form.questions().stream()
                .filter(question -> question.type() == type)
                .findFirst()
                .orElseThrow();
        }

        private List<String> optionTexts(CheckupQuestionResponse question) {
            return question.options().stream().map(CheckupOptionResponse::text).toList();
        }

        private String monthDay(LocalDate date) {
            return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
        }

        @Test
        @DisplayName("문항 3개를 USAGE → SATISFACTION → PLAN 순으로, 각 선택지 5개를 score 1~5로 반환한다")
        void returnsThreeQuestionsWithFiveOptions() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            assertThat(result.questions()).extracting(CheckupQuestionResponse::type)
                .containsExactly(CheckupQuestionType.USAGE, CheckupQuestionType.SATISFACTION, CheckupQuestionType.PLAN);
            assertThat(result.questions()).allSatisfy(question ->
                assertThat(question.options()).extracting(CheckupOptionResponse::score).containsExactly(1, 2, 3, 4, 5));
        }

        @Test
        @DisplayName("영상 · OTT 구독이면 영상 문구 · 선택지와 메모 예시 문구 · 칩, 구독 정보를 반환한다")
        void returnsVideoOttForm() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            CheckupQuestionResponse usage = questionOf(result, CheckupQuestionType.USAGE);
            assertThat(usage.label()).isEqualTo("최근 한 달 동안 넷플릭스를 얼마나 자주 이용했나요?");
            assertThat(usage.hint()).isEqualTo("에피소드, 영화 모두 1편으로 세어 주세요");
            assertThat(optionTexts(usage)).containsExactly("안 봤어요", "1~2편", "3~5편", "6~10편", "11편 이상");
            CheckupQuestionResponse satisfaction = questionOf(result, CheckupQuestionType.SATISFACTION);
            assertThat(satisfaction.label()).isEqualTo("넷플릭스에서 본 콘텐츠, 구독료만큼 가치 있었나요?");
            assertThat(optionTexts(satisfaction)).containsExactly("많이 아까워요", "조금 아까워요", "보통이에요",
                "구독료만큼 가치 있어요", "구독료보다 더 가치 있어요");
            CheckupQuestionResponse plan = questionOf(result, CheckupQuestionType.PLAN);
            assertThat(plan.label()).isEqualTo("다음 달에도 넷플릭스를 이용할 계획이 있나요?");
            assertThat(optionTexts(plan)).containsExactly("안 쓸 거예요", "거의 안 쓸 것 같아요", "아직 잘 모르겠어요",
                "아마 계속 쓸 것 같아요", "꼭 계속 쓸 거예요");
            assertThat(result.category()).isEqualTo(Category.VIDEO_OTT);
            assertThat(result.serviceName()).isEqualTo("넷플릭스");
            assertThat(result.nextBillingDate()).isEqualTo(nextBillingDate);
            assertThat(result.memoPlaceholder()).isEqualTo("예: 시즌 3 공개돼서 이번 달은 유지할래요");
            assertThat(result.memoChips()).containsExactly("보고 싶은 작품이 있어요", "이번 달은 바빴어요", "비싸지면 해지할래요");
        }

        @Test
        @DisplayName("서비스명 받침에 맞춰 을/를을 붙인다")
        void appliesObjectParticle() {
            // given
            Subscription tving = fromCatalog("티빙", Category.VIDEO_OTT, 13900, BillingCycleUnit.MONTH);

            // when
            CheckupFormResponse result = formOf(tving);

            // then
            assertThat(questionOf(result, CheckupQuestionType.USAGE).label())
                .isEqualTo("최근 한 달 동안 티빙을 얼마나 자주 이용했나요?");
            assertThat(questionOf(result, CheckupQuestionType.PLAN).label())
                .isEqualTo("다음 달에도 티빙을 이용할 계획이 있나요?");
        }

        @Test
        @DisplayName("음악 · 교육 만족도 문구는 받침에 맞춰 으로/로를 붙이고, ㄹ받침은 로를 붙인다")
        void appliesInstrumentalParticle() {
            // given
            Subscription melon = fromCatalog("멜론", Category.MUSIC, 10900, BillingCycleUnit.MONTH);
            Subscription bugs = fromCatalog("벅스", Category.MUSIC, 8900, BillingCycleUnit.MONTH);
            Subscription mySchool = custom("마이스쿨", Category.EDUCATION);

            // when
            CheckupFormResponse melonForm = formOf(melon);
            CheckupFormResponse bugsForm = formOf(bugs);
            CheckupFormResponse classForm = formOf(mySchool);

            // then
            assertThat(questionOf(melonForm, CheckupQuestionType.SATISFACTION).label())
                .isEqualTo("멜론으로 들은 음악, 구독료만큼 가치 있었나요?");
            assertThat(questionOf(bugsForm, CheckupQuestionType.SATISFACTION).label())
                .isEqualTo("벅스로 들은 음악, 구독료만큼 가치 있었나요?");
            assertThat(questionOf(classForm, CheckupQuestionType.SATISFACTION).label())
                .isEqualTo("마이스쿨로 한 공부, 구독료만큼 가치 있었나요?");
        }

        @Test
        @DisplayName("월 결제 구독은 만족도 보조 문구에 월 · 연 환산 금액, 계획 보조 문구에 다음 결제일 · 결제 금액을 쉼표 형식으로 채운다")
        void fillsMonthlyAmountHints() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            assertThat(questionOf(result, CheckupQuestionType.SATISFACTION).hint())
                .isEqualTo("월 13,500원 · 1년이면 162,000원");
            assertThat(questionOf(result, CheckupQuestionType.PLAN).hint())
                .isEqualTo("다음 결제 " + monthDay(nextBillingDate) + " · 13,500원");
            assertThat(result.amount()).isEqualTo(13500);
            assertThat(result.yearlyAmount()).isEqualTo(162000);
        }

        @Test
        @DisplayName("연 결제 구독은 만족도 '월 ○원'에 월 환산 금액, 계획 '다음 결제 · ○원'에 1회 결제 금액이 들어간다")
        void usesBillingAmountForPlanHintOfYearlySubscription() {
            // given
            Subscription duolingo = fromCatalog("듀오링고", Category.EDUCATION, 120000, BillingCycleUnit.YEAR);

            // when
            CheckupFormResponse result = formOf(duolingo);

            // then
            assertThat(questionOf(result, CheckupQuestionType.SATISFACTION).hint())
                .isEqualTo("월 10,000원 · 1년이면 120,000원");
            assertThat(questionOf(result, CheckupQuestionType.PLAN).hint())
                .isEqualTo("다음 결제 " + monthDay(nextBillingDate) + " · 120,000원");
            assertThat(result.amount()).isEqualTo(10000);
            assertThat(result.yearlyAmount()).isEqualTo(120000);
        }

        @Test
        @DisplayName("가장 최근 점검의 메모를 lastMemo로 반환한다")
        void returnsLatestMemo() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);
            SubscriptionCheck latestCheck = SubscriptionCheck.create(subscription, today.minusMonths(1),
                Category.VIDEO_OTT, 4, 4, 5, 87, CheckResultStage.KEEP_RECOMMENDED, "시즌 3까지만 볼래요");

            // when
            CheckupFormResponse result = formOf(subscription, latestCheck);

            // then
            assertThat(result.lastMemo()).isEqualTo("시즌 3까지만 볼래요");
        }

        @Test
        @DisplayName("점검 기록이 없으면 lastMemo는 null이다")
        void returnsNullLastMemoWithoutCheck() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            assertThat(result.lastMemo()).isNull();
        }

        @Test
        @DisplayName("기타 구독의 이용도 보조 문구는 두 줄이고 기타 메모 예시 문구 · 칩을 반환한다")
        void returnsEtcForm() {
            // given
            Subscription subscription = custom("동네 헬스장", Category.ETC);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            assertThat(questionOf(result, CheckupQuestionType.USAGE).hint())
                .isEqualTo("하루에 여러 번 써도 1일로 세어 주세요\n자동으로 쓴 날도 포함해요");
            assertThat(questionOf(result, CheckupQuestionType.SATISFACTION).label())
                .isEqualTo("동네 헬스장, 구독료만큼 가치 있었나요?");
            assertThat(result.memoPlaceholder()).isEqualTo("예: 사진 백업용이라 계속 써야 해요");
            assertThat(result.memoChips()).containsExactly("자동으로 쓰고 있어요", "이번 달은 안 썼어요", "비싸지면 해지할래요");
        }

        @Test
        @DisplayName("쇼핑 · 배달 구독이면 쇼핑 이용도 문구와 만족도 문구를 반환한다")
        void returnsShoppingForm() {
            // given
            Subscription subscription = fromCatalog("쿠팡 와우", Category.SHOPPING_DELIVERY, 7890,
                BillingCycleUnit.MONTH);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            CheckupQuestionResponse usage = questionOf(result, CheckupQuestionType.USAGE);
            assertThat(usage.hint()).isEqualTo("주문, 배달, 혜택 사용을 모두 1번으로 세어 주세요");
            assertThat(optionTexts(usage)).containsExactly("안 썼어요", "1~2번", "3~5번", "6~9번", "10번 이상");
            assertThat(questionOf(result, CheckupQuestionType.SATISFACTION).label())
                .isEqualTo("쿠팡 와우 혜택으로 아낀 돈, 구독료만큼 됐나요?");
        }

        @Test
        @DisplayName("꾸준히 쓰는 구독도 문항을 반환한다")
        void returnsFormForSteadySubscription() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);
            subscription.changeSteadyUse(true);

            // when
            CheckupFormResponse result = formOf(subscription);

            // then
            assertThat(result.questions()).hasSize(3);
        }

        @Test
        @DisplayName("구독 종료된 구독이면 CHECK_SUBSCRIPTION_NOT_ACTIVE 예외를 던진다")
        void throwsWhenSubscriptionEnded() {
            // given
            Subscription subscription = fromCatalog("넷플릭스", Category.VIDEO_OTT, 13500, BillingCycleUnit.MONTH);
            subscription.cancel(today.minusDays(3));
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscription.getId()))
                .willReturn(Optional.of(subscription));

            // when & then
            assertThatThrownBy(() -> checkUseCase.getCheckupForm(userId, subscription.getId()))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CheckErrorCode.CHECK_SUBSCRIPTION_NOT_ACTIVE);
            verifyNoInteractions(subscriptionCheckGetService);
        }

        @Test
        @DisplayName("구독이 없거나 다른 회원 것이면 SUBSCRIPTION_NOT_FOUND 예외를 던진다")
        void throwsWhenSubscriptionNotFound() {
            // given
            UUID subscriptionId = UUID.randomUUID();
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscriptionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.getCheckupForm(userId, subscriptionId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 구독을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.getCheckupForm(userId, UUID.randomUUID()))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }
    }

    @Nested
    @DisplayName("submitCheckup")
    class SubmitCheckup {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();
        private final LocalDate nextBillingDate = today.plusDays(10);

        private Subscription netflix() {
            ServiceCatalog service = ServiceCatalog.create("넷플릭스", null, null, Category.VIDEO_OTT,
                "https://logo.example.com/netflix.png", null, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            Subscription subscription = Subscription.createFromCatalog(user, service, Category.VIDEO_OTT, 13500, 1,
                BillingCycleUnit.MONTH, nextBillingDate, PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            // id · createdAt · startedOn은 JPA 저장 시(@PrePersist) 채워지므로 테스트에서만 지정한다.
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", today.minusMonths(2).atStartOfDay());
            ReflectionTestUtils.setField(subscription, "startedOn", today.minusMonths(2));
            return subscription;
        }

        private SubmitCheckupRequest request(int usage, int satisfaction, int plan, String memo) {
            return new SubmitCheckupRequest(List.of(
                new CheckupAnswerRequest(CheckupQuestionType.USAGE, usage),
                new CheckupAnswerRequest(CheckupQuestionType.SATISFACTION, satisfaction),
                new CheckupAnswerRequest(CheckupQuestionType.PLAN, plan)
            ), memo);
        }

        private void givenSubscription(Subscription subscription) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscription.getId()))
                .willReturn(Optional.of(subscription));
        }

        // 저장 시 id가 생기므로 저장한 점검에 id를 붙여 돌려준다.
        private void givenCheckSaved() {
            given(subscriptionCheckCreateService.createCheck(any())).willAnswer(invocation -> {
                SubscriptionCheck check = invocation.getArgument(0);
                ReflectionTestUtils.setField(check, "id", UUID.randomUUID());
                return check;
            });
        }

        private SubscriptionCheck submit(Subscription subscription, SubmitCheckupRequest request) {
            givenSubscription(subscription);
            givenCheckSaved();
            checkUseCase.submitCheckup(userId, subscription.getId(), request);
            verify(subscriptionCheckCreateService).createCheck(checkCaptor.capture());
            return checkCaptor.getValue();
        }

        @Test
        @DisplayName("응답 · 메모 · 활용도 · 단계 · 점검 대상 결제일 · 카테고리를 담아 점검을 저장한다")
        void savesCheck() {
            // given
            Subscription subscription = netflix();

            // when
            SubscriptionCheck saved = submit(subscription, request(4, 5, 5, "볼 작품 있음"));

            // then
            assertThat(saved.getSubscription()).isSameAs(subscription);
            assertThat(saved.getUsageScore()).isEqualTo(4);
            assertThat(saved.getSatisfactionScore()).isEqualTo(5);
            assertThat(saved.getPlanScore()).isEqualTo(5);
            assertThat(saved.getScorePercent()).isEqualTo(90);
            assertThat(saved.getStage()).isEqualTo(CheckResultStage.KEEP_RECOMMENDED);
            assertThat(saved.getMemo()).isEqualTo("볼 작품 있음");
            assertThat(saved.getTargetBillingOn()).isEqualTo(nextBillingDate);
            assertThat(saved.getCategory()).isEqualTo(Category.VIDEO_OTT);
            assertThat(saved.getDecision()).isNull();
        }

        @Test
        @DisplayName("응답으로 저장한 점검 ID만 반환한다")
        void returnsCheckupId() {
            // given
            Subscription subscription = netflix();
            givenSubscription(subscription);
            givenCheckSaved();

            // when
            CheckupSubmissionResponse result = checkUseCase.submitCheckup(userId, subscription.getId(),
                request(4, 5, 5, null));

            // then
            verify(subscriptionCheckCreateService).createCheck(checkCaptor.capture());
            assertThat(result.checkupId()).isNotNull().isEqualTo(checkCaptor.getValue().getId());
        }

        @ParameterizedTest(name = "이용도 {0} · 만족도 {1} · 계획 {2} → 활용도 {3}, {4}")
        @CsvSource({
            "5, 5, 5, 100, KEEP_RECOMMENDED",
            "2, 3, 2, 31, CANCEL_RECOMMENDED",
            "3, 3, 3, 50, CANCEL_CONSIDER",
            "3, 5, 4, 71, KEEP_CONSIDER",
            "4, 4, 4, 75, KEEP_CONSIDER",
            "1, 1, 2, 9, CANCEL_RECOMMENDED"
        })
        @DisplayName("활용도를 공식대로 계산해 반올림하고 구간별 단계로 매핑한다")
        void calculatesScoreAndStage(int usage, int satisfaction, int plan, int expectedPercent,
                                     CheckResultStage expectedStage) {
            // when
            SubscriptionCheck saved = submit(netflix(), request(usage, satisfaction, plan, null));

            // then
            assertThat(saved.getScorePercent()).isEqualTo(expectedPercent);
            assertThat(saved.getStage()).isEqualTo(expectedStage);
        }

        @ParameterizedTest(name = "이용도 {0} · 만족도 {1} · 계획 {2} → 활용도 {3}, {4}")
        @CsvSource({
            "3, 5, 5, 80, KEEP_RECOMMENDED",
            "5, 3, 4, 79, KEEP_CONSIDER",
            "1, 5, 5, 60, KEEP_CONSIDER",
            "3, 3, 4, 59, CANCEL_CONSIDER",
            "2, 3, 3, 40, CANCEL_CONSIDER",
            "1, 3, 4, 39, CANCEL_RECOMMENDED"
        })
        @DisplayName("활용도 구간 경계(80 · 79 · 60 · 59 · 40 · 39)에서 단계가 바뀐다")
        void mapsStageAtBoundaries(int usage, int satisfaction, int plan, int expectedPercent,
                                   CheckResultStage expectedStage) {
            // when
            SubscriptionCheck saved = submit(netflix(), request(usage, satisfaction, plan, null));

            // then
            assertThat(saved.getScorePercent()).isEqualTo(expectedPercent);
            assertThat(saved.getStage()).isEqualTo(expectedStage);
        }

        @Test
        @DisplayName("계획이 1점이면 활용도를 59로 제한한다")
        void capsScoreWhenPlanIsLowest() {
            // when
            SubscriptionCheck saved = submit(netflix(), request(5, 5, 1, null));

            // then
            assertThat(saved.getScorePercent()).isEqualTo(59);
            assertThat(saved.getStage()).isEqualTo(CheckResultStage.CANCEL_CONSIDER);
        }

        @Test
        @DisplayName("응답 순서가 달라도 type 기준으로 계산한다")
        void readsAnswersByType() {
            // given
            SubmitCheckupRequest request = new SubmitCheckupRequest(List.of(
                new CheckupAnswerRequest(CheckupQuestionType.PLAN, 2),
                new CheckupAnswerRequest(CheckupQuestionType.USAGE, 5),
                new CheckupAnswerRequest(CheckupQuestionType.SATISFACTION, 1)
            ), null);

            // when
            SubscriptionCheck saved = submit(netflix(), request);

            // then
            assertThat(saved.getUsageScore()).isEqualTo(5);
            assertThat(saved.getSatisfactionScore()).isEqualTo(1);
            assertThat(saved.getPlanScore()).isEqualTo(2);
            assertThat(saved.getScorePercent()).isEqualTo(49);
        }

        @Test
        @DisplayName("메모는 앞뒤 공백을 지우고 저장한다")
        void stripsMemo() {
            // when
            SubscriptionCheck trimmed = submit(netflix(), request(3, 3, 3, "  시즌 3까지만  "));

            // then
            assertThat(trimmed.getMemo()).isEqualTo("시즌 3까지만");
        }

        @Test
        @DisplayName("메모가 공백뿐이면 null로 저장한다")
        void savesBlankMemoAsNull() {
            // when
            SubscriptionCheck saved = submit(netflix(), request(3, 3, 3, "   "));

            // then
            assertThat(saved.getMemo()).isNull();
        }

        @Test
        @DisplayName("꾸준히 쓰는 구독도 제출할 수 있다")
        void submitsForSteadySubscription() {
            // given
            Subscription subscription = netflix();
            subscription.changeSteadyUse(true);

            // when
            SubscriptionCheck saved = submit(subscription, request(4, 4, 4, null));

            // then
            assertThat(saved.getScorePercent()).isEqualTo(75);
        }

        @Test
        @DisplayName("응답 type이 중복되거나 빠지면 INVALID_CHECKUP_ANSWERS 예외를 던지고 저장하지 않는다")
        void throwsWhenAnswerTypesInvalid() {
            // given
            Subscription subscription = netflix();
            givenSubscription(subscription);
            SubmitCheckupRequest request = new SubmitCheckupRequest(List.of(
                new CheckupAnswerRequest(CheckupQuestionType.USAGE, 4),
                new CheckupAnswerRequest(CheckupQuestionType.USAGE, 5),
                new CheckupAnswerRequest(CheckupQuestionType.PLAN, 5)
            ), null);

            // when & then
            assertThatThrownBy(() -> checkUseCase.submitCheckup(userId, subscription.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CheckErrorCode.INVALID_CHECKUP_ANSWERS);
            verify(subscriptionCheckCreateService, never()).createCheck(any());
        }

        @Test
        @DisplayName("구독 종료된 구독이면 CHECK_SUBSCRIPTION_NOT_ACTIVE 예외를 던지고 저장하지 않는다")
        void throwsWhenSubscriptionEnded() {
            // given
            Subscription subscription = netflix();
            subscription.cancel(today.minusDays(3));
            givenSubscription(subscription);

            // when & then
            assertThatThrownBy(() -> checkUseCase.submitCheckup(userId, subscription.getId(), request(4, 5, 5, null)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CheckErrorCode.CHECK_SUBSCRIPTION_NOT_ACTIVE);
            verifyNoInteractions(subscriptionCheckCreateService);
        }

        @Test
        @DisplayName("구독이 없거나 다른 회원 것이면 SUBSCRIPTION_NOT_FOUND 예외를 던진다")
        void throwsWhenSubscriptionNotFound() {
            // given
            UUID subscriptionId = UUID.randomUUID();
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findSubscription(user, subscriptionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.submitCheckup(userId, subscriptionId, request(4, 5, 5, null)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
            verifyNoInteractions(subscriptionCheckCreateService);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 구독을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.submitCheckup(userId, UUID.randomUUID(), request(4, 5, 5, null)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService, subscriptionCheckCreateService);
        }
    }

    @Nested
    @DisplayName("getCheckupResult")
    class GetCheckupResult {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();
        private final LocalDate nextBillingDate = today.plusDays(10);

        private Subscription subscription(String name, Category category, int amount, BillingCycleUnit cycleUnit) {
            ServiceCatalog service = ServiceCatalog.create(name, null, null, category,
                "https://logo.example.com/" + name + ".png", null, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            Subscription subscription = Subscription.createFromCatalog(user, service, category, amount, 1, cycleUnit,
                nextBillingDate, PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            // id · createdAt · startedOn은 JPA 저장 시(@PrePersist) 채워지므로 테스트에서만 지정한다.
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", today.minusMonths(2).atStartOfDay());
            ReflectionTestUtils.setField(subscription, "startedOn", today.minusMonths(2));
            return subscription;
        }

        private Subscription video(String name, int amount) {
            return subscription(name, Category.VIDEO_OTT, amount, BillingCycleUnit.MONTH);
        }

        private SubscriptionCheck check(Subscription subscription, int usage, int satisfaction, int plan,
                                        String memo) {
            int percent = CheckScore.percentOf(usage, satisfaction, plan);
            SubscriptionCheck check = SubscriptionCheck.create(subscription, nextBillingDate,
                subscription.getCategory(), usage, satisfaction, plan, percent, CheckResultStage.of(percent), memo);
            ReflectionTestUtils.setField(check, "id", UUID.randomUUID());
            return check;
        }

        private CheckupResultResponse resultOf(SubscriptionCheck check, Subscription... subscriptions) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionCheckGetService.findCheck(user, check.getId())).willReturn(Optional.of(check));
            given(subscriptionGetService.findSubscriptions(user)).willReturn(List.of(subscriptions));
            return checkUseCase.getCheckupResult(userId, check.getId());
        }

        private List<String> itemNames(CategorySpendingResponse spending) {
            return spending.items().stream().map(CategorySpendingItemResponse::serviceName).toList();
        }

        @Test
        @DisplayName("점검 · 구독 정보, 활용도 · 단계, 메모, 1년 비용, 다음 결제일을 반환한다")
        void returnsCheckupResult() {
            // given
            Subscription netflix = video("넷플릭스", 17000);
            SubscriptionCheck check = check(netflix, 1, 3, 3, "다음 달엔 안 볼 듯");

            // when
            CheckupResultResponse result = resultOf(check, netflix);

            // then
            assertThat(result.checkupId()).isEqualTo(check.getId());
            assertThat(result.subscriptionId()).isEqualTo(netflix.getId());
            assertThat(result.serviceName()).isEqualTo("넷플릭스");
            assertThat(result.nextBillingDate()).isEqualTo(nextBillingDate);
            assertThat(result.yearlyCost()).isEqualTo(204000);
            assertThat(result.score()).isEqualTo(30);
            assertThat(result.stage()).isEqualTo(CheckResultStage.CANCEL_RECOMMENDED);
            assertThat(result.memo()).isEqualTo("다음 달엔 안 볼 듯");
        }

        @Test
        @DisplayName("저장한 점수를 점검 당시 카테고리의 선택지 문구로 바꿔 반환한다")
        void returnsAnswerTexts() {
            // given
            Subscription netflix = video("넷플릭스", 13500);
            SubscriptionCheck check = check(netflix, 1, 3, 3, null);

            // when
            CheckupResultResponse result = resultOf(check, netflix);

            // then
            assertThat(result.usageAnswer()).isEqualTo("안 봤어요");
            assertThat(result.satisfactionAnswer()).isEqualTo("보통이에요");
            assertThat(result.planAnswer()).isEqualTo("아직 잘 모르겠어요");
        }

        @Test
        @DisplayName("점검한 요금제가 포함된 번들이 없으면 절약 제안은 빈 배열이다")
        void returnsEmptyBundlesWithoutCandidates() {
            // given
            Subscription netflix = video("넷플릭스", 13500);

            // when
            CheckupResultResponse result = resultOf(check(netflix, 4, 5, 5, null), netflix);

            // then
            assertThat(result.bundles()).isEmpty();
        }

        @ParameterizedTest(name = "이용도 {0} · 만족도 {1} · 계획 {2} → {3}")
        @CsvSource({
            "1, 3, 3, USAGE",
            "3, 1, 2, PLAN",
            "4, 1, 3, SATISFACTION",
            "4, 5, 5, PLAN",
            "5, 4, 3, USAGE",
            "5, 5, 1, PLAN"
        })
        @DisplayName("활용도 60 미만은 가중 감점, 60 이상은 가중 점수가 가장 큰 문항을 mainFactor로 반환하고 계획 1점이면 계획이다")
        void returnsMainFactor(int usage, int satisfaction, int plan, CheckupQuestionType expected) {
            // given
            Subscription netflix = video("넷플릭스", 13500);

            // when
            CheckupResultResponse result = resultOf(check(netflix, usage, satisfaction, plan, null), netflix);

            // then
            assertThat(result.mainFactor()).isEqualTo(expected);
        }

        @Test
        @DisplayName("같은 카테고리 구독 중 구독의 월 환산 합계 · 개수 · 목록(점검한 구독 맨 위, 이후 금액 큰 순)을 반환한다")
        void returnsCategorySpending() {
            // given
            Subscription netflix = video("넷플릭스", 13500);
            Subscription youtube = video("유튜브 프리미엄", 14900);
            Subscription tving = video("티빙", 13900);
            Subscription melon = subscription("멜론", Category.MUSIC, 10900, BillingCycleUnit.MONTH);

            // when
            CheckupResultResponse result = resultOf(check(netflix, 1, 3, 3, null), netflix, youtube, tving, melon);

            // then
            CategorySpendingResponse spending = result.categorySpending();
            assertThat(spending.category()).isEqualTo(Category.VIDEO_OTT);
            assertThat(spending.monthlyTotal()).isEqualTo(42300);
            assertThat(spending.subscriptionCount()).isEqualTo(3);
            assertThat(itemNames(spending)).containsExactly("넷플릭스", "유튜브 프리미엄", "티빙");
            assertThat(spending.items().get(0).logoUrl()).isEqualTo("https://logo.example.com/넷플릭스.png");
        }

        @Test
        @DisplayName("연 결제 구독은 월 환산해 합산하고, 구독 종료 구독은 카테고리 지출에서 뺀다")
        void convertsYearlyAndExcludesEnded() {
            // given
            Subscription netflix = video("넷플릭스", 13500);
            Subscription yearly = subscription("왓챠", Category.VIDEO_OTT, 120000, BillingCycleUnit.YEAR);
            Subscription ended = video("웨이브", 10900);
            ended.cancel(today.minusDays(5));

            // when
            CheckupResultResponse result = resultOf(check(netflix, 1, 3, 3, null), netflix, yearly, ended);

            // then
            CategorySpendingResponse spending = result.categorySpending();
            assertThat(spending.monthlyTotal()).isEqualTo(23500);
            assertThat(spending.subscriptionCount()).isEqualTo(2);
            assertThat(itemNames(spending)).containsExactly("넷플릭스", "왓챠");
        }

        @Test
        @DisplayName("같은 카테고리 구독이 6개면 6개 전부를 목록으로 반환하고 합계도 6개 전부다")
        void returnsAllSubscriptionsOverFive() {
            // given
            Subscription netflix = video("넷플릭스", 9000);
            Subscription a = video("디즈니+", 13900);
            Subscription b = video("유튜브 프리미엄", 14900);
            Subscription c = video("티빙", 13500);
            Subscription d = video("웨이브", 10900);
            Subscription e = video("왓챠", 7900);

            // when
            CheckupResultResponse result = resultOf(check(netflix, 1, 3, 3, null), netflix, a, b, c, d, e);

            // then
            CategorySpendingResponse spending = result.categorySpending();
            assertThat(spending.subscriptionCount()).isEqualTo(6);
            assertThat(itemNames(spending)).containsExactly("넷플릭스", "유튜브 프리미엄", "디즈니+", "티빙", "웨이브", "왓챠");
            assertThat(spending.monthlyTotal()).isEqualTo(70100);
        }

        @Test
        @DisplayName("구독 종료된 구독의 점검이면 다음 결제일은 null이고 점검한 구독은 카테고리 목록 맨 위에 포함된다")
        void returnsResultOfEndedSubscription() {
            // given
            Subscription netflix = video("넷플릭스", 13500);
            Subscription youtube = video("유튜브 프리미엄", 14900);
            SubscriptionCheck check = check(netflix, 1, 3, 3, null);
            netflix.cancel(today.minusDays(1));

            // when
            CheckupResultResponse result = resultOf(check, netflix, youtube);

            // then
            assertThat(result.nextBillingDate()).isNull();
            assertThat(itemNames(result.categorySpending())).containsExactly("넷플릭스", "유튜브 프리미엄");
            assertThat(result.categorySpending().monthlyTotal()).isEqualTo(28400);
        }

        @Test
        @DisplayName("점검이 없거나 다른 회원 것이면 CHECKUP_NOT_FOUND 예외를 던진다")
        void throwsWhenCheckupNotFound() {
            // given
            UUID checkupId = UUID.randomUUID();
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionCheckGetService.findCheck(user, checkupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.getCheckupResult(userId, checkupId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CheckErrorCode.CHECKUP_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 점검을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.getCheckupResult(userId, UUID.randomUUID()))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionCheckGetService, subscriptionGetService);
        }
    }

    @Nested
    @DisplayName("decideCheckup")
    class DecideCheckup {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();

        private SubscriptionCheck check() {
            ServiceCatalog service = ServiceCatalog.create("넷플릭스", null, null, Category.VIDEO_OTT,
                "https://logo.example.com/netflix.png", null, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            Subscription subscription = Subscription.createFromCatalog(user, service, Category.VIDEO_OTT, 13500, 1,
                BillingCycleUnit.MONTH, today.plusDays(10), PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            SubscriptionCheck check = SubscriptionCheck.create(subscription, today.plusDays(10), Category.VIDEO_OTT,
                1, 3, 3, 30, CheckResultStage.CANCEL_RECOMMENDED, null);
            // id는 JPA 저장 시 생성되므로 테스트에서만 지정한다.
            ReflectionTestUtils.setField(check, "id", UUID.randomUUID());
            return check;
        }

        private SubscriptionCheckCancelReason savedReason(SubscriptionCheck check, CancelReason reason,
                                                          boolean deleted) {
            SubscriptionCheckCancelReason cancelReason = SubscriptionCheckCancelReason.create(check, reason);
            ReflectionTestUtils.setField(cancelReason, "id", UUID.randomUUID());
            if (deleted) {
                cancelReason.softDelete();
            }
            return cancelReason;
        }

        private CheckupDecisionResponse decide(SubscriptionCheck check, DecideCheckupRequest request,
                                               SubscriptionCheckCancelReason... existing) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionCheckGetService.findCheck(user, check.getId())).willReturn(Optional.of(check));
            given(subscriptionCheckCancelReasonGetService.findAllCancelReasonsIncludingDeleted(check))
                .willReturn(List.of(existing));
            return checkUseCase.decideCheckup(userId, check.getId(), request);
        }

        private List<CancelReason> createdReasons() {
            verify(subscriptionCheckCancelReasonCreateService).createCancelReasons(cancelReasonsCaptor.capture());
            return cancelReasonsCaptor.getValue().stream().map(SubscriptionCheckCancelReason::getReason).toList();
        }

        @Test
        @DisplayName("유지하기면 KEEP과 결정 시각을 저장하고 점검 ID · 결정을 반환한다")
        void keeps() {
            // given
            SubscriptionCheck check = check();

            // when
            CheckupDecisionResponse result = decide(check, new DecideCheckupRequest(CheckDecision.KEEP, null, null));

            // then
            assertThat(check.getDecision()).isEqualTo(CheckDecision.KEEP);
            assertThat(check.getDecidedAt()).isNotNull();
            assertThat(result.checkupId()).isEqualTo(check.getId());
            assertThat(result.decision()).isEqualTo(CheckDecision.KEEP);
            verify(subscriptionCheckCancelReasonCreateService, never()).createCancelReasons(any());
        }

        @Test
        @DisplayName("해지하기면 CANCEL과 결정 시각, 고른 해지 이유를 저장한다")
        void cancelsWithReasons() {
            // given
            SubscriptionCheck check = check();

            // when
            CheckupDecisionResponse result = decide(check, new DecideCheckupRequest(CheckDecision.CANCEL,
                List.of(CancelReason.RARELY_USED, CancelReason.PRICE_BURDEN), null));

            // then
            assertThat(check.getDecision()).isEqualTo(CheckDecision.CANCEL);
            assertThat(check.getDecidedAt()).isNotNull();
            assertThat(createdReasons()).containsExactlyInAnyOrder(CancelReason.RARELY_USED, CancelReason.PRICE_BURDEN);
            assertThat(result.decision()).isEqualTo(CheckDecision.CANCEL);
        }

        @Test
        @DisplayName("직접 입력을 고르면 텍스트를 앞뒤 공백을 지워 저장한다")
        void savesCustomText() {
            // given
            SubscriptionCheck check = check();

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL,
                List.of(CancelReason.RARELY_USED, CancelReason.CUSTOM), "  요즘 잘 안 봐요  "));

            // then
            assertThat(check.getCancelReasonText()).isEqualTo("요즘 잘 안 봐요");
            assertThat(createdReasons()).containsExactlyInAnyOrder(CancelReason.RARELY_USED, CancelReason.CUSTOM);
        }

        @Test
        @DisplayName("건너뛰기(이유 없이 해지)면 해지 결정만 저장하고 해지 이유는 저장하지 않는다")
        void cancelsWithoutReasons() {
            // given
            SubscriptionCheck check = check();

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL, null, null));

            // then
            assertThat(check.getDecision()).isEqualTo(CheckDecision.CANCEL);
            assertThat(check.getCancelReasonText()).isNull();
            verify(subscriptionCheckCancelReasonCreateService, never()).createCancelReasons(any());
        }

        @Test
        @DisplayName("유지하기인데 해지 이유를 보내면 무시한다")
        void ignoresReasonsWhenKeep() {
            // given
            SubscriptionCheck check = check();

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.KEEP, List.of(CancelReason.CUSTOM), "이유"));

            // then
            assertThat(check.getCancelReasonText()).isNull();
            verify(subscriptionCheckCancelReasonCreateService, never()).createCancelReasons(any());
        }

        @Test
        @DisplayName("직접 입력을 고르지 않고 텍스트만 보내면 텍스트를 저장하지 않는다")
        void ignoresTextWithoutCustom() {
            // given
            SubscriptionCheck check = check();

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL, List.of(CancelReason.RARELY_USED), "이유"));

            // then
            assertThat(check.getCancelReasonText()).isNull();
            assertThat(createdReasons()).containsExactly(CancelReason.RARELY_USED);
        }

        @Test
        @DisplayName("직접 입력을 골랐는데 텍스트가 비어 있으면 직접 입력 이유만 저장하고 텍스트는 null이다")
        void savesCustomWithoutBlankText() {
            // given
            SubscriptionCheck check = check();

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL, List.of(CancelReason.CUSTOM), "   "));

            // then
            assertThat(check.getCancelReasonText()).isNull();
            assertThat(createdReasons()).containsExactly(CancelReason.CUSTOM);
        }

        @Test
        @DisplayName("같은 해지 이유가 중복으로 오면 한 번만 저장한다")
        void deduplicatesReasons() {
            // given
            SubscriptionCheck check = check();

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL,
                List.of(CancelReason.RARELY_USED, CancelReason.RARELY_USED), null));

            // then
            assertThat(createdReasons()).containsExactly(CancelReason.RARELY_USED);
        }

        @Test
        @DisplayName("해지 후 유지로 바꾸면 기존 해지 이유를 삭제하고 직접 입력 텍스트를 지운다")
        void switchesCancelToKeep() {
            // given
            SubscriptionCheck check = check();
            check.cancel("요즘 잘 안 봐요", today.atStartOfDay());
            SubscriptionCheckCancelReason rarelyUsed = savedReason(check, CancelReason.RARELY_USED, false);
            SubscriptionCheckCancelReason custom = savedReason(check, CancelReason.CUSTOM, false);

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.KEEP, null, null), rarelyUsed, custom);

            // then
            assertThat(check.getDecision()).isEqualTo(CheckDecision.KEEP);
            assertThat(check.getCancelReasonText()).isNull();
            assertThat(rarelyUsed.isDeleted()).isTrue();
            assertThat(custom.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("해지 이유를 바꾸면 빠진 이유는 삭제하고 남은 이유는 유지하며 새 이유만 새로 저장한다")
        void changesReasons() {
            // given
            SubscriptionCheck check = check();
            SubscriptionCheckCancelReason rarelyUsed = savedReason(check, CancelReason.RARELY_USED, false);
            SubscriptionCheckCancelReason priceBurden = savedReason(check, CancelReason.PRICE_BURDEN, false);

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL,
                List.of(CancelReason.PRICE_BURDEN, CancelReason.SWITCHING), null), rarelyUsed, priceBurden);

            // then
            assertThat(rarelyUsed.isDeleted()).isTrue();
            assertThat(priceBurden.isDeleted()).isFalse();
            assertThat(createdReasons()).containsExactly(CancelReason.SWITCHING);
        }

        @Test
        @DisplayName("예전에 지운 해지 이유를 다시 고르면 새로 저장하지 않고 되살린다")
        void restoresDeletedReason() {
            // given — 해지(잘 안 써요) → 유지로 바꿔 삭제됨 → 다시 해지(잘 안 써요)
            SubscriptionCheck check = check();
            SubscriptionCheckCancelReason rarelyUsed = savedReason(check, CancelReason.RARELY_USED, true);

            // when
            decide(check, new DecideCheckupRequest(CheckDecision.CANCEL, List.of(CancelReason.RARELY_USED), null),
                rarelyUsed);

            // then
            assertThat(rarelyUsed.isDeleted()).isFalse();
            assertThat(rarelyUsed.getDeletedAt()).isNull();
            verify(subscriptionCheckCancelReasonCreateService, never()).createCancelReasons(any());
        }

        @Test
        @DisplayName("점검이 없거나 다른 회원 것이면 CHECKUP_NOT_FOUND 예외를 던지고 저장하지 않는다")
        void throwsWhenCheckupNotFound() {
            // given
            UUID checkupId = UUID.randomUUID();
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionCheckGetService.findCheck(user, checkupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.decideCheckup(userId, checkupId,
                new DecideCheckupRequest(CheckDecision.KEEP, null, null)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(CheckErrorCode.CHECKUP_NOT_FOUND);
            verifyNoInteractions(subscriptionCheckCancelReasonGetService, subscriptionCheckCancelReasonCreateService);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 점검을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> checkUseCase.decideCheckup(userId, UUID.randomUUID(),
                new DecideCheckupRequest(CheckDecision.KEEP, null, null)))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionCheckGetService, subscriptionCheckCancelReasonGetService);
        }
    }

    @Nested
    @DisplayName("getCheckupResult — 절약 제안(bundles)")
    class GetCheckupResultBundles {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");
        private final LocalDate today = LocalDate.now();

        private Subscription plan(String serviceName, Category category, int amount) {
            ServiceCatalog service = ServiceCatalog.create(serviceName, null, null, category, null, amount, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            Subscription subscription = Subscription.createFromCatalog(user, service, category, amount, 1,
                BillingCycleUnit.MONTH, today.plusDays(10), PaymentMethod.APP_STORE, RegistrationSource.MANUAL);
            ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(subscription, "createdAt", today.minusMonths(2).atStartOfDay());
            ReflectionTestUtils.setField(subscription, "startedOn", today.minusMonths(2));
            return subscription;
        }

        private Bundle bundle(String name, BundleRecommendType type, BundleProviderType provider, Integer price,
                              List<String> fixed, List<String> choiceA, List<String> choiceB,
                              Map<String, Integer> appliedPrices, List<BundleDiscount> discounts) {
            return new Bundle(name, type, provider, price, null, name + " 태그1", name + " 태그2",
                fixed, choiceA, choiceB, appliedPrices, discounts);
        }

        private Bundle fixed(String name, BundleProviderType provider, int price, List<String> fixed) {
            return bundle(name, BundleRecommendType.FIXED, provider, price, fixed, List.of(), List.of(), Map.of(),
                List.of());
        }

        private Bundle card(String name, List<String> fixed, List<String> choiceA, List<String> choiceB,
                            List<BundleDiscount> discounts) {
            return bundle(name, BundleRecommendType.MAX, BundleProviderType.CARD, null, fixed, choiceA, choiceB,
                Map.of(), discounts);
        }

        private Bundle benefit(String name, int price, List<String> fixed) {
            return bundle(name, BundleRecommendType.BENEFIT, BundleProviderType.TELECOM, price, fixed, List.of(),
                List.of(), Map.of(), List.of());
        }

        private BundleDiscount percent(BundleDiscountTarget target, int percent) {
            return new BundleDiscount(target, null, percent, null);
        }

        private List<CheckupBundleResponse> bundlesOf(Subscription checked, List<Bundle> candidates,
                                                      Subscription... subscriptions) {
            SubscriptionCheck check = SubscriptionCheck.create(checked, today.plusDays(10), checked.getCategory(),
                2, 2, 2, CheckScore.percentOf(2, 2, 2), CheckResultStage.of(CheckScore.percentOf(2, 2, 2)), null);
            ReflectionTestUtils.setField(check, "id", UUID.randomUUID());
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionCheckGetService.findCheck(user, check.getId())).willReturn(Optional.of(check));
            given(subscriptionGetService.findSubscriptions(user)).willReturn(List.of(subscriptions));
            if (checked.getServiceCatalog() != null) {
                given(bundleGetService.findBundlesIncluding(checked.getServiceCatalog().getName()))
                    .willReturn(candidates);
            }
            return checkUseCase.getCheckupResult(userId, check.getId()).bundles();
        }

        @Test
        @DisplayName("확정 절약: 구독 중인 포함 서비스 합 − 상품 가격을 월 절약액으로, 절약액 ÷ 지금 내는 금액을 막대로 반환한다")
        void returnsFixedSaving() {
            // given — 넷플릭스 13,500 + 유튜브 14,900 = 28,400 vs 18,900 → 9,500 · 33%
            Subscription netflix = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            Subscription youtube = plan("YouTube Premium", Category.VIDEO_OTT, 14900);
            Bundle bundle = fixed("유독 더블 스트리밍", BundleProviderType.TELECOM, 18900,
                List.of("Netflix 스탠다드", "YouTube Premium"));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflix, List.of(bundle), netflix, youtube);

            // then
            assertThat(bundles).singleElement().satisfies(response -> {
                assertThat(response.name()).isEqualTo("유독 더블 스트리밍");
                assertThat(response.includes()).isEqualTo("Netflix 스탠다드 + YouTube Premium");
                assertThat(response.tags()).containsExactly("유독 더블 스트리밍 태그1", "유독 더블 스트리밍 태그2");
                assertThat(response.priceType()).isEqualTo(BundleRecommendType.FIXED);
                assertThat(response.monthlySaving()).isEqualTo(9500);
                assertThat(response.savingRate()).isEqualTo(33);
                assertThat(response.monthlyPrice()).isEqualTo(18900);
            });
        }

        @Test
        @DisplayName("확정 절약: 포함 서비스 일부만 구독 중이면 구독 중인 것만 더하고, 절약액이 0원 이하면 추천하지 않는다")
        void excludesWhenNotSaving() {
            // given — 유튜브만 구독 14,900 < 상품 15,990
            Subscription youtube = plan("YouTube Premium", Category.VIDEO_OTT, 14900);
            Bundle bundle = fixed("배민클럽 x 유튜브", BundleProviderType.PLATFORM, 15990,
                List.of("배달의민족 배민클럽", "YouTube Premium"));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(youtube, List.of(bundle), youtube);

            // then
            assertThat(bundles).isEmpty();
        }

        @Test
        @DisplayName("확정 절약: 택1은 구독 중인 것 중 가장 비싼 1개만 더하고 고정 포함 서비스는 함께 더한다")
        void addsMostExpensiveChoiceAndFixed() {
            // given — 고정 컬리 1,900 + 택1(넷플릭스 광고형 7,000 · 스포티파이 10,900 중 10,900) = 12,800 − 4,900 = 7,900
            Subscription kurly = plan("컬리멤버스", Category.SHOPPING_DELIVERY, 1900);
            Subscription netflixAd = plan("Netflix 광고형 스탠다드", Category.VIDEO_OTT, 7000);
            Subscription spotify = plan("Spotify Basic", Category.MUSIC, 10900);
            Bundle bundle = bundle("네이버플러스 멤버십", BundleRecommendType.FIXED, BundleProviderType.PLATFORM, 4900,
                List.of("컬리멤버스"), List.of("Netflix 광고형 스탠다드", "Spotify Basic"), List.of(), Map.of(), List.of());

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflixAd, List.of(bundle), kurly, netflixAd, spotify);

            // then
            assertThat(bundles).singleElement()
                .extracting(CheckupBundleResponse::monthlySaving)
                .isEqualTo(7900L);
        }

        @Test
        @DisplayName("확정 절약: 서비스별 적용가가 있으면 고른 요금제의 적용가를 상품 가격으로 쓴다")
        void usesAppliedPrice() {
            // given — 넷플릭스 스탠다드 13,500 vs KT 적용가 12,500 → 1,000
            Subscription netflix = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            Bundle bundle = bundle("넷플릭스 (KT 구독)", BundleRecommendType.FIXED, BundleProviderType.TELECOM, 6500,
                List.of(), List.of("Netflix 광고형 스탠다드", "Netflix 스탠다드"), List.of(),
                Map.of("Netflix 광고형 스탠다드", 6500, "Netflix 스탠다드", 12500), List.of());

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflix, List.of(bundle), netflix);

            // then — 같은 서비스 요금제 여러 개는 서비스 이름 하나로 줄인다
            assertThat(bundles).singleElement().satisfies(response -> {
                assertThat(response.monthlySaving()).isEqualTo(1000);
                assertThat(response.monthlyPrice()).isEqualTo(12500);
                assertThat(response.includes()).isEqualTo("Netflix 택1");
            });
        }

        @Test
        @DisplayName("카드: 구독 중인 포함 서비스 금액 × 할인율을 최대 절약액으로, 할인율을 막대로 반환한다")
        void returnsCardSaving() {
            // given — 넷플릭스 13,500 × 50% = 6,750
            Subscription netflix = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            Bundle bundle = card("하나 카드", List.of("Netflix 스탠다드"), List.of(), List.of(),
                List.of(percent(BundleDiscountTarget.ALL, 50)));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflix, List.of(bundle), netflix);

            // then
            assertThat(bundles).singleElement().satisfies(response -> {
                assertThat(response.priceType()).isEqualTo(BundleRecommendType.MAX);
                assertThat(response.monthlySaving()).isEqualTo(6750);
                assertThat(response.savingRate()).isEqualTo(50);
                assertThat(response.monthlyPrice()).isNull();
            });
        }

        @Test
        @DisplayName("카드: 택1 그룹은 가장 비싼 1개에만 할인하고, 그룹별 할인(A 50% / B 3,000원)을 각각 적용한다")
        void appliesDiscountPerChoiceGroup() {
            // given — A: 유튜브 14,900(가장 비쌈) × 50% = 7,450 / B: 쿠팡 7,890 → 3,000 → 10,450, 막대 = 10,450 ÷ 22,790
            Subscription netflixAd = plan("Netflix 광고형 스탠다드", Category.VIDEO_OTT, 7000);
            Subscription youtube = plan("YouTube Premium", Category.VIDEO_OTT, 14900);
            Subscription coupang = plan("쿠팡 와우 멤버십", Category.SHOPPING_DELIVERY, 7890);
            Bundle bundle = card("신한카드 국민행복", List.of(), List.of("Netflix 광고형 스탠다드", "YouTube Premium"),
                List.of("쿠팡 와우 멤버십"),
                List.of(percent(BundleDiscountTarget.A, 50), new BundleDiscount(BundleDiscountTarget.B, null, null,
                    3000)));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(youtube, List.of(bundle), netflixAd, youtube, coupang);

            // then
            assertThat(bundles).singleElement().satisfies(response -> {
                assertThat(response.monthlySaving()).isEqualTo(10450);
                assertThat(response.savingRate()).isEqualTo(45);
                assertThat(response.includes()).isEqualTo("Netflix 광고형 스탠다드 · YouTube Premium 등 택1");
            });
        }

        @Test
        @DisplayName("카드: 카테고리 할인은 고정 포함 서비스에만 적용하고 택1 그룹 서비스에는 적용하지 않는다")
        void appliesCategoryDiscountToFixedOnly() {
            // given — 고정 넷플릭스 13,500(영상) · 택1 유튜브 14,900(영상), 영상 50% → 넷플릭스만 6,750
            Subscription netflix = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            Subscription youtube = plan("YouTube Premium", Category.VIDEO_OTT, 14900);
            Bundle bundle = card("카테고리 카드", List.of("Netflix 스탠다드"), List.of("YouTube Premium"), List.of(),
                List.of(new BundleDiscount(BundleDiscountTarget.CATEGORY, Category.VIDEO_OTT, 50, null)));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflix, List.of(bundle), netflix, youtube);

            // then
            assertThat(bundles).singleElement()
                .extracting(CheckupBundleResponse::monthlySaving)
                .isEqualTo(6750L);
        }

        @Test
        @DisplayName("같은 요금제에 종료된 구독과 구독 중인 구독이 있으면 구독 중인 구독 금액으로 계산한다")
        void prefersActiveSubscriptionOfSamePlan() {
            // given — 점검한 구독은 종료된 넷플릭스(10,000), 새로 등록한 넷플릭스는 13,500 → 13,500 − 12,000 = 1,500
            Subscription ended = plan("Netflix 스탠다드", Category.VIDEO_OTT, 10000);
            ended.cancel(today);
            Subscription active = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            Bundle bundle = fixed("넷플릭스 할인", BundleProviderType.PLATFORM, 12000, List.of("Netflix 스탠다드"));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(ended, List.of(bundle), ended, active);

            // then
            assertThat(bundles).singleElement()
                .extracting(CheckupBundleResponse::monthlySaving)
                .isEqualTo(1500L);
        }

        @Test
        @DisplayName("통신 요금제: 계산 없이 monthlySaving · savingRate는 null, monthlyPrice는 요금제 가격이다")
        void returnsBenefitWithoutCalculation() {
            // given
            Subscription netflix = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            Bundle bundle = benefit("베스트 109(넷플릭스)", 109000, List.of("Netflix 스탠다드"));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflix, List.of(bundle), netflix);

            // then
            assertThat(bundles).singleElement().satisfies(response -> {
                assertThat(response.priceType()).isEqualTo(BundleRecommendType.BENEFIT);
                assertThat(response.monthlySaving()).isNull();
                assertThat(response.savingRate()).isNull();
                assertThat(response.monthlyPrice()).isEqualTo(109000);
            });
        }

        @Test
        @DisplayName("확정 절약(큰 순) → 카드 → 통신 요금제 순으로, 같은 제공사 유형은 2개까지 · 전체 4개까지 반환한다")
        void ordersAndLimits() {
            // given
            Subscription netflix = plan("Netflix 스탠다드", Category.VIDEO_OTT, 13500);
            List<String> includesNetflix = List.of("Netflix 스탠다드");
            List<Bundle> candidates = List.of(
                benefit("요금제1", 100000, includesNetflix),
                fixed("통신 결합 3,500", BundleProviderType.TELECOM, 10000, includesNetflix),
                fixed("통신 결합 1,000", BundleProviderType.TELECOM, 12500, includesNetflix),
                fixed("통신 결합 5,000", BundleProviderType.TELECOM, 8500, includesNetflix),
                card("카드 10%", includesNetflix, List.of(), List.of(), List.of(percent(BundleDiscountTarget.ALL, 10))),
                fixed("플랫폼 결합 500", BundleProviderType.PLATFORM, 13000, includesNetflix)
            );

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(netflix, candidates, netflix);

            // then — 통신사 3개 중 큰 2개, 플랫폼, 카드 → 4개에서 끝 (요금제는 통신사 2개 제한 · 4개 제한으로 빠짐)
            assertThat(bundles).extracting(CheckupBundleResponse::name)
                .containsExactly("통신 결합 5,000", "통신 결합 3,500", "플랫폼 결합 500", "카드 10%");
        }

        @Test
        @DisplayName("직접 입력 구독의 점검이면 절약 제안은 빈 배열이다")
        void returnsEmptyForCustomSubscription() {
            // given
            Subscription custom = Subscription.createCustom(user, "헬스장", Category.ETC, 50000, 1,
                BillingCycleUnit.MONTH, today.plusDays(10), PaymentMethod.WEB_DIRECT, RegistrationSource.MANUAL);
            ReflectionTestUtils.setField(custom, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(custom, "createdAt", today.minusMonths(2).atStartOfDay());
            ReflectionTestUtils.setField(custom, "startedOn", today.minusMonths(2));

            // when
            List<CheckupBundleResponse> bundles = bundlesOf(custom, List.of(), custom);

            // then
            assertThat(bundles).isEmpty();
            verifyNoInteractions(bundleGetService);
        }
    }
}
