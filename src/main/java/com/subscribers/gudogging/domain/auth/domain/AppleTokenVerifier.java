package com.subscribers.gudogging.domain.auth.domain;

public interface AppleTokenVerifier {

    AppleUserInfo verify(String identityToken);
}
