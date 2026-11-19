package com.subscribers.gudogging.domain.check.application.usecase;

import com.subscribers.gudogging.domain.check.application.dto.request.CheckupAnswerRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.SubmitCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupSubmissionResponse;
import com.subscribers.gudogging.domain.check.application.exception.CheckSubscriptionNotActiveException;
import com.subscribers.gudogging.domain.check.application.exception.InvalidCheckupAnswersException;
import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.form.CheckupQuestionType;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
import com.subscribers.gudogging.domain.check.domain.result.CheckScore;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckCreateService;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotFoundException;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckUseCase {

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionCheckGetService subscriptionCheckGetService;
    private final SubscriptionCheckCreateService subscriptionCheckCreateService;

    /**
     * 구독 카테고리에 맞는 점검 문항 3개와 지난 메모를 조회한다 (K-11~K-70). 구독 종료 구독은 점검할 수 없고, 꾸준히 쓰는 구독은 직접 점검할 수 있다.
     */
    public CheckupFormResponse getCheckupForm(UUID userId, UUID subscriptionId) {
        Subscription subscription = findActiveSubscription(userId, subscriptionId);

        SubscriptionCheck latestCheck = subscriptionCheckGetService.findLatestCheck(subscription).orElse(null);
        return CheckupFormResponse.of(subscription, latestCheck, LocalDate.now());
    }

    /**
     * 점검 응답 · 메모를 저장하고 활용도 · 단계를 계산해 함께 저장한다 (K-70 → K-80). 점검 대상 결제일(이번 다음 결제일)을 저장해 이번 결제 주기 점검 완료를 표시한다.
     * 같은 주기에 다시 제출하면 새 점검으로 쌓인다.
     */
    @Transactional
    public CheckupSubmissionResponse submitCheckup(UUID userId, UUID subscriptionId, SubmitCheckupRequest request) {
        Subscription subscription = findActiveSubscription(userId, subscriptionId);
        Map<CheckupQuestionType, Integer> scores = scoresByType(request);

        int usageScore = scores.get(CheckupQuestionType.USAGE);
        int satisfactionScore = scores.get(CheckupQuestionType.SATISFACTION);
        int planScore = scores.get(CheckupQuestionType.PLAN);
        int scorePercent = CheckScore.percentOf(usageScore, satisfactionScore, planScore);
        SubscriptionCheck subscriptionCheck = SubscriptionCheck.create(subscription,
            subscription.nextBillingOn(LocalDate.now()), subscription.getCategory(), usageScore, satisfactionScore,
            planScore, scorePercent, CheckResultStage.of(scorePercent), memoOf(request.memo()));

        SubscriptionCheck saved = subscriptionCheckCreateService.createCheck(subscriptionCheck);
        return CheckupSubmissionResponse.from(saved);
    }

    /**
     * 본인 구독 중 점검할 수 있는(구독 중) 구독. 꾸준히 쓰는 구독도 직접 점검할 수 있다.
     */
    private Subscription findActiveSubscription(UUID userId, UUID subscriptionId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Subscription subscription = subscriptionGetService.findSubscription(user, subscriptionId)
            .orElseThrow(SubscriptionNotFoundException::new);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new CheckSubscriptionNotActiveException();
        }
        return subscription;
    }

    /**
     * 이용도 · 만족도 · 계획 응답이 하나씩이어야 한다.
     */
    private Map<CheckupQuestionType, Integer> scoresByType(SubmitCheckupRequest request) {
        Map<CheckupQuestionType, Integer> scores = new EnumMap<>(CheckupQuestionType.class);
        for (CheckupAnswerRequest answer : request.answers()) {
            if (scores.putIfAbsent(answer.type(), answer.score()) != null) {
                throw new InvalidCheckupAnswersException();
            }
        }
        if (scores.size() != CheckupQuestionType.values().length) {
            throw new InvalidCheckupAnswersException();
        }
        return scores;
    }

    /**
     * 앞뒤 공백을 지우고, 비어 있으면 저장하지 않는다(null).
     */
    private static String memoOf(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }
        return memo.strip();
    }
}
