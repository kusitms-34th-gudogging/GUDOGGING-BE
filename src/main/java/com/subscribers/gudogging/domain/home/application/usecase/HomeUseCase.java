package com.subscribers.gudogging.domain.home.application.usecase;

import com.subscribers.gudogging.domain.check.domain.entity.SubscriptionCheck;
import com.subscribers.gudogging.domain.check.domain.service.SubscriptionCheckGetService;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeResponse;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeSummaryResponse;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeUpcomingPaymentResponse;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeUsageItemResponse;
import com.subscribers.gudogging.domain.home.application.dto.response.HomeUsageResponse;
import com.subscribers.gudogging.domain.notification.domain.service.NotificationGetService;
import com.subscribers.gudogging.domain.subscription.domain.billing.Billing;
import com.subscribers.gudogging.domain.subscription.domain.billing.BillingSchedule;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeUseCase {

    private static final int MAX_LOGOS = 10;
    private static final int MAX_UPCOMING_PAYMENTS = 5;
    private static final int MAX_USAGE_ITEMS = 3;
    private static final int UNUSED_SCORE_PERCENT_UNDER = 60;

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionBillingHistoryGetService subscriptionBillingHistoryGetService;
    private final SubscriptionCheckGetService subscriptionCheckGetService;
    private final NotificationGetService notificationGetService;

    /**
     * 홈 정보를 조회한다 (H-01, H-02, H-03). 이번 달 요약 · 구독 점검하기 · 잘 안 쓰는 구독은 구독 중 구독 기준이고, 점검하기 · 점검 유도 · 잘 안 쓰는 구독에서
     * 꾸준히 쓰는 구독은 뺀다.
     */
    public HomeResponse getHome(UUID userId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        LocalDate today = LocalDate.now();

        List<Subscription> subscriptions = subscriptionGetService.findSubscriptions(user);
        List<Subscription> activeSubscriptions = subscriptions.stream()
            .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
            .toList();
        Map<UUID, BillingSchedule> schedules = findBillingSchedules(activeSubscriptions);
        Map<UUID, SubscriptionCheck> latestChecks = findLatestChecks(subscriptions);

        Comparator<Subscription> byNextBillingThenAmountDesc = Comparator
            .comparing((Subscription subscription) -> subscription.nextBillingOn(today))
            .thenComparing(Subscription::getAmount, Comparator.reverseOrder());
        List<Subscription> checkTargets = activeSubscriptions.stream()
            .filter(subscription -> !subscription.isSteadyUse())
            .sorted(byNextBillingThenAmountDesc)
            .toList();

        HomeSummaryResponse summary = HomeSummaryResponse.of(
            totalAmount(activeSubscriptions, schedules, schedule -> schedule.billingsBetween(today,
                YearMonth.from(today).atEndOfMonth())),
            yearlyAmount(activeSubscriptions, schedules, today),
            activeSubscriptions.size(),
            logosOf(activeSubscriptions, byNextBillingThenAmountDesc));
        List<HomeUpcomingPaymentResponse> upcomingPayments = checkTargets.stream()
            .filter(subscription -> needsCheckup(subscription, latestChecks.get(subscription.getId()), today))
            .limit(MAX_UPCOMING_PAYMENTS)
            .map(subscription -> HomeUpcomingPaymentResponse.of(subscription, today))
            .toList();
        HomeUsageResponse usage = usageOf(checkTargets, schedules, latestChecks, today);
        return HomeResponse.of(summary, upcomingPayments, usage, notificationGetService.hasUnreadNotification(user,
            LocalDateTime.now()));
    }

    private Map<UUID, BillingSchedule> findBillingSchedules(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<SubscriptionBillingHistory>> historiesBySubscriptionId = subscriptionBillingHistoryGetService
            .findBillingHistoriesBySubscriptions(subscriptions).stream()
            .collect(Collectors.groupingBy(history -> history.getSubscription().getId()));
        return subscriptions.stream()
            .collect(Collectors.toMap(Subscription::getId, subscription -> BillingSchedule.of(subscription,
                historiesBySubscriptionId.getOrDefault(subscription.getId(), List.of()))));
    }

    /**
     * 구독별 가장 최근 점검 (삭제 제외, 구독 종료 구독 포함 — hasCheckup 판단).
     */
    private Map<UUID, SubscriptionCheck> findLatestChecks(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return Map.of();
        }
        BinaryOperator<SubscriptionCheck> later = BinaryOperator.maxBy(
            Comparator.comparing(SubscriptionCheck::getCreatedAt, Comparator.nullsFirst(LocalDateTime::compareTo)));
        return subscriptionCheckGetService.findChecksBySubscriptions(subscriptions).stream()
            .collect(Collectors.toMap(check -> check.getSubscription().getId(), Function.identity(), later));
    }

    /**
     * 이번 결제 주기 점검 완료 = 가장 최근 점검의 점검 대상 결제일이 지금의 다음 결제일 (제출 때 그때 다음 결제일을 저장).
     */
    private boolean needsCheckup(Subscription subscription, SubscriptionCheck latestCheck, LocalDate today) {
        return latestCheck == null || !latestCheck.getTargetBillingOn().equals(subscription.nextBillingOn(today));
    }

    /**
     * 잘 안 쓰는 구독: 가장 최근 점검 활용도 60% 미만인 구독 (유지하기 · 해지하기 · 결정 안 함 상관없이).
     */
    private HomeUsageResponse usageOf(List<Subscription> checkTargets, Map<UUID, BillingSchedule> schedules,
                                      Map<UUID, SubscriptionCheck> latestChecks, LocalDate today) {
        List<Subscription> unusedSubscriptions = checkTargets.stream()
            .filter(subscription -> isUnused(latestChecks.get(subscription.getId())))
            .toList();
        YearMonth thisMonth = YearMonth.from(today);
        // checkTargets가 결제일 가까운 순이고 정렬이 안정적이므로 활용도가 같으면 결제일 가까운 순
        List<HomeUsageItemResponse> items = unusedSubscriptions.stream()
            .sorted(Comparator.comparingInt(
                (Subscription subscription) -> latestChecks.get(subscription.getId()).getScorePercent()))
            .limit(MAX_USAGE_ITEMS)
            .map(subscription -> HomeUsageItemResponse.of(subscription, latestChecks.get(subscription.getId())))
            .toList();
        UUID nudgeSubscriptionId = checkTargets.isEmpty() ? null : checkTargets.get(0).getId();
        return HomeUsageResponse.of(
            !latestChecks.isEmpty(),
            nudgeSubscriptionId,
            unusedSubscriptions.size(),
            totalAmount(unusedSubscriptions, schedules,
                schedule -> schedule.billingsBetween(thisMonth.atDay(1), thisMonth.atEndOfMonth())),
            yearlyAmount(unusedSubscriptions, schedules, today),
            items);
    }

    private boolean isUnused(SubscriptionCheck latestCheck) {
        return latestCheck != null
            && latestCheck.getScorePercent() < UNUSED_SCORE_PERCENT_UNDER;
    }

    private List<String> logosOf(List<Subscription> activeSubscriptions, Comparator<Subscription> order) {
        return activeSubscriptions.stream()
            .filter(subscription -> subscription.getServiceCatalog() != null)
            .sorted(order)
            .map(subscription -> subscription.getServiceCatalog().getLogoUrl())
            .filter(Objects::nonNull)
            .limit(MAX_LOGOS)
            .toList();
    }

    /**
     * 올해 1~12월 결제 합계 — 등록 전 기간도 같은 주기로 결제했다고 본 예상치 (화면설계서 H-02).
     */
    private int yearlyAmount(Collection<Subscription> subscriptions, Map<UUID, BillingSchedule> schedules,
                             LocalDate today) {
        Year thisYear = Year.from(today);
        return totalAmount(subscriptions, schedules, schedule -> schedule.billingsBetweenIncludingBeforeStart(
            thisYear.atDay(1), thisYear.atMonth(12).atEndOfMonth()));
    }

    private int totalAmount(Collection<Subscription> subscriptions, Map<UUID, BillingSchedule> schedules,
                            Function<BillingSchedule, List<Billing>> billingsOf) {
        return subscriptions.stream()
            .flatMap(subscription -> billingsOf.apply(schedules.get(subscription.getId())).stream())
            .mapToInt(Billing::amount)
            .reduce(0, Math::addExact);
    }
}
