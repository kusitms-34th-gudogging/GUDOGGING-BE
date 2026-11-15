package com.subscribers.gudogging.domain.auth.domain;

public record GoogleUserInfo(
    String sub,
    String email,
    String name
) {
}
