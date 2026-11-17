package com.subscribers.gudogging.domain.subscription.domain.entity;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_subscriptions_user_status", columnList = "user_id, status"),
        @Index(name = "idx_subscriptions_service_id", columnList = "service_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Subscription extends BaseEntity {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int WEEKS_PER_YEAR = 52;
    private static final int DAYS_PER_YEAR = 365;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceCatalog serviceCatalog;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category;

    @Column(name = "amount", nullable = false)
    private int amount;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "billing_cycle_value", nullable = false)
    private int billingCycleValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle_unit", nullable = false, length = 10)
    private BillingCycleUnit billingCycleUnit;

    @Column(name = "billing_anchor_on", nullable = false)
    private LocalDate billingAnchorOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SubscriptionStatus status;

    @Column(name = "canceled_on")
    private LocalDate canceledOn;

    @Column(name = "is_steady_use", nullable = false)
    private boolean isSteadyUse = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", nullable = false, length = 20)
    private RegistrationSource registrationSource;

    public static Subscription createFromCatalog(
        User user,
        ServiceCatalog serviceCatalog,
        Category category,
        int amount,
        int billingCycleValue,
        BillingCycleUnit billingCycleUnit,
        LocalDate billingAnchorOn,
        PaymentMethod paymentMethod,
        RegistrationSource registrationSource
    ) {
        return new Subscription(user, serviceCatalog, serviceCatalog.getName(), category, amount,
            billingCycleValue, billingCycleUnit, billingAnchorOn, paymentMethod,
            SubscriptionStatus.ACTIVE, null, false, registrationSource);
    }

    public static Subscription createCustom(
        User user,
        String name,
        Category category,
        int amount,
        int billingCycleValue,
        BillingCycleUnit billingCycleUnit,
        LocalDate billingAnchorOn,
        PaymentMethod paymentMethod,
        RegistrationSource registrationSource
    ) {
        return new Subscription(user, null, name, category, amount,
            billingCycleValue, billingCycleUnit, billingAnchorOn, paymentMethod,
            SubscriptionStatus.ACTIVE, null, false, registrationSource);
    }

    public void updatePaymentInfo(
        Category category,
        int amount,
        int billingCycleValue,
        BillingCycleUnit billingCycleUnit,
        LocalDate billingAnchorOn,
        PaymentMethod paymentMethod
    ) {
        this.category = category;
        this.amount = amount;
        this.billingCycleValue = billingCycleValue;
        this.billingCycleUnit = billingCycleUnit;
        this.billingAnchorOn = billingAnchorOn;
        this.paymentMethod = paymentMethod;
    }

    public void updateCustomName(String name) {
        this.name = name;
    }

    public void changeSteadyUse(boolean enabled) {
        this.isSteadyUse = enabled;
    }

    public void cancel(LocalDate today) {
        this.status = SubscriptionStatus.ENDED;
        this.canceledOn = today;
    }

    /**
     * 월 환산 금액 (ERD 6.1). 원 단위 반올림.
     */
    public int monthlyAmount() {
        BigDecimal numerator = BigDecimal.valueOf(amount);
        BigDecimal denominator = BigDecimal.valueOf(billingCycleValue);
        switch (billingCycleUnit) {
            case MONTH -> {
            }
            case YEAR -> denominator = denominator.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
            case WEEK -> {
                numerator = numerator.multiply(BigDecimal.valueOf(WEEKS_PER_YEAR));
                denominator = denominator.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
            }
            case DAY -> {
                numerator = numerator.multiply(BigDecimal.valueOf(DAYS_PER_YEAR));
                denominator = denominator.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
            }
        }
        return numerator.divide(denominator, 0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * base 이후(포함) 첫 결제일 (ERD 6.2). 결제일은 매번 billingAnchorOn에서 계산하므로 월말이 밀리지 않는다.
     */
    public LocalDate nextBillingOn(LocalDate base) {
        return billingDateAt(firstBillingIndexOnOrAfter(base));
    }

    /**
     * 구독 시작일 = 등록일 (ERD 10.1 #2).
     */
    public LocalDate startedOn() {
        return getCreatedAt().toLocalDate();
    }

    /**
     * from ~ to (양끝 포함) 사이 결제일 목록.
     */
    public List<LocalDate> billingDatesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> billingDates = new ArrayList<>();
        for (long index = firstBillingIndexOnOrAfter(from); !billingDateAt(index).isAfter(to); index++) {
            billingDates.add(billingDateAt(index));
        }
        return billingDates;
    }

    /**
     * 지금까지 결제 횟수: 등록일 ~ (구독 종료면 해지일, 구독 중이면 today) 사이 결제일 수.
     */
    public int paidCount(LocalDate today) {
        LocalDate end = status == SubscriptionStatus.ENDED ? canceledOn : today;
        return billingDatesBetween(startedOn(), end).size();
    }

    public int totalPaidAmount(LocalDate today) {
        return Math.multiplyExact(paidCount(today), amount);
    }

    private LocalDate billingDateAt(long index) {
        long cycles = index * billingCycleValue;
        return switch (billingCycleUnit) {
            case DAY -> billingAnchorOn.plusDays(cycles);
            case WEEK -> billingAnchorOn.plusWeeks(cycles);
            case MONTH -> billingAnchorOn.plusMonths(cycles);
            case YEAR -> billingAnchorOn.plusMonths(cycles * MONTHS_PER_YEAR);
        };
    }

    private long firstBillingIndexOnOrAfter(LocalDate date) {
        long index = switch (billingCycleUnit) {
            case DAY -> Math.ceilDiv(ChronoUnit.DAYS.between(billingAnchorOn, date), billingCycleValue);
            case WEEK -> Math.ceilDiv(ChronoUnit.DAYS.between(billingAnchorOn, date), 7L * billingCycleValue);
            case MONTH -> Math.floorDiv(ChronoUnit.MONTHS.between(billingAnchorOn, date), billingCycleValue) - 1;
            case YEAR -> Math.floorDiv(ChronoUnit.MONTHS.between(billingAnchorOn, date),
                (long) MONTHS_PER_YEAR * billingCycleValue) - 1;
        };
        while (billingDateAt(index).isBefore(date)) {
            index++;
        }
        return index;
    }

}
