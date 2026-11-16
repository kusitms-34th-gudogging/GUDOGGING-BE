package com.subscribers.gudogging.domain.check.domain.entity;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
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
    name = "subscription_checks",
    indexes = @Index(
        name = "idx_subscription_checks_subscription_target",
        columnList = "subscription_id, target_billing_on"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionCheck extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "target_billing_on", nullable = false)
    private LocalDate targetBillingOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "usage_score", nullable = false)
    private int usageScore;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "satisfaction_score", nullable = false)
    private int satisfactionScore;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "plan_score", nullable = false)
    private int planScore;

    @Column(name = "memo", length = 100)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 10)
    private CheckDecision decision;

    @Column(name = "cancel_reason_text", length = 100)
    private String cancelReasonText;

    public static SubscriptionCheck create(
        Subscription subscription,
        LocalDate targetBillingOn,
        Category category,
        int usageScore,
        int satisfactionScore,
        int planScore,
        String memo
    ) {
        return new SubscriptionCheck(subscription, targetBillingOn, category,
            usageScore, satisfactionScore, planScore, memo, null, null);
    }

    public void keep() {
        this.decision = CheckDecision.KEEP;
    }

    public void cancel(String cancelReasonText) {
        this.decision = CheckDecision.CANCEL;
        this.cancelReasonText = cancelReasonText;
    }

}
