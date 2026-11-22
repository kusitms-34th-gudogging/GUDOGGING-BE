package com.subscribers.gudogging.domain.gmail.domain;

/**
 * 인증 코드 교환 결과. googleSub · email은 응답 id_token의 sub · email.
 */
public record GmailTokens(
    String refreshToken,
    String googleSub,
    String email
) {
}
