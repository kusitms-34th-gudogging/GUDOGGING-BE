package com.subscribers.gudogging.domain.subscription.infrastructure;

import com.subscribers.gudogging.domain.subscription.domain.draft.SubscriptionDraft;
import com.subscribers.gudogging.domain.subscription.domain.repository.SubscriptionDraftRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * 회원당 1건(key subscription-draft:{userId})을 JSON으로 저장한다. TTL 없이 덮어쓰고, 등록 성공 · 삭제 요청 때 지운다.
 */
@Repository
@RequiredArgsConstructor
public class RedisSubscriptionDraftRepository implements SubscriptionDraftRepository {

    private static final String KEY_PREFIX = "subscription-draft:";

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public void save(SubscriptionDraft subscriptionDraft) {
        redisTemplate.opsForValue().set(
            KEY_PREFIX + subscriptionDraft.userId(),
            jsonMapper.writeValueAsString(subscriptionDraft)
        );
    }

    @Override
    public Optional<SubscriptionDraft> findByUserId(UUID userId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(jsonMapper.readValue(json, SubscriptionDraft.class));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
