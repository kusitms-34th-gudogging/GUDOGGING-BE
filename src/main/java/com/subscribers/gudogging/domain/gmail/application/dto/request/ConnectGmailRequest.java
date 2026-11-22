package com.subscribers.gudogging.domain.gmail.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConnectGmailRequest(
    @NotBlank String authCode
) {
}
