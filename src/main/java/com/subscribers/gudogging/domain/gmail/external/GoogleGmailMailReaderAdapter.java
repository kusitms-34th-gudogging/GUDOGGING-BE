package com.subscribers.gudogging.domain.gmail.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailAnalysisFailedException;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailReconnectRequiredException;
import com.subscribers.gudogging.domain.gmail.domain.GmailMail;
import com.subscribers.gudogging.domain.gmail.domain.GmailMailReader;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.web.util.HtmlUtils;

/**
 * Gmail API(REST)로 결제 · 구독 관련 메일을 읽는다. 메일 내용은 로그에 남기지 않는다.
 */
@Slf4j
@Component
public class GoogleGmailMailReaderAdapter implements GmailMailReader {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_BASE_URL = "https://gmail.googleapis.com/gmail/v1/users/me";
    private static final String SEARCH_QUERY = "newer_than:60d (category:purchases OR 결제 OR 영수증 OR 구독 OR 정기결제 "
        + "OR receipt OR invoice OR subscription OR renewal)";
    private static final int MAX_MAILS = 50;
    private static final int MAX_BODY_LENGTH = 2_000;
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String INVALID_GRANT = "invalid_grant";
    private static final Pattern CHARSET = Pattern.compile("charset=\"?([\\w-]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVISIBLE_BLOCK = Pattern.compile("(?is)<(style|script|head)[^>]*>.*?</\\1>");
    private static final Pattern LINE_BREAK_TAG = Pattern.compile("(?i)<(br|/p|/div|/tr|/li|/h\\d)[^>]*>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SPACES = Pattern.compile("[ \\t\\x0B\\f\\u00A0]+");
    private static final Pattern BLANK_LINES = Pattern.compile("\\s*\\n\\s*");

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public GoogleGmailMailReaderAdapter(
        @Value("${google.gmail.client-id}") String clientId,
        @Value("${google.gmail.client-secret}") String clientSecret
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
            .requestFactory(requestFactory)
            .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public List<GmailMail> readPaymentMails(String refreshToken) {
        String accessToken = refreshAccessToken(refreshToken);
        List<String> messageIds = searchMessageIds(accessToken);
        if (messageIds.isEmpty()) {
            return List.of();
        }
        return fetchMails(accessToken, messageIds);
    }

    private String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        try {
            AccessTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AccessTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new GmailAnalysisFailedException();
            }
            return response.accessToken();
        } catch (RestClientResponseException e) {
            String error = extractError(e);
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value() && INVALID_GRANT.equals(error)) {
                log.info("Gmail refresh token이 무효입니다. 다시 연결이 필요합니다.");
                throw new GmailReconnectRequiredException();
            }
            log.warn("Gmail access token 갱신 실패: status={}, error={}", e.getStatusCode().value(), error);
            throw new GmailAnalysisFailedException();
        } catch (RestClientException e) {
            log.warn("Gmail access token 갱신 실패: {}", e.getClass().getSimpleName());
            throw new GmailAnalysisFailedException();
        }
    }

    private List<String> searchMessageIds(String accessToken) {
        MessageListResponse response = getGmail(accessToken, GMAIL_BASE_URL + "/messages?maxResults={max}&q={q}",
            MessageListResponse.class, MAX_MAILS, SEARCH_QUERY);
        if (response == null || response.messages() == null) {
            return List.of();
        }
        return response.messages().stream()
            .map(MessageRef::id)
            .filter(Objects::nonNull)
            .limit(MAX_MAILS)
            .toList();
    }

    private List<GmailMail> fetchMails(String accessToken, List<String> messageIds) {
        Semaphore permits = new Semaphore(MAX_CONCURRENT_REQUESTS);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<GmailMail>> futures = new ArrayList<>();
            for (String messageId : messageIds) {
                futures.add(executor.submit(() -> {
                    permits.acquire();
                    try {
                        return fetchMail(accessToken, messageId);
                    } finally {
                        permits.release();
                    }
                }));
            }
            List<GmailMail> mails = new ArrayList<>();
            for (Future<GmailMail> future : futures) {
                mails.add(future.get());
            }
            return mails;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new GmailAnalysisFailedException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GmailAnalysisFailedException();
        }
    }

    private GmailMail fetchMail(String accessToken, String messageId) {
        MessageResponse message = getGmail(accessToken, GMAIL_BASE_URL + "/messages/{id}?format=full",
            MessageResponse.class, messageId);
        if (message == null || message.payload() == null) {
            throw new GmailAnalysisFailedException();
        }
        MessagePart payload = message.payload();
        LocalDate receivedOn = message.internalDate() == null ? null
            : Instant.ofEpochMilli(Long.parseLong(message.internalDate())).atZone(ZONE).toLocalDate();
        return new GmailMail(header(payload, "From"), header(payload, "Subject"), receivedOn, bodyText(payload));
    }

    private <T> T getGmail(String accessToken, String uri, Class<T> type, Object... uriVariables) {
        try {
            return restClient.get()
                .uri(uri, uriVariables)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(type);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                log.info("Gmail API 권한 오류: status={}", status);
                throw new GmailReconnectRequiredException();
            }
            log.warn("Gmail API 호출 실패: status={}", status);
            throw new GmailAnalysisFailedException();
        } catch (RestClientException e) {
            log.warn("Gmail API 호출 실패: {}", e.getClass().getSimpleName());
            throw new GmailAnalysisFailedException();
        }
    }

    private String header(MessagePart part, String name) {
        if (part.headers() == null) {
            return null;
        }
        return part.headers().stream()
            .filter(header -> name.equalsIgnoreCase(header.name()))
            .map(Header::value)
            .findFirst()
            .orElse(null);
    }

    /**
     * text/plain을 우선 쓰고, 없으면 text/html에서 태그를 뺀다. 앞 2,000자만 남긴다.
     */
    private String bodyText(MessagePart payload) {
        String text = findPart(payload, "text/plain")
            .map(this::decode)
            .filter(plain -> !plain.isBlank())
            .or(() -> findPart(payload, "text/html").map(this::decode).map(this::htmlToText))
            .orElse("");
        String normalized = BLANK_LINES.matcher(SPACES.matcher(text).replaceAll(" ")).replaceAll("\n").strip();
        return normalized.length() > MAX_BODY_LENGTH ? normalized.substring(0, MAX_BODY_LENGTH) : normalized;
    }

    private Optional<MessagePart> findPart(MessagePart part, String mimeType) {
        if (mimeType.equalsIgnoreCase(part.mimeType()) && part.body() != null && part.body().data() != null) {
            return Optional.of(part);
        }
        if (part.parts() == null) {
            return Optional.empty();
        }
        for (MessagePart child : part.parts()) {
            Optional<MessagePart> found = findPart(child, mimeType);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private String decode(MessagePart part) {
        byte[] bytes = Base64.getUrlDecoder().decode(part.body().data());
        return new String(bytes, charsetOf(part));
    }

    private Charset charsetOf(MessagePart part) {
        String contentType = header(part, "Content-Type");
        if (contentType != null) {
            Matcher matcher = CHARSET.matcher(contentType);
            if (matcher.find()) {
                try {
                    return Charset.forName(matcher.group(1));
                } catch (IllegalArgumentException ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String htmlToText(String html) {
        String withoutBlocks = INVISIBLE_BLOCK.matcher(html).replaceAll(" ");
        String withBreaks = LINE_BREAK_TAG.matcher(withoutBlocks).replaceAll("\n");
        return HtmlUtils.htmlUnescape(TAG.matcher(withBreaks).replaceAll(" "));
    }

    private String extractError(RestClientResponseException e) {
        try {
            ErrorResponse body = e.getResponseBodyAs(ErrorResponse.class);
            return body == null ? null : body.error();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record AccessTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record ErrorResponse(String error) {
    }

    private record MessageListResponse(List<MessageRef> messages) {
    }

    private record MessageRef(String id) {
    }

    private record MessageResponse(String internalDate, MessagePart payload) {
    }

    private record MessagePart(String mimeType, List<Header> headers, PartBody body, List<MessagePart> parts) {
    }

    private record PartBody(String data) {
    }

    private record Header(String name, String value) {
    }
}
