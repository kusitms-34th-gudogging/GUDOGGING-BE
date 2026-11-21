package com.subscribers.gudogging.domain.subscription.domain.billing;

import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 결제 조건 하나의 결제일 — 기준일(anchorOn) + k × 주기 (k ≥ 0) (ERD 6.2). 매번 기준일에서 계산하므로 월말 결제일이 밀리지 않는다 (1/31 → 2/28 →
 * 3/31). 기준일 = 사용자가 입력한 다음 결제일이라 그 전에는 결제일이 없다.
 */
public record BillingCycle(int value, BillingCycleUnit unit, LocalDate anchorOn) {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int DAYS_PER_WEEK = 7;

    /**
     * date 이후(포함) 첫 결제일.
     */
    public LocalDate firstDateOnOrAfter(LocalDate date) {
        return dateAt(firstIndexOnOrAfter(date));
    }

    /**
     * from ~ to (양끝 포함) 사이 결제일 수. 결제일을 하나씩 만들지 않고 인덱스 차이로 계산한다.
     */
    public long countBetween(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 0;
        }
        return firstIndexOnOrAfter(to.plusDays(1)) - firstIndexOnOrAfter(from);
    }

    /**
     * from ~ to (양끝 포함) 사이 결제일 목록. to가 from보다 앞서면 빈 목록.
     */
    public List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (long index = firstIndexOnOrAfter(from); !dateAt(index).isAfter(to); index++) {
            dates.add(dateAt(index));
        }
        return dates;
    }

    private LocalDate dateAt(long index) {
        long cycles = index * value;
        return switch (unit) {
            case DAY -> anchorOn.plusDays(cycles);
            case WEEK -> anchorOn.plusWeeks(cycles);
            case MONTH -> anchorOn.plusMonths(cycles);
            case YEAR -> anchorOn.plusMonths(cycles * MONTHS_PER_YEAR);
        };
    }

    private long firstIndexOnOrAfter(LocalDate date) {
        if (!date.isAfter(anchorOn)) {
            return 0;
        }
        long estimate = switch (unit) {
            case DAY -> Math.ceilDiv(ChronoUnit.DAYS.between(anchorOn, date), value);
            case WEEK -> Math.ceilDiv(ChronoUnit.DAYS.between(anchorOn, date), (long) DAYS_PER_WEEK * value);
            case MONTH -> ChronoUnit.MONTHS.between(anchorOn, date) / value - 1;
            case YEAR -> ChronoUnit.MONTHS.between(anchorOn, date) / ((long) MONTHS_PER_YEAR * value) - 1;
        };
        long index = Math.max(estimate, 0);
        while (dateAt(index).isBefore(date)) {
            index++;
        }
        return index;
    }
}
