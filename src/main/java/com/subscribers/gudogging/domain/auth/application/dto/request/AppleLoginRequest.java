package com.subscribers.gudogging.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppleLoginRequest(
    @NotBlank String identityToken,
    @NotBlank String authorizationCode,
    @Size(max = 100) String name
) {
    /**
     * 공백뿐인 이름은 null로 취급한다.
     */
    public String nameOrNull() {
        return name == null || name.isBlank() ? null : name;
    }
}
