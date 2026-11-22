package com.subscribers.gudogging.domain.gmail.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailPermissionRequiredException;
import com.subscribers.gudogging.domain.gmail.application.exception.GoogleOAuthServerException;
import com.subscribers.gudogging.domain.gmail.application.exception.InvalidGmailAuthCodeException;
import com.subscribers.gudogging.domain.gmail.domain.GmailOAuthClient;
import com.subscribers.gudogging.domain.gmail.domain.GmailTokens;
import java.net.http.HttpClient;
import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class GoogleGmailOAuthClientAdapter implements GmailOAuthClient {

    private static final String GOOGLE_OAUTH_BASE_URL = "https://oauth2.googleapis.com";
    private static final String TOKEN_PATH = "/token";
    private static final String REVOKE_PATH = "/revoke";
    private static final String GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String INVALID_GRANT = "invalid_grant";
    private static final String INVALID_TOKEN = "invalid_token";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleGmailOAuthClientAdapter(
        @Value("${google.gmail.client-id}") String clientId,
        @Value("${google.gmail.client-secret}") String clientSecret,
        @Value("${google.gmail.redirect-uri:}") String redirectUri
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
            .baseUrl(GOOGLE_OAUTH_BASE_URL)
            .requestFactory(requestFactory)
            .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public GmailTokens exchangeAuthCode(String authCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", authCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        GoogleTokenResponse response;
        try {
            response = restClient.post()
                .uri(TOKEN_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
        } catch (RestClientResponseException e) {
            String error = extractError(e);
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value() && INVALID_GRANT.equals(error)) {
                throw new InvalidGmailAuthCodeException();
            }
            log.warn("Google token 교환 실패: status={}, error={}", e.getStatusCode().value(), error);
            throw new GoogleOAuthServerException();
        } catch (RestClientException e) {
            log.warn("Google token 교환 실패: {}", e.getClass().getSimpleName());
            throw new GoogleOAuthServerException();
        }

        if (response == null) {
            throw new GoogleOAuthServerException();
        }
        if (!hasGmailReadonlyScope(response.scope())) {
            log.info("Gmail 읽기 권한 없이 인증 코드가 전달됐습니다.");
            throw new GmailPermissionRequiredException();
        }
        if (response.refreshToken() == null || response.idToken() == null) {
            log.info("Google token 응답에 refresh_token 또는 id_token이 없습니다.");
            throw new GmailPermissionRequiredException();
        }
        return toTokens(response);
    }

    @Override
    public void revokeToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);

        try {
            restClient.post()
                .uri(REVOKE_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            String error = extractError(e);
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value() && INVALID_TOKEN.equals(error)) {
                log.info("이미 무효인 Gmail refresh token입니다. revoke 성공으로 처리합니다.");
                return;
            }
            log.warn("Google token revoke 실패: status={}, error={}", e.getStatusCode().value(), error);
            throw new GoogleOAuthServerException();
        } catch (RestClientException e) {
            log.warn("Google token revoke 실패: {}", e.getClass().getSimpleName());
            throw new GoogleOAuthServerException();
        }
    }

    private boolean hasGmailReadonlyScope(String scope) {
        return scope != null && Arrays.asList(scope.split(" ")).contains(GMAIL_READONLY_SCOPE);
    }

    /**
     * id_token은 TLS로 Google 토큰 엔드포인트에서 직접 받은 값이므로 서명 검증 없이 sub · email만 꺼낸다.
     */
    private GmailTokens toTokens(GoogleTokenResponse response) {
        try {
            JWTClaimsSet claims = JWTParser.parse(response.idToken()).getJWTClaimsSet();
            String sub = claims.getSubject();
            String email = claims.getStringClaim("email");
            if (sub == null || email == null) {
                log.info("Google id_token에 sub 또는 email이 없습니다.");
                throw new GmailPermissionRequiredException();
            }
            return new GmailTokens(response.refreshToken(), sub, email);
        } catch (ParseException e) {
            log.warn("Google token 응답의 id_token을 해석하지 못했습니다.");
            throw new GoogleOAuthServerException();
        }
    }

    private String extractError(RestClientResponseException e) {
        try {
            GoogleErrorResponse body = e.getResponseBodyAs(GoogleErrorResponse.class);
            return body == null ? null : body.error();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record GoogleTokenResponse(
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken,
        String scope
    ) {
    }

    private record GoogleErrorResponse(String error) {
    }
}
