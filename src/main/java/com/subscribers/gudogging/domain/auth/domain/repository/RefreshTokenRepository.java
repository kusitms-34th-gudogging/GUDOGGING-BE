package com.subscribers.gudogging.domain.auth.domain.repository;

import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);

    Optional<String> findByUserId(UUID userId);
}
