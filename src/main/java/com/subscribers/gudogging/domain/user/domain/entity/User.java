package com.subscribers.gudogging.domain.user.domain.entity;

import com.subscribers.gudogging.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_social_provider_social_id",
        columnNames = {"social_provider", "social_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    private static final String WITHDRAWN_SOCIAL_ID_PREFIX = "withdrawn_";

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false, length = 10)
    private SocialProvider socialProvider;

    @Column(name = "social_id", nullable = false, length = 255)
    private String socialId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 100)
    private String name;


    public static User create(SocialProvider socialProvider, String socialId, String email, String name) {
        return new User(socialProvider, socialId, email, name);
    }

    public void withdraw() {
        softDelete();
        this.name = null;
        this.socialId = WITHDRAWN_SOCIAL_ID_PREFIX + getId();
        this.email = null;
    }

}
