package com.subscribers.gudogging.domain.notification.application.dto.request;

import com.subscribers.gudogging.domain.user.domain.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 알림 권한 요청(O-03)에서 권한을 허용했을 때 등록하는 FCM 기기 토큰과 기기 종류.
 */
public record RegisterPushTokenRequest(
    @NotBlank @Size(max = 255) String deviceToken,
    @NotNull Platform platform
) {
}
