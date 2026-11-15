package com.subscribers.gudogging.domain.auth.domain;

import java.time.Duration;
import java.util.UUID;

public record RefreshToken(
    UUID userId,
    String token,
    Duration timeToLive
) {
    public static RefreshToken of(UUID userId, String token, long expirationSeconds) {
        return new RefreshToken(userId, token, Duration.ofSeconds(expirationSeconds));
    }
}
