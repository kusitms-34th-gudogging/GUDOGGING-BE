package com.subscribers.gudogging.domain.notification.external;

import com.subscribers.gudogging.domain.notification.domain.PushMessage;
import com.subscribers.gudogging.domain.notification.domain.PushSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Firebase 서비스 계정 키가 없는 동안 쓰는 기본 구현 — 실제로 보내지 않고 로그만 남긴다.
 */
@Slf4j
@Component
public class LoggingPushSender implements PushSender {

    private static final int TOKEN_PREFIX_LENGTH = 8;

    @Override
    public void send(PushMessage message) {
        String token = message.token();
        String tokenPrefix = token.length() > TOKEN_PREFIX_LENGTH ? token.substring(0, TOKEN_PREFIX_LENGTH) : token;
        log.info("[푸시 발송(로그)] token={}..., title={}, data={}", tokenPrefix, message.title(), message.data());
    }
}
