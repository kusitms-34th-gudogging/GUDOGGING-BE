package com.subscribers.gudogging.domain.gmail.application.usecase;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.gmail.application.dto.request.ConnectGmailRequest;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailAnalysisResponse;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailConnectionResponse;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailConnectionNotFoundException;
import com.subscribers.gudogging.domain.gmail.domain.GmailCandidate;
import com.subscribers.gudogging.domain.gmail.domain.GmailExtractedPayment;
import com.subscribers.gudogging.domain.gmail.domain.GmailMail;
import com.subscribers.gudogging.domain.gmail.domain.GmailMailReader;
import com.subscribers.gudogging.domain.gmail.domain.GmailOAuthClient;
import com.subscribers.gudogging.domain.gmail.domain.GmailPaymentAnalyzer;
import com.subscribers.gudogging.domain.gmail.domain.GmailTokens;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionCreateService;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GmailUseCase {

    private final UserGetService userGetService;
    private final GmailConnectionGetService gmailConnectionGetService;
    private final GmailConnectionCreateService gmailConnectionCreateService;
    private final GmailOAuthClient gmailOAuthClient;
    private final GmailMailReader gmailMailReader;
    private final GmailPaymentAnalyzer gmailPaymentAnalyzer;
    private final ServiceCatalogGetService serviceCatalogGetService;
    private final SubscriptionGetService subscriptionGetService;

    /**
     * 인증 코드를 Google 토큰으로 바꾼 뒤 연결을 저장한다. 이미 연결이 있으면 기존 토큰을 revoke하고 새 연결로 교체한다.
     */
    @Transactional
    public GmailConnectionResponse connectGmail(UUID userId, ConnectGmailRequest request) {
        User user = findUser(userId);
        GmailTokens tokens = gmailOAuthClient.exchangeAuthCode(request.authCode());

        gmailConnectionGetService.findGmailConnection(user)
            .ifPresent(this::replaceConnection);

        GmailConnection saved = gmailConnectionCreateService.saveGmailConnection(
            GmailConnection.create(user, tokens.googleSub(), tokens.email(), tokens.refreshToken())
        );
        return GmailConnectionResponse.from(saved);
    }

    public GmailConnectionResponse getGmailConnection(UUID userId) {
        User user = findUser(userId);
        return GmailConnectionResponse.from(gmailConnectionGetService.findGmailConnection(user));
    }

    /**
     * Google 토큰을 revoke한 뒤 연결을 삭제한다. revoke가 실패하면 연결을 그대로 둔다.
     */
    @Transactional
    public void disconnectGmail(UUID userId) {
        User user = findUser(userId);
        GmailConnection connection = gmailConnectionGetService.findGmailConnection(user)
            .orElseThrow(GmailConnectionNotFoundException::new);

        gmailOAuthClient.revokeToken(connection.getRefreshToken());
        connection.softDelete();
    }

    /**
     * 연동된 Gmail의 결제 메일을 LLM으로 분석해 구독 후보를 만든다 (A-12 → A-13). 저장하지 않는다.
     * 서비스 목록에 있는 요금제만, 이미 구독 중인 서비스는 빼고, 같은 서비스는 결제일이 가장 최근인 1건만 남긴다. 후보가 없으면 빈 목록.
     */
    @Transactional
    public GmailAnalysisResponse analyzeGmailPayments(UUID userId) {
        User user = findUser(userId);
        GmailConnection connection = gmailConnectionGetService.findGmailConnection(user)
            .orElseThrow(GmailConnectionNotFoundException::new);
        LocalDate today = LocalDate.now();

        List<GmailMail> mails = gmailMailReader.readPaymentMails(connection.getRefreshToken());
        connection.markSynced();
        if (mails.isEmpty()) {
            return GmailAnalysisResponse.from(List.of());
        }

        Map<String, ServiceCatalog> servicesByName = serviceCatalogGetService.findActiveServices().stream()
            .collect(Collectors.toMap(ServiceCatalog::getName, Function.identity(), (first, second) -> first,
                LinkedHashMap::new));
        List<GmailExtractedPayment> payments = gmailPaymentAnalyzer.analyze(
            mails, List.copyOf(servicesByName.keySet()), today);

        Set<UUID> activeServiceIds = subscriptionGetService.findActiveServiceIds(user);
        List<GmailCandidate> candidates = payments.stream()
            .filter(payment -> payment.serviceName() != null && servicesByName.containsKey(payment.serviceName()))
            .map(payment -> GmailCandidate.of(servicesByName.get(payment.serviceName()), payment, today))
            .filter(candidate -> !activeServiceIds.contains(candidate.service().getId()))
            .collect(Collectors.toMap(candidate -> candidate.service().getId(), Function.identity(),
                GmailUseCase::latestPaid, LinkedHashMap::new))
            .values().stream()
            .sorted(Comparator.comparing(GmailCandidate::paidDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        log.info("Gmail 결제 메일 분석: mails={}, extracted={}, candidates={}", mails.size(), payments.size(),
            candidates.size());
        return GmailAnalysisResponse.from(candidates);
    }

    private static GmailCandidate latestPaid(GmailCandidate first, GmailCandidate second) {
        if (first.paidDate() == null) {
            return second;
        }
        if (second.paidDate() == null) {
            return first;
        }
        return second.paidDate().isAfter(first.paidDate()) ? second : first;
    }

    private void replaceConnection(GmailConnection connection) {
        try {
            gmailOAuthClient.revokeToken(connection.getRefreshToken());
        } catch (RuntimeException e) {
            log.warn("재연동 중 기존 Gmail 토큰 revoke 실패, 연결은 교체합니다: connectionId={}", connection.getId());
        }
        connection.softDelete();
    }

    private User findUser(UUID userId) {
        return userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
    }
}
