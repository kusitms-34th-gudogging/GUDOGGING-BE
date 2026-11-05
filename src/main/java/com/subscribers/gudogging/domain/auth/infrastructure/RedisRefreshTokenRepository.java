package com.subscribers.gudogging.domain.auth.infrastructure;

import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import com.subscribers.gudogging.domain.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(
            KEY_PREFIX + refreshToken.userId(),
            refreshToken.token(),
            refreshToken.timeToLive()
        );
    }
}
