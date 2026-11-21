package com.subscribers.gudogging.domain.notification.domain;

/**
 * 푸시 발송. 지금은 Firebase 서비스 계정 키가 없어 로그만 남기는 구현(LoggingPushSender)을 쓰고, 키를 받으면 Firebase Admin SDK 구현으로 교체한다.
 */
public interface PushSender {

    void send(PushMessage message);
}
