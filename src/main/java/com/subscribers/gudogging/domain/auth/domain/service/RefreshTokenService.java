package com.subscribers.gudogging.domain.auth.domain.service;

import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import com.subscribers.gudogging.domain.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void saveRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }
}
