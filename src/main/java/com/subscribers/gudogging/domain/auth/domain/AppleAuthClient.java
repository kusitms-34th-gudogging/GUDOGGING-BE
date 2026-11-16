package com.subscribers.gudogging.domain.auth.domain;

public interface AppleAuthClient {

    /**
     * authorizationCode를 Apple에 보내 refresh token으로 교환한다.
     */
    AppleTokens issueTokens(String authorizationCode);

    /**
     * 저장해 둔 Apple refresh token을 무효화한다. (회원 탈퇴, USER-001)
     */
    void revokeRefreshToken(String refreshToken);
}
