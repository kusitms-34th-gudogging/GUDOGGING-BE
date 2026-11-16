package com.subscribers.gudogging.domain.auth.domain;

/**
 * authorizationCode 교환 결과. sub는 응답 id_token의 sub.
 */
public record AppleTokens(
    String sub,
    String refreshToken
) {
}
