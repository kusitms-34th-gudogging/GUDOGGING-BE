package com.subscribers.gudogging.domain.notification.presentation;

import com.subscribers.gudogging.domain.notification.application.dto.request.RegisterPushTokenRequest;
import com.subscribers.gudogging.domain.notification.application.dto.request.UpdateNotificationSettingRequest;
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
            알림 설정(E-05)의 결제 3일 전 점검 알림(D-3) 수신 여부를 조회합니다. 기본값은 true입니다.
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
}
