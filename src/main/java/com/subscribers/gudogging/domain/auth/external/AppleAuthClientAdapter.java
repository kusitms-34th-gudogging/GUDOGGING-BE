package com.subscribers.gudogging.domain.auth.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWTParser;
import com.subscribers.gudogging.domain.auth.application.exception.AppleServerException;
import com.subscribers.gudogging.domain.auth.application.exception.InvalidAppleAuthorizationCodeException;
import com.subscribers.gudogging.domain.auth.domain.AppleAuthClient;
import com.subscribers.gudogging.domain.auth.domain.AppleTokens;
import io.jsonwebtoken.Jwts;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
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
public class AppleAuthClientAdapter implements AppleAuthClient {

    private static final String APPLE_BASE_URL = "https://appleid.apple.com";
    private static final String TOKEN_PATH = "/auth/token";
    private static final String REVOKE_PATH = "/auth/revoke";
    private static final String CLIENT_SECRET_AUDIENCE = "https://appleid.apple.com";
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String INVALID_GRANT = "invalid_grant";

    private final RestClient restClient;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final PrivateKey privateKey;

    public AppleAuthClientAdapter(
        @Value("${apple.client-id}") String clientId,
        @Value("${apple.team-id}") String teamId,
        @Value("${apple.key-id}") String keyId,
        @Value("${apple.private-key}") String privateKeyPem
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
            .baseUrl(APPLE_BASE_URL)
            .requestFactory(requestFactory)
            .build();
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = parsePrivateKey(privateKeyPem);
    }

    @Override
    public AppleTokens issueTokens(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", createClientSecret());
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");

        AppleTokenResponse response;
        try {
            response = restClient.post()
                .uri(TOKEN_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AppleTokenResponse.class);
        } catch (RestClientResponseException e) {
            String error = extractError(e);
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value() && INVALID_GRANT.equals(error)) {
                throw new InvalidAppleAuthorizationCodeException();
            }
            log.warn("Apple token 교환 실패: status={}, error={}", e.getStatusCode().value(), error);
            throw new AppleServerException();
        } catch (RestClientException e) {
            log.warn("Apple token 교환 실패: {}", e.getClass().getSimpleName());
            throw new AppleServerException();
        }

        if (response == null || response.refreshToken() == null || response.idToken() == null) {
            log.warn("Apple token 응답에 refresh_token 또는 id_token이 없습니다.");
            throw new AppleServerException();
        }
        return new AppleTokens(extractSubject(response.idToken()), response.refreshToken());
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", createClientSecret());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                .uri(REVOKE_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Apple token revoke 실패: status={}, error={}", e.getStatusCode().value(), extractError(e));
            throw new AppleServerException();
        } catch (RestClientException e) {
            log.warn("Apple token revoke 실패: {}", e.getClass().getSimpleName());
            throw new AppleServerException();
        }
    }

    /**
     * Apple REST API 호출용 client_secret(ES256 JWT). 요청마다 새로 만들고 5분 뒤 만료된다.
     * aud는 Apple 문서대로 배열이 아닌 문자열로 넣어야 하므로 jjwt의 audience().single()을 사용한다.
     */
    @SuppressWarnings("deprecation")
    private String createClientSecret() {
        Instant now = Instant.now();
        return Jwts.builder()
            .header().keyId(keyId).and()
            .issuer(teamId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(CLIENT_SECRET_TTL)))
            .audience().single(CLIENT_SECRET_AUDIENCE)
            .subject(clientId)
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact();
    }

    /**
     * id_token은 TLS로 Apple에서 직접 받은 값이므로 서명 검증 없이 sub만 꺼낸다.
     */
    private String extractSubject(String idToken) {
        try {
            String sub = JWTParser.parse(idToken).getJWTClaimsSet().getSubject();
            if (sub == null) {
                throw new AppleServerException();
            }
            return sub;
        } catch (ParseException e) {
            log.warn("Apple token 응답의 id_token을 해석하지 못했습니다.");
            throw new AppleServerException();
        }
    }

    private String extractError(RestClientResponseException e) {
        try {
            AppleErrorResponse body = e.getResponseBodyAs(AppleErrorResponse.class);
            return body == null ? null : body.error();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * .p8 파일 내용(PKCS#8 PEM)을 PrivateKey로 변환한다. 환경변수에 줄바꿈이 '\n' 문자열로 들어온 경우도 처리한다.
     */
    private static PrivateKey parsePrivateKey(String pem) {
        String base64 = pem
            .replace("\\n", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("apple.private-key를 EC 개인키(PKCS#8 PEM)로 읽을 수 없습니다.", e);
        }
    }

    private record AppleTokenResponse(
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken
    ) {
    }

    private record AppleErrorResponse(String error) {
    }
}
