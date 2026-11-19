package com.subscribers.gudogging.domain.check.domain.entity;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.check.domain.result.CheckResultStage;
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
import java.time.LocalDateTime;
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

    @Column(name = "score_percent", nullable = false)
    private int scorePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private CheckResultStage stage;

    @Column(name = "memo", length = 100)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 10)
    private CheckDecision decision;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "cancel_reason_text", length = 100)
    private String cancelReasonText;

    public static SubscriptionCheck create(
        Subscription subscription,
        LocalDate targetBillingOn,
        Category category,
        int usageScore,
        int satisfactionScore,
        int planScore,
        int scorePercent,
        CheckResultStage stage,
        String memo
    ) {
        return new SubscriptionCheck(subscription, targetBillingOn, category,
            usageScore, satisfactionScore, planScore, scorePercent, stage, memo, null, null, null);
    }

    public void keep(LocalDateTime decidedAt) {
        this.decision = CheckDecision.KEEP;
        this.decidedAt = decidedAt;
    }

    public void cancel(String cancelReasonText, LocalDateTime decidedAt) {
        this.decision = CheckDecision.CANCEL;
        this.cancelReasonText = cancelReasonText;
        this.decidedAt = decidedAt;
    }

}
