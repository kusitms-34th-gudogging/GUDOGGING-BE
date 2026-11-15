package com.subscribers.gudogging.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank String idToken
) {
}
