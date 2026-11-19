package com.subscribers.gudogging.domain.check.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.check.application.dto.request.CheckupAnswerRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.SubmitCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupSubmissionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupOptionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupQuestionResponse;
import com.subscribers.gudogging.domain.check.application.exception.CheckErrorCode;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
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
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private SubscriptionCheckGetService subscriptionCheckGetService;

    @Mock
    private SubscriptionCheckCreateService subscriptionCheckCreateService;

    @InjectMocks
    private CheckUseCase checkUseCase;

    @Captor
    private ArgumentCaptor<SubscriptionCheck> checkCaptor;

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
            given(subscriptionCheckGetService.findLatestCheck(subscription)).willReturn(Optional.ofNullable(latestCheck));
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
}
