package com.subscribers.gudogging.domain.calendar.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.SortedMap;

/**
 * 월별 결제 캘린더 (C-01, C-02). days는 결제가 있는 날짜만 날짜순. currentMonthTotal은 오늘이 속한 달 합계로 조회 월과 무관 (상단 배지).
 */
public record CalendarResponse(
    int currentMonthTotal,
    List<CalendarDayResponse> days
) {

    public static CalendarResponse of(SortedMap<LocalDate, List<CalendarPaymentResponse>> paymentsByDate,
                                      int currentMonthTotal) {
        List<CalendarDayResponse> days = paymentsByDate.entrySet().stream()
            .map(entry -> CalendarDayResponse.of(entry.getKey(), entry.getValue()))
            .toList();
        return new CalendarResponse(currentMonthTotal, days);
    }
}
