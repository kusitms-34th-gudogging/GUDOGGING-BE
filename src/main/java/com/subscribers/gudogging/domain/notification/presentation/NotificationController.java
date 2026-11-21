package com.subscribers.gudogging.domain.notification.presentation;

import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationListResponse;
import com.subscribers.gudogging.domain.notification.application.dto.response.NotificationSettingResponse;
import com.subscribers.gudogging.domain.notification.application.usecase.NotificationUseCase;
import com.subscribers.gudogging.domain.notification.presentation.response.NotificationResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "푸시 알림 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @Operation(
        summary = "푸시 토큰 등록",
        description = """
            **기능**
            알림 권한 요청(O-03)에서 권한을 허용했을 때 FCM 기기 토큰과 기기 종류를 저장합니다.
            권한을 거부하면 호출하지 않습니다. 이미 등록된 토큰이 있으면 새 토큰으로 덮어씁니다 (회원당 1개, 마지막 기기).
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - deviceToken (String, 필수): FCM 기기 토큰 (최대 255자)
            - platform (String, 필수): 기기 종류 (IOS, ANDROID)

            **응답값**
            - 없음 (data = null)
            """
    )
    @PostMapping("/tokens")
    public CommonResponse<Void> registerPushToken(@CurrentUserId UUID userId,
                                                  @RequestBody @Valid RegisterPushTokenRequest request) {
        notificationUseCase.registerPushToken(userId, request);
        return CommonResponse.success(NotificationResponseCode.PUSH_TOKEN_REGISTERED);
    }

    @Operation(
        summary = "알림 설정 조회",
        description = """
            **기능**
            알림 설정(E-05)의 결제 3일 전 점검 알림(D-3) 수신 여부를 조회합니다. 기본값은 false(OFF)입니다.
            결제 1일 전 알림 · 해지 확인 알림은 이 설정과 관계없이 항상 발송합니다.
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)

            **응답값**
            - paymentReminderEnabled (Boolean): 결제 3일 전 점검 알림 수신 여부
            """
    )
    @GetMapping("/settings")
    public CommonResponse<NotificationSettingResponse> getNotificationSetting(@CurrentUserId UUID userId) {
        NotificationSettingResponse response = notificationUseCase.getNotificationSetting(userId);
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_SETTING_FOUND, response);
    }

    @Operation(
        summary = "알림 설정 변경",
        description = """
            **기능**
            결제 3일 전 점검 알림(D-3) 수신 여부를 변경합니다 (E-05 토글, 즉시 저장).
            false면 D-3 점검 알림만 보내지 않고, 결제 1일 전 알림 · 해지 확인 알림은 항상 발송합니다.
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - paymentReminderEnabled (Boolean, 필수): true = 수신, false = 수신 안 함

            **응답값**
            - paymentReminderEnabled (Boolean): 변경된 수신 여부
            """
    )
    @PatchMapping("/settings")
    public CommonResponse<NotificationSettingResponse> updateNotificationSetting(
        @CurrentUserId UUID userId,
        @RequestBody @Valid UpdateNotificationSettingRequest request
    ) {
        NotificationSettingResponse response = notificationUseCase.updateNotificationSetting(userId, request);
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_SETTING_UPDATED, response);
    }

    @Operation(
        summary = "알림 목록 조회",
        description = """
            **기능**
            홈 알림 아이콘으로 들어가는 알림 목록(H-04)을 조회합니다.
            발송한 푸시(결제 3일 전 · 결제 1일 전 · 해지 확인)를 최근 30일 동안 최신순으로 내려줍니다.
            '오늘' · '이전 알림' 구분은 sentAt으로 나눕니다. 탭하면 읽음 처리 API를 호출한 뒤 type에 따라 이동합니다
            (CHECK_REMINDER → 구독 점검 1/4, BILLING_REMINDER → 구독 상세, CANCEL_CONFIRM → 구독 상세 + 해지 완료 확인 모달).
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)

            **응답값**
            - notifications (Array): 알림 목록 (최신순, 최근 30일, 없으면 빈 배열)
            - notifications[].notificationId (UUID): 알림 ID
            - notifications[].type (String): CHECK_REMINDER / BILLING_REMINDER / CANCEL_CONFIRM — 화면 표시 없이 이동 분기에만 사용
            - notifications[].title (String): 푸시 제목
            - notifications[].body (String): 푸시 본문
            - notifications[].subscriptionId (UUID): 이동할 구독 ID (묶음이면 금액이 가장 큰 구독)
            - notifications[].logoUrl (String): 서비스 로고 URL (직접 입력 구독은 null, 묶음이면 금액이 가장 큰 구독의 로고)
            - notifications[].sentAt (String): 발송 시각 (ISO 8601)
            - notifications[].read (Boolean): 읽음 여부
            """
    )
    @GetMapping
    public CommonResponse<NotificationListResponse> getNotifications(@CurrentUserId UUID userId) {
        NotificationListResponse response = notificationUseCase.getNotifications(userId);
        return CommonResponse.success(NotificationResponseCode.NOTIFICATIONS_FOUND, response);
    }

    @Operation(
        summary = "알림 읽음 처리",
        description = """
            **기능**
            알림 목록(H-04)에서 알림을 탭하면 읽음 처리합니다. 이미 읽은 알림이어도 성공으로 응답합니다.
            읽음 처리하면 홈 조회의 hasUnreadNotification에도 바로 반영됩니다.
            - 알림이 없거나 다른 회원의 알림이면 40701(404)
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - notificationId (UUID, 필수, Path): 알림 ID

            **응답값**
            - 없음 (data = null)
            """
    )
    @PatchMapping("/{notificationId}/read")
    public CommonResponse<Void> readNotification(@CurrentUserId UUID userId, @PathVariable UUID notificationId) {
        notificationUseCase.readNotification(userId, notificationId);
        return CommonResponse.success(NotificationResponseCode.NOTIFICATION_READ);
    }
}
