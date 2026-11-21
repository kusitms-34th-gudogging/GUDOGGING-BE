package com.subscribers.gudogging.domain.notification.domain;

import java.util.Map;

/**
 * 푸시 한 건. data = type, subscriptionId, notificationId (클라이언트가 탭했을 때 이동 분기).
 */
public record PushMessage(String token, String title, String body, Map<String, String> data) {
}
