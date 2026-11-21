package com.subscribers.gudogging.domain.calendar.application.usecase;

import com.subscribers.gudogging.domain.calendar.application.dto.response.CalendarPaymentResponse;
import com.subscribers.gudogging.domain.calendar.application.dto.response.CalendarResponse;
import com.subscribers.gudogging.domain.calendar.application.exception.InvalidCalendarMonthException;
import com.subscribers.gudogging.domain.subscription.domain.billing.Billing;
import com.subscribers.gudogging.domain.subscription.domain.billing.BillingSchedule;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionBillingHistory;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionBillingHistoryGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarUseCase {

    private static final int MIN_YEAR = 2024;
    private static final int MAX_YEAR = 2028;

    private final UserGetService userGetService;
    private final SubscriptionGetService subscriptionGetService;
    private final SubscriptionBillingHistoryGetService subscriptionBillingHistoryGetService;

    /**
     * 선택한 연 · 월의 결제 예정일을 날짜별로 조회한다 (C-01, C-02). 구독마다 결제 조건 이력 + 현재 조건으로 결제일을 펼치고, 등록일 전 · 해지일 뒤 결제는 뺀다.
     * 상단 배지용 이번 달 합계는 조회 월과 관계없이 오늘이 속한 달 1일~말일 합계다.
     */
    public CalendarResponse getMonthlyCalendar(UUID userId, int year, int month) {
        YearMonth targetMonth = toYearMonth(year, month);
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        List<BillingSchedule> schedules = findBillingSchedules(user);
        SortedMap<LocalDate, List<CalendarPaymentResponse>> paymentsByDate = new TreeMap<>();
        for (BillingSchedule schedule : schedules) {
            for (Billing billing : billingsIn(schedule, targetMonth)) {
                paymentsByDate.computeIfAbsent(billing.date(), date -> new ArrayList<>())
                    .add(CalendarPaymentResponse.of(schedule.subscription(), billing));
            }
        }
        return CalendarResponse.of(paymentsByDate, totalAmountIn(schedules, YearMonth.now()));
    }

    private YearMonth toYearMonth(int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR || month < 1 || month > 12) {
            throw new InvalidCalendarMonthException();
        }
        return YearMonth.of(year, month);
    }

    private List<BillingSchedule> findBillingSchedules(User user) {
        List<Subscription> subscriptions = subscriptionGetService.findSubscriptions(user);
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<SubscriptionBillingHistory>> historiesBySubscriptionId = subscriptionBillingHistoryGetService
            .findBillingHistoriesBySubscriptions(subscriptions).stream()
            .collect(Collectors.groupingBy(history -> history.getSubscription().getId()));
        return subscriptions.stream()
            .map(subscription -> BillingSchedule.of(subscription,
                historiesBySubscriptionId.getOrDefault(subscription.getId(), List.of())))
            .toList();
    }

    private List<Billing> billingsIn(BillingSchedule schedule, YearMonth yearMonth) {
        return schedule.billingsBetween(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    private int totalAmountIn(List<BillingSchedule> schedules, YearMonth yearMonth) {
        return schedules.stream()
            .flatMap(schedule -> billingsIn(schedule, yearMonth).stream())
            .mapToInt(Billing::amount)
            .reduce(0, Math::addExact);
    }
}
