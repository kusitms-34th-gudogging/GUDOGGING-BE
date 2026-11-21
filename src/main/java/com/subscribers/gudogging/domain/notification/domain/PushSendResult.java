package com.subscribers.gudogging.domain.notification.domain;

/**
 * 푸시 발송 결과. INVALID_TOKEN이면 FCM이 더 이상 쓸 수 없는 토큰이라고 응답한 것 (앱 삭제 · 토큰 만료) — 회원 토큰을 비운다.
 */
public enum PushSendResult {
    SENT,
    INVALID_TOKEN
}
