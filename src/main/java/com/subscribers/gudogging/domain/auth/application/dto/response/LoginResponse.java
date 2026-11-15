package com.subscribers.gudogging.domain.auth.application.dto.response;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    boolean isNewUser
) {
    public static LoginResponse of(String accessToken, String refreshToken, boolean isNewUser) {
        return new LoginResponse(accessToken, refreshToken, isNewUser);
    }
}
