package com.subscribers.gudogging.domain.auth.domain.repository;

import com.subscribers.gudogging.domain.auth.domain.RefreshToken;

public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);
}
