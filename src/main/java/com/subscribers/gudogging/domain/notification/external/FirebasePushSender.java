package com.subscribers.gudogging.domain.notification.external;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.subscribers.gudogging.domain.notification.domain.PushMessage;
import com.subscribers.gudogging.domain.notification.domain.PushSendResult;
import com.subscribers.gudogging.domain.notification.domain.PushSender;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Firebase Admin SDK로 FCM 발송. 없는 토큰(UNREGISTERED · INVALID_ARGUMENT)이면 INVALID_TOKEN, 그 밖의 실패는 예외. 빈 등록은 PushSenderConfig.
 */
@RequiredArgsConstructor
public class FirebasePushSender implements PushSender {

    private static final Set<MessagingErrorCode> INVALID_TOKEN_CODES = Set.of(
        MessagingErrorCode.UNREGISTERED,
        MessagingErrorCode.INVALID_ARGUMENT
    );

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(PushMessage message) {
        Message fcmMessage = Message.builder()
            .setToken(message.token())
            .setNotification(Notification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .build())
            .putAllData(message.data())
            .build();
        try {
            firebaseMessaging.send(fcmMessage);
            return PushSendResult.SENT;
        } catch (FirebaseMessagingException e) {
            if (INVALID_TOKEN_CODES.contains(e.getMessagingErrorCode())) {
                return PushSendResult.INVALID_TOKEN;
            }
            throw new IllegalStateException("FCM 발송에 실패했습니다: " + e.getMessagingErrorCode(), e);
        }
    }
}
