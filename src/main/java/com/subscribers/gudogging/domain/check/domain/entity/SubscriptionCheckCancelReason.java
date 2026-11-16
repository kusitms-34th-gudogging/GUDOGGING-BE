package com.subscribers.gudogging.domain.check.domain.entity;

import com.subscribers.gudogging.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "subscription_check_cancel_reasons",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_subscription_check_cancel_reasons_check_reason",
        columnNames = {"subscription_check_id", "reason"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionCheckCancelReason extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_check_id", nullable = false)
    private SubscriptionCheck subscriptionCheck;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private CancelReason reason;

    public static SubscriptionCheckCancelReason create(SubscriptionCheck subscriptionCheck, CancelReason reason) {
        return new SubscriptionCheckCancelReason(
            subscriptionCheck,
            reason
        );
    }

}
