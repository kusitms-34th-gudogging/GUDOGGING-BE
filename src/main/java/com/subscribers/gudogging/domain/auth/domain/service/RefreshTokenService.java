package com.subscribers.gudogging.domain.auth.domain.service;

import com.subscribers.gudogging.domain.auth.application.exception.InvalidRefreshTokenException;
import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import com.subscribers.gudogging.domain.auth.domain.repository.RefreshTokenRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void saveRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 저장된 Refresh Token이 없거나(만료·미발급) 요청 토큰과 다르면(이미 교체됨) InvalidRefreshTokenException을 던진다.
     */
    public void validateRefreshToken(UUID userId, String token) {
        String savedToken = refreshTokenRepository.findByUserId(userId)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (!savedToken.equals(token)) {
            throw new InvalidRefreshTokenException();
        }
    }
}
