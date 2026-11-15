package com.subscribers.gudogging.domain.auth.domain;

public interface GoogleTokenVerifier {

    GoogleUserInfo verify(String idToken);
}
