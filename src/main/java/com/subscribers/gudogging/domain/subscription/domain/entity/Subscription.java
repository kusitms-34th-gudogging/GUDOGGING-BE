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
import java.time.LocalDate;
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

}
