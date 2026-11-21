package com.subscribers.gudogging.domain.notification.external;

import com.subscribers.gudogging.domain.notification.domain.PushMessage;
import com.subscribers.gudogging.domain.notification.domain.PushSendResult;
import com.subscribers.gudogging.domain.notification.domain.PushSender;
import lombok.extern.slf4j.Slf4j;

/**
 * FIREBASE_CREDENTIALS가 없을 때 쓰는 구현 — 실제로 보내지 않고 로그만 남긴다. 빈 등록은 PushSenderConfig.
 */
@Slf4j
public class LoggingPushSender implements PushSender {

    private static final int TOKEN_PREFIX_LENGTH = 8;

    @Override
    public PushSendResult send(PushMessage message) {
        String token = message.token();
        String tokenPrefix = token.length() > TOKEN_PREFIX_LENGTH ? token.substring(0, TOKEN_PREFIX_LENGTH) : token;
        log.info("[푸시 발송(로그)] token={}..., title={}, data={}", tokenPrefix, message.title(), message.data());
        return PushSendResult.SENT;
    }
}
