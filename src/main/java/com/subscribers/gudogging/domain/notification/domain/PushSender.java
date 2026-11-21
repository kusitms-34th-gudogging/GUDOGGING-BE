package com.subscribers.gudogging.domain.notification.domain;

/**
 * 푸시 발송. FIREBASE_CREDENTIALS가 있으면 Firebase Admin SDK 구현(FirebasePushSender), 없으면 로그만 남기는 구현(LoggingPushSender)을 쓴다.
 * 없는 토큰이면 INVALID_TOKEN을 돌려주고, 그 밖의 실패는 예외를 던진다.
 */
public interface PushSender {

    PushSendResult send(PushMessage message);
}
