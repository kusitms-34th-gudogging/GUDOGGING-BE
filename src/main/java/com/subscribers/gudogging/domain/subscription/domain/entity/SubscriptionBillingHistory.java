package com.subscribers.gudogging.domain.subscription.domain.entity;

import com.subscribers.gudogging.domain.subscription.domain.billing.BillingCycle;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 구독 결제 조건 이력 (ERD 4.4.1). 결제 정보를 수정할 때 수정 전 조건(금액 · 주기 · 기준일)으로 결제한 적이 있으면 수정일(ended_on)과 함께 남긴다. 현재 조건은
 * subscriptions에 있다.
 */
@Entity
@Table(
    name = "subscription_billing_histories",
    indexes = @Index(name = "idx_subscription_billing_histories_subscription_id", columnList = "subscription_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionBillingHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

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

    /**
     * 이 조건을 다른 조건으로 바꾼 날(수정일). 이 날까지의 결제일을 이 조건으로 세되, 다음 조건의 기준일이 같은 날이면 그 날은 다음 조건으로 센다.
     */
    @Column(name = "ended_on", nullable = false)
    private LocalDate endedOn;

    public static SubscriptionBillingHistory create(
        Subscription subscription,
        int amount,
        int billingCycleValue,
        BillingCycleUnit billingCycleUnit,
        LocalDate billingAnchorOn,
        LocalDate endedOn
    ) {
        return new SubscriptionBillingHistory(subscription, amount, billingCycleValue, billingCycleUnit,
            billingAnchorOn, endedOn);
    }

    public BillingCycle billingCycle() {
        return new BillingCycle(billingCycleValue, billingCycleUnit, billingAnchorOn);
    }
}
