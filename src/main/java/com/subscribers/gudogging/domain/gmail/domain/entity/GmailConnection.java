package com.subscribers.gudogging.domain.gmail.domain.entity;

import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.global.common.crypto.EncryptedStringConverter;
import com.subscribers.gudogging.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "gmail_connections",
    indexes = @Index(name = "idx_gmail_connections_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GmailConnection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "google_sub", nullable = false, length = 255)
    private String googleSub;

    @Column(name = "gmail_address", nullable = false, length = 255)
    private String gmailAddress;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public static GmailConnection create(User user, String googleSub, String gmailAddress, String refreshToken) {
        return new GmailConnection(
            user,
            googleSub,
            gmailAddress,
            refreshToken,
            null
        );
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void markSynced() {
        this.lastSyncedAt = LocalDateTime.now();
    }

}
