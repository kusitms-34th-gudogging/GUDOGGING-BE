package com.subscribers.gudogging.domain.calendar.application.dto.response;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 결제가 있는 날짜 1개. payments는 금액 낮은 순, 같으면 서비스명 순 (C-02, 날짜 칸 로고는 앞 2개).
 */
public record CalendarDayResponse(
    LocalDate date,
    int totalAmount,
    List<CalendarPaymentResponse> payments
) {

    private static final Comparator<CalendarPaymentResponse> PAYMENT_ORDER =
        Comparator.comparingInt(CalendarPaymentResponse::amount)
            .thenComparing(CalendarPaymentResponse::serviceName);

    public static CalendarDayResponse of(LocalDate date, List<CalendarPaymentResponse> payments) {
        List<CalendarPaymentResponse> sortedPayments = payments.stream()
            .sorted(PAYMENT_ORDER)
            .toList();
        int totalAmount = sortedPayments.stream()
            .mapToInt(CalendarPaymentResponse::amount)
            .reduce(0, Math::addExact);
        return new CalendarDayResponse(date, totalAmount, sortedPayments);
    }
}
