package com.subscribers.gudogging.domain.check.application.usecase;

import com.subscribers.gudogging.domain.check.application.dto.request.CheckupAnswerRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.DecideCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.SubmitCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupDecisionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupResultResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupSubmissionResponse;
import com.subscribers.gudogging.domain.check.application.exception.CheckSubscriptionNotActiveException;
import com.subscribers.gudogging.domain.check.application.exception.CheckupNotFoundException;
import com.subscribers.gudogging.domain.check.application.exception.InvalidCheckupAnswersException;
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
import com.subscribers.gudogging.domain.subscription.application.exception.SubscriptionNotFoundException;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
public class CheckUseCase {

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionCheckGetService subscriptionCheckGetService;
    private final SubscriptionCheckCreateService subscriptionCheckCreateService;
    private final SubscriptionCheckCancelReasonGetService subscriptionCheckCancelReasonGetService;
    private final SubscriptionCheckCancelReasonCreateService subscriptionCheckCancelReasonCreateService;

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
     * 점검 결과 화면(K-80) — 활용도 · 단계 · 응답 문구 · 메모 · 가장 큰 영향 문항과 같은 카테고리 지출. 구독 종료된 구독의 점검도 조회할 수 있다.
     * 절약 제안(번들)은 데이터 기준이 정해지기 전까지 빈 배열로 내려준다.
     */
    public CheckupResultResponse getCheckupResult(UUID userId, UUID checkupId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        SubscriptionCheck check = subscriptionCheckGetService.findCheck(user, checkupId)
            .orElseThrow(CheckupNotFoundException::new);

        List<Subscription> subscriptions = subscriptionGetService.findSubscriptions(user);
        return CheckupResultResponse.of(check, subscriptions, LocalDate.now());
    }

    /**
     * 점검 결과(K-80) · 해지 이유(K-90)에서 고른 유지/해지 결정을 저장한다. 같은 점검에 다시 결정하면 마지막 결정으로 덮어쓴다.
     * 해지 이유는 요청한 이유로 맞춘다 — 빠진 이유는 Soft Delete, 예전에 지운 이유는 새 행 대신 되살린다 (UNIQUE(점검, 이유)가 삭제된 행까지 검사).
     * 결정과 맞지 않는 값(KEEP의 이유, CUSTOM 없는 텍스트)은 무시한다.
     */
    @Transactional
    public CheckupDecisionResponse decideCheckup(UUID userId, UUID checkupId, DecideCheckupRequest request) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        SubscriptionCheck check = subscriptionCheckGetService.findCheck(user, checkupId)
            .orElseThrow(CheckupNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();
        Set<CancelReason> reasons = EnumSet.noneOf(CancelReason.class);
        if (request.decision() == CheckDecision.KEEP) {
            check.keep(now);
        } else {
            if (request.cancelReasons() != null) {
                reasons.addAll(request.cancelReasons());
            }
            check.cancel(cancelReasonTextOf(reasons, request.cancelReasonText()), now);
        }
        syncCancelReasons(check, reasons);

        return CheckupDecisionResponse.from(check);
    }

    /**
     * 점검의 해지 이유를 reasons와 같게 맞춘다. 기존 행(삭제 포함)이 있으면 되살리거나 Soft Delete하고, 없는 이유만 새로 저장한다.
     */
    private void syncCancelReasons(SubscriptionCheck check, Set<CancelReason> reasons) {
        Map<CancelReason, SubscriptionCheckCancelReason> existingByReason = subscriptionCheckCancelReasonGetService
            .findAllCancelReasonsIncludingDeleted(check).stream()
            .collect(Collectors.toMap(SubscriptionCheckCancelReason::getReason, Function.identity()));

        existingByReason.forEach((reason, cancelReason) -> {
            if (reasons.contains(reason)) {
                cancelReason.restore();
            } else if (!cancelReason.isDeleted()) {
                cancelReason.softDelete();
            }
        });

        List<SubscriptionCheckCancelReason> newReasons = reasons.stream()
            .filter(reason -> !existingByReason.containsKey(reason))
            .map(reason -> SubscriptionCheckCancelReason.create(check, reason))
            .toList();
        if (!newReasons.isEmpty()) {
            subscriptionCheckCancelReasonCreateService.createCancelReasons(newReasons);
        }
    }

    /**
     * 직접 입력 텍스트 — CUSTOM을 골랐고 공백이 아니면 앞뒤 공백을 지운 값, 아니면 저장하지 않는다(null).
     */
    private static String cancelReasonTextOf(Set<CancelReason> reasons, String cancelReasonText) {
        if (!reasons.contains(CancelReason.CUSTOM) || cancelReasonText == null || cancelReasonText.isBlank()) {
            return null;
        }
        return cancelReasonText.strip();
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
