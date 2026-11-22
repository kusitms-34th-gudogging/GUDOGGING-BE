package com.subscribers.gudogging.domain.gmail.domain;

public interface GmailOAuthClient {

    /**
     * 앱이 받은 서버용 인증 코드를 Google에 보내 Gmail 읽기용 refresh token과 계정 정보로 교환한다.
     */
    GmailTokens exchangeAuthCode(String authCode);

    /**
     * 저장해 둔 refresh token을 Google에서 무효화한다. 이미 무효인 토큰은 성공으로 본다.
     */
    void revokeToken(String refreshToken);
}
