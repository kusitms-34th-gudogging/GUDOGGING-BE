package com.subscribers.gudogging.domain.subscription.domain.entity;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.billing.BillingCycle;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
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
    },
    uniqueConstraints = @UniqueConstraint(
        name = Subscription.ACTIVE_SERVICE_UNIQUE_CONSTRAINT,
        columnNames = {"user_id", "active_service_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Subscription extends BaseEntity {

    public static final String ACTIVE_SERVICE_UNIQUE_CONSTRAINT = "uk_subscriptions_user_active_service";

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

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    /**
     * 회원별 사전 등록 서비스의 ACTIVE 구독을 1건으로 제한하는 DB 계산 컬럼 (동시 등록 방지). 애플리케이션은 쓰지 않는다.
     * 삭제되지 않은 ACTIVE 구독이면 service_id, 아니면 NULL — 고유 인덱스는 NULL끼리 충돌하지 않으므로 종료 · 삭제된 구독과 직접 입력 구독은
     * 제한하지 않는다.
     */
    @Column(name = "active_service_id", insertable = false, updatable = false,
        columnDefinition = "BINARY(16) GENERATED ALWAYS AS "
            + "(CASE WHEN status = 'ACTIVE' AND deleted = FALSE THEN service_id END)")
    private UUID activeServiceId;

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
            SubscriptionStatus.ACTIVE, null, false, registrationSource, null, null);
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
            SubscriptionStatus.ACTIVE, null, false, registrationSource, null, null);
    }

    /**
     * 구독 시작일은 등록일 — BaseEntity.createdAt과 같이 저장 시점에 채운다. 재등록하면 reactivate에서 재등록일로 바뀐다.
     */
    @PrePersist
    private void initStartedOn() {
        if (startedOn == null) {
            startedOn = LocalDate.now();
        }
    }

    /**
     * 구독 정보 편집 (M-06). 입력한 다음 결제일을 새 결제 기준일로 저장하고, 지금 조건으로 결제한 적이 있으면 그 조건을 수정일(today)까지의 결제 조건 이력으로
     * 반환한다 (ERD 4.4.1). 오늘 결제일은 새 다음 결제일이 오늘이 아니면 지금 조건으로 센다. 금액 · 주기 · 다음 결제일이 그대로면 기준일을 바꾸지 않는다 (월말 기준일 유지).
     */
    public Optional<SubscriptionBillingHistory> updatePaymentInfo(
        Category category,
        int amount,
        int billingCycleValue,
        BillingCycleUnit billingCycleUnit,
        LocalDate nextBillingDate,
        PaymentMethod paymentMethod,
        LocalDate today
    ) {
        this.category = category;
        this.paymentMethod = paymentMethod;
        boolean billingUnchanged = this.amount == amount
            && this.billingCycleValue == billingCycleValue
            && this.billingCycleUnit == billingCycleUnit
            && nextBillingOn(today).equals(nextBillingDate);
        if (billingUnchanged) {
            return Optional.empty();
        }

        LocalDate lastPaidOn = nextBillingDate.isAfter(today) ? today : today.minusDays(1);
        Optional<SubscriptionBillingHistory> history = billingAnchorOn.isAfter(lastPaidOn)
            ? Optional.empty()
            : Optional.of(SubscriptionBillingHistory.create(this, this.amount, this.billingCycleValue,
                this.billingCycleUnit, this.billingAnchorOn, today));
        this.amount = amount;
        this.billingCycleValue = billingCycleValue;
        this.billingCycleUnit = billingCycleUnit;
        this.billingAnchorOn = nextBillingDate;
        return history;
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
     * 구독 종료 구독 재등록 (M-05). 같은 행을 되살리며 시작일을 재등록일로 다시 잡아 쉬었던 기간이 결제 횟수 · 캘린더에 들어가지 않게 한다.
     */
    public void reactivate(LocalDate billingAnchorOn, LocalDate today) {
        this.status = SubscriptionStatus.ACTIVE;
        this.canceledOn = null;
        this.billingAnchorOn = billingAnchorOn;
        this.startedOn = today;
    }

    /**
     * 월 환산 금액 (ERD 6.1). 원 단위 반올림.
     */
    public long monthlyAmount() {
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
        return numerator.divide(denominator, 0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * 연 환산 금액 = 월 환산 금액 × 12 (ERD 6.1).
     */
    public long yearlyAmount() {
        return Math.multiplyExact(monthlyAmount(), MONTHS_PER_YEAR);
    }

    /**
     * base 날짜 기준 상태 (ERD 6.2 캘린더 C-03). base가 등록일 ~ 해지일(구독 중이면 상한 없음) 사이면 ACTIVE.
     */
    public SubscriptionStatus statusOn(LocalDate base) {
        boolean afterStart = !base.isBefore(startedOn());
        boolean beforeCancel = canceledOn == null || !base.isAfter(canceledOn);
        return afterStart && beforeCancel ? SubscriptionStatus.ACTIVE : SubscriptionStatus.ENDED;
    }

    /**
     * 현재 결제 조건 — 결제 기준일 + k × 주기 (ERD 6.2).
     */
    public BillingCycle billingCycle() {
        return new BillingCycle(billingCycleValue, billingCycleUnit, billingAnchorOn);
    }

    /**
     * 현재 조건으로 base 이후(포함) 첫 결제일. 결제 조건 이력은 모두 오늘까지 끝났으므로 오늘 이후 다음 결제일은 이 값이다. 과거 날짜 기준은 BillingSchedule을 쓴다.
     */
    public LocalDate nextBillingOn(LocalDate base) {
        return billingCycle().firstDateOnOrAfter(base);
    }

    /**
     * 구독 시작일 = started_on (등록일, 재등록하면 재등록일).
     */
    public LocalDate startedOn() {
        return startedOn;
    }

    /**
     * 결제 경로 + 서비스 해지 링크 유무로 해지 안내 4분기 결정 (X-01 / X-03 / X-04 / X-05). 직접 입력 구독(serviceCatalog = null)은 링크 없음으로 본다.
     */
    public CancelGuideType cancelGuideType() {
        return switch (paymentMethod) {
            case APP_STORE -> CancelGuideType.APP_STORE;
            case GOOGLE_PLAY -> CancelGuideType.GOOGLE_PLAY;
            case WEB_DIRECT -> hasServiceCancelUrl() ? CancelGuideType.WEB_WITH_URL : CancelGuideType.WEB_WITHOUT_URL;
        };
    }

    /**
     * 서비스명 뒤에 붙는 목적격 조사. 한글 음절로 끝나면 받침 유무로 '을'/'를', 그 외(영문·숫자 등)는 services.object_particle, 없으면 '를'.
     */
    public String objectParticle() {
        char last = name.charAt(name.length() - 1);
        if (last >= '가' && last <= '힣') {
            return (last - '가') % 28 == 0 ? "를" : "을";
        }
        if (serviceCatalog != null && serviceCatalog.getObjectParticle() != null
            && !serviceCatalog.getObjectParticle().isBlank()) {
            return serviceCatalog.getObjectParticle();
        }
        return "를";
    }

    private boolean hasServiceCancelUrl() {
        return serviceCatalog != null && serviceCatalog.getCancelUrl() != null
            && !serviceCatalog.getCancelUrl().isBlank();
    }

}
