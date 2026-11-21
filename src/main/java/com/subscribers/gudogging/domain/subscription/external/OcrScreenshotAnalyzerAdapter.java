package com.subscribers.gudogging.domain.subscription.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.subscribers.gudogging.domain.subscription.application.exception.ScreenshotAnalysisFailedException;
import com.subscribers.gudogging.domain.subscription.application.exception.ScreenshotTooLargeException;
import com.subscribers.gudogging.domain.subscription.application.exception.UnsupportedScreenshotFormatException;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.ScreenshotCandidate;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.ScreenshotImage;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.ScreenshotReviewField;
import com.subscribers.gudogging.domain.subscription.domain.screenshot.SubscriptionScreenshotAnalyzer;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * OCR 서버(POST /v2/ocr) 호출. OCR 서버가 서비스 매칭까지 해서 서비스 ID가 채워진 후보를 돌려준다 (AI_SERVER_GUIDE.md). 이미지와 인식 결과(서비스명·금액)는 로그에 남기지
 * 않는다.
 */
@Slf4j
@Component
public class OcrScreenshotAnalyzerAdapter implements SubscriptionScreenshotAnalyzer {

    private static final String OCR_PATH = "/v2/ocr";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String IMAGES_PART = "images";
    private static final String REFERENCE_DATE_PART = "ref_date";
    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;

    public OcrScreenshotAnalyzerAdapter(
        @Value("${ocr.base-url}") String baseUrl,
        @Value("${ocr.api-key}") String apiKey,
        @Value("${ocr.connect-timeout}") Duration connectTimeout,
        @Value("${ocr.read-timeout}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(API_KEY_HEADER, apiKey)
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public List<ScreenshotCandidate> analyzeScreenshots(List<ScreenshotImage> images, LocalDate referenceDate) {
        MultiValueMap<String, ?> body = multipartBody(images, referenceDate);
        long startedAt = System.currentTimeMillis();
        OcrResponse response = requestWithRetry(body);
        if (response == null) {
            log.warn("OCR 응답 본문이 없습니다.");
            throw new ScreenshotAnalysisFailedException();
        }

        List<OcrSubscription> subscriptions = response.subscriptions() != null ? response.subscriptions() : List.of();
        List<ScreenshotCandidate> candidates = subscriptions.stream()
            .map(this::toCandidate)
            .filter(Objects::nonNull)
            .toList();
        log.info("OCR 분석 완료: images={}, candidates={}, skipped={}, modelVersion={}, catalogVersion={}, elapsedMs={}",
            images.size(), candidates.size(), subscriptions.size() - candidates.size(),
            response.modelVersion(), response.catalogVersion(), System.currentTimeMillis() - startedAt);
        return candidates;
    }

    /**
     * 업로드 순서 = 스크롤 순서이므로 순서를 바꾸지 않고 담는다.
     */
    private MultiValueMap<String, ?> multipartBody(List<ScreenshotImage> images, LocalDate referenceDate) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        for (ScreenshotImage image : images) {
            builder.part(IMAGES_PART, new NamedByteArrayResource(image.content(), image.filename()))
                .contentType(MediaType.parseMediaType(image.contentType()));
        }
        builder.part(REFERENCE_DATE_PART, referenceDate.toString());
        return builder.build();
    }

    /**
     * 5xx · 연결 실패 · 타임아웃은 한 번 더 시도한다. 4xx는 다시 보내도 같으므로 바로 변환한다.
     */
    private OcrResponse requestWithRetry(MultiValueMap<String, ?> body) {
        for (int attempt = 1; ; attempt++) {
            try {
                return restClient.post()
                    .uri(OCR_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrResponse.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (!e.getStatusCode().is5xxServerError() || attempt >= MAX_ATTEMPTS) {
                    throw toException(status);
                }
                log.warn("OCR 호출 실패, 재시도: status={}, attempt={}", status, attempt);
            } catch (RestClientException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("OCR 호출 실패: {}", e.getClass().getSimpleName());
                    throw new ScreenshotAnalysisFailedException();
                }
                log.warn("OCR 호출 실패, 재시도: {}, attempt={}", e.getClass().getSimpleName(), attempt);
            }
        }
    }

    private RuntimeException toException(int status) {
        if (status == HttpStatus.CONTENT_TOO_LARGE.value()) {
            return new ScreenshotTooLargeException();
        }
        if (status == HttpStatus.UNPROCESSABLE_CONTENT.value()) {
            return new UnsupportedScreenshotFormatException();
        }
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            log.error("OCR 서버 API 키가 거부되었습니다. ocr.api-key 설정을 확인하세요.");
        } else {
            log.warn("OCR 호출 실패: status={}", status);
        }
        return new ScreenshotAnalysisFailedException();
    }

    /**
     * 서비스 ID를 해석하지 못한 항목은 건너뛴다. 나머지 값은 해석하지 못하면 null로 두고 ScreenshotCandidate가 확인 필요로 표시한다.
     */
    private ScreenshotCandidate toCandidate(OcrSubscription subscription) {
        UUID serviceId = parseUuid(subscription.serviceId());
        if (serviceId == null) {
            log.warn("OCR 후보의 service_id를 해석하지 못해 제외합니다.");
            return null;
        }
        return ScreenshotCandidate.of(
            serviceId,
            subscription.amount(),
            subscription.billingCycleValue(),
            parseEnum(BillingCycleUnit.class, subscription.billingCycleUnit()),
            parseDate(subscription.nextBillingDate()),
            parseEnum(PaymentMethod.class, subscription.paymentMethod()),
            Boolean.TRUE.equals(subscription.needsReview()),
            toReviewFields(subscription.reviewFields())
        );
    }

    /**
     * OCR review_fields 중 화면 필드에 대응하는 값만 쓴다. status 등 항목 전체 확인 값은 needsReview로만 반영된다.
     */
    private List<ScreenshotReviewField> toReviewFields(List<String> reviewFields) {
        if (reviewFields == null) {
            return List.of();
        }
        List<ScreenshotReviewField> fields = new ArrayList<>();
        for (String field : reviewFields) {
            switch (field) {
                case "service" -> fields.add(ScreenshotReviewField.SERVICE);
                case "amount" -> fields.add(ScreenshotReviewField.AMOUNT);
                case "billing_cycle" -> fields.add(ScreenshotReviewField.BILLING_CYCLE);
                case "next_billing_date" -> fields.add(ScreenshotReviewField.NEXT_BILLING_DATE);
                case null, default -> {
                }
            }
        }
        return fields;
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * multipart 파트에 파일명을 붙이기 위한 리소스 (파일명이 없으면 OCR 서버가 파일 파트로 인식하지 않는다).
     */
    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] content, String filename) {
            super(content);
            this.filename = filename != null && !filename.isBlank() ? filename : "screenshot";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrResponse(
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("catalog_version") String catalogVersion,
        List<OcrSubscription> subscriptions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrSubscription(
        @JsonProperty("service_id") String serviceId,
        Integer amount,
        @JsonProperty("billing_cycle_value") Integer billingCycleValue,
        @JsonProperty("billing_cycle_unit") String billingCycleUnit,
        @JsonProperty("next_billing_date") String nextBillingDate,
        @JsonProperty("payment_method") String paymentMethod,
        @JsonProperty("needs_review") Boolean needsReview,
        @JsonProperty("review_fields") List<String> reviewFields
    ) {
    }
}
