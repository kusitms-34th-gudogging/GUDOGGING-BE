package com.subscribers.gudogging.global.config.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.subscribers.gudogging.domain.notification.domain.PushSender;
import com.subscribers.gudogging.domain.notification.external.FirebasePushSender;
import com.subscribers.gudogging.domain.notification.external.LoggingPushSender;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 푸시 발송 구현 선택 — firebase.credentials(FIREBASE_CREDENTIALS, 서비스 계정 JSON의 Base64)가 있으면 FCM, 없으면 로그만 남긴다. PushSender 빈은 항상 하나.
 */
@Slf4j
@Configuration
public class PushSenderConfig {

    private static final String HAS_CREDENTIALS = "!'${firebase.credentials:}'.isBlank()";
    private static final String NO_CREDENTIALS = "'${firebase.credentials:}'.isBlank()";

    @Bean
    @ConditionalOnExpression(HAS_CREDENTIALS)
    public PushSender firebasePushSender(@Value("${firebase.credentials}") String encodedCredentials)
        throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(Base64.getDecoder().decode(encodedCredentials.trim())));
        FirebaseApp firebaseApp = FirebaseApp.getApps().isEmpty()
            ? FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build())
            : FirebaseApp.getInstance();
        log.info("푸시 발송: Firebase(FCM) 사용");
        return new FirebasePushSender(FirebaseMessaging.getInstance(firebaseApp));
    }

    @Bean
    @ConditionalOnExpression(NO_CREDENTIALS)
    public PushSender loggingPushSender() {
        log.info("푸시 발송: FIREBASE_CREDENTIALS 없음 — 로그만 남김");
        return new LoggingPushSender();
    }
}
