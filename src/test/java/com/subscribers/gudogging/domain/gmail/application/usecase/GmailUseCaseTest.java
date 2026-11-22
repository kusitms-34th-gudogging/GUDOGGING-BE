package com.subscribers.gudogging.domain.gmail.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.gmail.application.dto.request.ConnectGmailRequest;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailAnalysisResponse;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailCandidateResponse;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailConnectionResponse;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailConnectionNotFoundException;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailErrorCode;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailPermissionRequiredException;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailReconnectRequiredException;
import com.subscribers.gudogging.domain.gmail.application.exception.GoogleOAuthServerException;
import com.subscribers.gudogging.domain.gmail.application.exception.InvalidGmailAuthCodeException;
import com.subscribers.gudogging.domain.gmail.domain.GmailExtractedPayment;
import com.subscribers.gudogging.domain.gmail.domain.GmailMail;
import com.subscribers.gudogging.domain.gmail.domain.GmailMailReader;
import com.subscribers.gudogging.domain.gmail.domain.GmailOAuthClient;
import com.subscribers.gudogging.domain.gmail.domain.GmailPaymentAnalyzer;
import com.subscribers.gudogging.domain.gmail.domain.GmailReviewField;
import com.subscribers.gudogging.domain.gmail.domain.GmailTokens;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnectionStatus;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionCreateService;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionGetService;
import com.subscribers.gudogging.domain.subscription.domain.entity.BillingCycleUnit;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailUseCase")
class GmailUseCaseTest {

    private static final String AUTH_CODE = "4/0AX4XfWh-auth-code";
    private static final String GOOGLE_SUB = "google-sub-456";
    private static final String GMAIL_ADDRESS = "payments@gmail.com";
    private static final String REFRESH_TOKEN = "gmail-refresh-token";
    private static final String OLD_REFRESH_TOKEN = "old-gmail-refresh-token";
    private static final String OTHER_GOOGLE_SUB = "other-google-sub-789";

    @Mock
    private UserGetService userGetService;

    @Mock
    private GmailConnectionGetService gmailConnectionGetService;

    @Mock
    private GmailConnectionCreateService gmailConnectionCreateService;

    @Mock
    private GmailOAuthClient gmailOAuthClient;

    @Mock
    private GmailMailReader gmailMailReader;

    @Mock
    private GmailPaymentAnalyzer gmailPaymentAnalyzer;

    @Mock
    private ServiceCatalogGetService serviceCatalogGetService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @InjectMocks
    private GmailUseCase gmailUseCase;

    private final UUID userId = UUID.randomUUID();

    private User givenUser() {
        User user = User.create(SocialProvider.GOOGLE, "login-sub-123", "login@gmail.com", "홍길동");
        given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
        return user;
    }

    private void givenUserNotFound() {
        given(userGetService.findUserById(userId)).willReturn(Optional.empty());
    }

    private GmailConnection givenConnection(User user, String refreshToken) {
        return givenConnection(user, GOOGLE_SUB, refreshToken);
    }

    private GmailConnection givenConnection(User user, String googleSub, String refreshToken) {
        GmailConnection connection = GmailConnection.create(user, googleSub, GMAIL_ADDRESS, refreshToken);
        given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.of(connection));
        return connection;
    }

    private void assertErrorCode(Throwable thrown, Object errorCode) {
        assertThat(((ApplicationException) thrown).getErrorCode()).isEqualTo(errorCode);
    }

    @Nested
    @DisplayName("connectGmail")
    class ConnectGmail {

        private final ConnectGmailRequest request = new ConnectGmailRequest(AUTH_CODE);

        private void givenTokens() {
            given(gmailOAuthClient.exchangeAuthCode(AUTH_CODE))
                .willReturn(new GmailTokens(REFRESH_TOKEN, GOOGLE_SUB, GMAIL_ADDRESS));
        }

        private void givenSaved() {
            given(gmailConnectionCreateService.saveGmailConnection(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("인증 코드로 받은 refresh token · Gmail 주소 · 계정 ID로 연결을 저장하고 CONNECTED와 주소를 반환한다")
        void savesConnection() {
            // given
            User user = givenUser();
            givenTokens();
            givenSaved();

            // when
            GmailConnectionResponse response = gmailUseCase.connectGmail(userId, request);

            // then
            ArgumentCaptor<GmailConnection> captor = ArgumentCaptor.forClass(GmailConnection.class);
            verify(gmailConnectionCreateService).saveGmailConnection(captor.capture());
            GmailConnection saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getGoogleSub()).isEqualTo(GOOGLE_SUB);
            assertThat(saved.getGmailAddress()).isEqualTo(GMAIL_ADDRESS);
            assertThat(saved.getRefreshToken()).isEqualTo(REFRESH_TOKEN);

            assertThat(response.status()).isEqualTo(GmailConnectionStatus.CONNECTED);
            assertThat(response.connected()).isTrue();
            assertThat(response.email()).isEqualTo(GMAIL_ADDRESS);
            verify(gmailOAuthClient, never()).revokeToken(any());
        }

        @Test
        @DisplayName("다른 Google 계정으로 연결돼 있으면 기존 토큰을 revoke하고 기존 연결을 삭제한 뒤 새 연결을 저장한다")
        void replacesConnectionOfOtherAccount() {
            // given
            User user = givenUser();
            givenTokens();
            GmailConnection old = givenConnection(user, OTHER_GOOGLE_SUB, OLD_REFRESH_TOKEN);
            givenSaved();

            // when
            gmailUseCase.connectGmail(userId, request);

            // then
            verify(gmailOAuthClient).revokeToken(OLD_REFRESH_TOKEN);
            assertThat(old.isDeleted()).isTrue();
            verify(gmailConnectionCreateService).saveGmailConnection(any());
        }

        @Test
        @DisplayName("같은 Google 계정으로 다시 연결하면 revoke하지 않고 기존 연결만 새 연결로 교체한다")
        void replacesSameAccountWithoutRevoke() {
            // given
            User user = givenUser();
            givenTokens();
            GmailConnection old = givenConnection(user, GOOGLE_SUB, OLD_REFRESH_TOKEN);
            givenSaved();

            // when
            gmailUseCase.connectGmail(userId, request);

            // then
            verify(gmailOAuthClient, never()).revokeToken(any());
            assertThat(old.isDeleted()).isTrue();
            verify(gmailConnectionCreateService).saveGmailConnection(any());
        }

        @Test
        @DisplayName("기존 계정 토큰 revoke가 실패해도 기존 연결을 삭제하고 새 연결을 저장한다")
        void replacesEvenWhenRevokeFails() {
            // given
            User user = givenUser();
            givenTokens();
            GmailConnection old = givenConnection(user, OTHER_GOOGLE_SUB, OLD_REFRESH_TOKEN);
            willThrow(new GoogleOAuthServerException()).given(gmailOAuthClient).revokeToken(OLD_REFRESH_TOKEN);
            givenSaved();

            // when
            GmailConnectionResponse response = gmailUseCase.connectGmail(userId, request);

            // then
            assertThat(old.isDeleted()).isTrue();
            assertThat(response.status()).isEqualTo(GmailConnectionStatus.CONNECTED);
        }

        @Test
        @DisplayName("인증 코드가 잘못되면 INVALID_GMAIL_AUTH_CODE를 던지고 기존 연결을 건드리지 않는다")
        void throwsWhenAuthCodeInvalid() {
            // given
            givenUser();
            given(gmailOAuthClient.exchangeAuthCode(AUTH_CODE)).willThrow(new InvalidGmailAuthCodeException());

            // when & then
            assertThatThrownBy(() -> gmailUseCase.connectGmail(userId, request))
                .isInstanceOf(InvalidGmailAuthCodeException.class)
                .satisfies(e -> assertErrorCode(e, GmailErrorCode.INVALID_GMAIL_AUTH_CODE));
            verifyNoInteractions(gmailConnectionGetService, gmailConnectionCreateService);
        }

        @Test
        @DisplayName("Gmail 읽기 권한이 없으면 GMAIL_PERMISSION_REQUIRED를 던지고 저장하지 않는다")
        void throwsWhenPermissionMissing() {
            // given
            givenUser();
            given(gmailOAuthClient.exchangeAuthCode(AUTH_CODE)).willThrow(new GmailPermissionRequiredException());

            // when & then
            assertThatThrownBy(() -> gmailUseCase.connectGmail(userId, request))
                .isInstanceOf(GmailPermissionRequiredException.class)
                .satisfies(e -> assertErrorCode(e, GmailErrorCode.GMAIL_PERMISSION_REQUIRED));
            verifyNoInteractions(gmailConnectionCreateService);
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND를 던지고 Google을 호출하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            givenUserNotFound();

            // when & then
            assertThatThrownBy(() -> gmailUseCase.connectGmail(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .satisfies(e -> assertErrorCode(e, UserErrorCode.USER_NOT_FOUND));
            verifyNoInteractions(gmailOAuthClient, gmailConnectionCreateService);
        }
    }

    @Nested
    @DisplayName("getGmailConnection")
    class GetGmailConnection {

        @Test
        @DisplayName("연결이 있으면 CONNECTED, connected true, Gmail 주소를 반환한다")
        void returnsConnected() {
            // given
            User user = givenUser();
            givenConnection(user, REFRESH_TOKEN);

            // when
            GmailConnectionResponse response = gmailUseCase.getGmailConnection(userId);

            // then
            assertThat(response.status()).isEqualTo(GmailConnectionStatus.CONNECTED);
            assertThat(response.connected()).isTrue();
            assertThat(response.email()).isEqualTo(GMAIL_ADDRESS);
        }

        @Test
        @DisplayName("연결이 없으면 NOT_CONNECTED, connected false, email null을 반환한다")
        void returnsNotConnected() {
            // given
            User user = givenUser();
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.empty());

            // when
            GmailConnectionResponse response = gmailUseCase.getGmailConnection(userId);

            // then
            assertThat(response.status()).isEqualTo(GmailConnectionStatus.NOT_CONNECTED);
            assertThat(response.connected()).isFalse();
            assertThat(response.email()).isNull();
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenUserNotFound();

            // when & then
            assertThatThrownBy(() -> gmailUseCase.getGmailConnection(userId))
                .isInstanceOf(UserNotFoundException.class)
                .satisfies(e -> assertErrorCode(e, UserErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("disconnectGmail")
    class DisconnectGmail {

        @Test
        @DisplayName("Google 토큰을 revoke하고 연결을 삭제한다")
        void revokesAndDeletes() {
            // given
            User user = givenUser();
            GmailConnection connection = givenConnection(user, REFRESH_TOKEN);

            // when
            gmailUseCase.disconnectGmail(userId);

            // then
            verify(gmailOAuthClient).revokeToken(REFRESH_TOKEN);
            assertThat(connection.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("연결이 없으면 GMAIL_CONNECTION_NOT_FOUND를 던진다")
        void throwsWhenNotConnected() {
            // given
            User user = givenUser();
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gmailUseCase.disconnectGmail(userId))
                .isInstanceOf(GmailConnectionNotFoundException.class)
                .satisfies(e -> assertErrorCode(e, GmailErrorCode.GMAIL_CONNECTION_NOT_FOUND));
            verifyNoInteractions(gmailOAuthClient);
        }

        @Test
        @DisplayName("revoke가 실패하면 GOOGLE_OAUTH_SERVER_ERROR를 던지고 연결을 삭제하지 않는다")
        void keepsConnectionWhenRevokeFails() {
            // given
            User user = givenUser();
            GmailConnection connection = givenConnection(user, REFRESH_TOKEN);
            willThrow(new GoogleOAuthServerException()).given(gmailOAuthClient).revokeToken(REFRESH_TOKEN);

            // when & then
            assertThatThrownBy(() -> gmailUseCase.disconnectGmail(userId))
                .isInstanceOf(GoogleOAuthServerException.class)
                .satisfies(e -> assertErrorCode(e, GmailErrorCode.GOOGLE_OAUTH_SERVER_ERROR));
            assertThat(connection.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenUserNotFound();

            // when & then
            assertThatThrownBy(() -> gmailUseCase.disconnectGmail(userId))
                .isInstanceOf(UserNotFoundException.class)
                .satisfies(e -> assertErrorCode(e, UserErrorCode.USER_NOT_FOUND));
            verifyNoInteractions(gmailOAuthClient);
        }
    }

    @Nested
    @DisplayName("analyzeGmailPayments")
    class AnalyzeGmailPayments {

        private final LocalDate today = LocalDate.now();
        private final GmailMail mail = new GmailMail("receipt@example.com", "결제 완료", today, "본문");
        private final ServiceCatalog netflix = service("Netflix 스탠다드", Category.VIDEO_OTT, "netflix.png");
        private final ServiceCatalog youtube = service("YouTube Premium", Category.VIDEO_OTT, null);

        private ServiceCatalog service(String name, Category category, String logoUrl) {
            ServiceCatalog service = ServiceCatalog.create(name, null, null, category, logoUrl, null, 1);
            ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
            return service;
        }

        private GmailConnection givenConnected() {
            User user = givenUser();
            GmailConnection connection = givenConnection(user, REFRESH_TOKEN);
            given(gmailMailReader.readPaymentMails(REFRESH_TOKEN)).willReturn(List.of(mail));
            given(serviceCatalogGetService.findActiveServices()).willReturn(List.of(netflix, youtube));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of());
            return connection;
        }

        private void givenPayments(GmailExtractedPayment... payments) {
            given(gmailPaymentAnalyzer.analyze(any(), any(), any())).willReturn(List.of(payments));
        }

        private GmailExtractedPayment payment(String serviceName, Integer amount, LocalDate paidDate) {
            return new GmailExtractedPayment(serviceName, amount, 1, BillingCycleUnit.MONTH, paidDate, null,
                PaymentMethod.GOOGLE_PLAY, false);
        }

        @Test
        @DisplayName("추출한 결제를 서비스 목록과 맞춰 후보로 반환하고 로고 · 카테고리는 서비스 목록 값을 쓴다")
        void returnsMatchedCandidates() {
            // given
            givenConnected();
            givenPayments(new GmailExtractedPayment("Netflix 스탠다드", 13_500, 1, BillingCycleUnit.MONTH,
                today.minusDays(5), today.plusDays(25), PaymentMethod.WEB_DIRECT, false));

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            assertThat(response.candidates()).hasSize(1);
            GmailCandidateResponse candidate = response.candidates().getFirst();
            assertThat(candidate.serviceId()).isEqualTo(netflix.getId());
            assertThat(candidate.serviceName()).isEqualTo("Netflix 스탠다드");
            assertThat(candidate.logoUrl()).isEqualTo("netflix.png");
            assertThat(candidate.category()).isEqualTo(Category.VIDEO_OTT);
            assertThat(candidate.amount()).isEqualTo(13_500);
            assertThat(candidate.nextBillingDate()).isEqualTo(today.plusDays(25));
            assertThat(candidate.paymentMethod()).isEqualTo(PaymentMethod.WEB_DIRECT);
            assertThat(candidate.needsReview()).isFalse();
            assertThat(candidate.reviewFields()).isEmpty();
        }

        @Test
        @DisplayName("성공하면 마지막 조회 시각을 갱신한다")
        void marksSynced() {
            // given
            GmailConnection connection = givenConnected();
            givenPayments();

            // when
            gmailUseCase.analyzeGmailPayments(userId);

            // then
            assertThat(connection.getLastSyncedAt()).isNotNull();
        }

        @Test
        @DisplayName("서비스 목록에 없는 서비스 · 이미 구독 중인 서비스는 후보에서 뺀다")
        void excludesUnknownAndSubscribedServices() {
            // given
            User user = givenUser();
            givenConnection(user, REFRESH_TOKEN);
            given(gmailMailReader.readPaymentMails(REFRESH_TOKEN)).willReturn(List.of(mail));
            given(serviceCatalogGetService.findActiveServices()).willReturn(List.of(netflix, youtube));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(Set.of(youtube.getId()));
            givenPayments(
                payment("Netflix 스탠다드", 13_500, today),
                payment("YouTube Premium", 14_900, today),
                payment("없는 서비스", 9_900, today),
                payment(null, 5_000, today));

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            assertThat(response.candidates()).extracting(GmailCandidateResponse::serviceName)
                .containsExactly("Netflix 스탠다드");
        }

        @Test
        @DisplayName("같은 서비스가 여러 건이면 결제일이 가장 최근인 1건만 남기고 결제일 최신순으로 반환한다")
        void keepsLatestPaymentPerService() {
            // given
            givenConnected();
            givenPayments(
                payment("Netflix 스탠다드", 13_500, today.minusDays(40)),
                payment("YouTube Premium", 14_900, today.minusDays(3)),
                payment("Netflix 스탠다드", 17_000, today.minusDays(10)));

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            assertThat(response.candidates()).extracting(GmailCandidateResponse::serviceName)
                .containsExactly("YouTube Premium", "Netflix 스탠다드");
            assertThat(response.candidates().get(1).amount()).isEqualTo(17_000);
        }

        @Test
        @DisplayName("다음 결제일이 없으면 결제일 + 결제 주기로 오늘 이후 첫 날짜를 계산한다")
        void calculatesNextBillingDate() {
            // given
            givenConnected();
            LocalDate paidDate = today.minusDays(10);
            givenPayments(payment("Netflix 스탠다드", 13_500, paidDate));

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            GmailCandidateResponse candidate = response.candidates().getFirst();
            assertThat(candidate.nextBillingDate()).isEqualTo(paidDate.plusMonths(1));
            assertThat(candidate.reviewFields()).isEmpty();
        }

        @Test
        @DisplayName("금액을 못 읽었거나 주기를 모르면 확인 필요로 표시하고 해당 필드를 reviewFields에 넣는다")
        void marksReviewFields() {
            // given
            givenConnected();
            givenPayments(new GmailExtractedPayment("Netflix 스탠다드", null, null, null, null, null, null, false));

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            GmailCandidateResponse candidate = response.candidates().getFirst();
            assertThat(candidate.needsReview()).isTrue();
            assertThat(candidate.amount()).isNull();
            assertThat(candidate.billingCycleValue()).isEqualTo(1);
            assertThat(candidate.billingCycleUnit()).isEqualTo(BillingCycleUnit.MONTH);
            assertThat(candidate.nextBillingDate()).isNull();
            assertThat(candidate.paymentMethod()).isEqualTo(PaymentMethod.WEB_DIRECT);
            assertThat(candidate.reviewFields()).containsExactlyInAnyOrder(
                GmailReviewField.AMOUNT, GmailReviewField.BILLING_CYCLE, GmailReviewField.NEXT_BILLING_DATE);
        }

        @Test
        @DisplayName("검색된 메일이 없으면 LLM을 호출하지 않고 빈 배열을 반환한다")
        void returnsEmptyWithoutMails() {
            // given
            User user = givenUser();
            givenConnection(user, REFRESH_TOKEN);
            given(gmailMailReader.readPaymentMails(REFRESH_TOKEN)).willReturn(List.of());

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            assertThat(response.candidates()).isEmpty();
            verifyNoInteractions(gmailPaymentAnalyzer);
        }

        @Test
        @DisplayName("남은 후보가 없으면 빈 배열을 반환한다")
        void returnsEmptyWithoutCandidates() {
            // given
            givenConnected();
            givenPayments(payment("없는 서비스", 9_900, today));

            // when
            GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);

            // then
            assertThat(response.candidates()).isEmpty();
        }

        @Test
        @DisplayName("Gmail이 연동되지 않았으면 GMAIL_CONNECTION_NOT_FOUND를 던진다")
        void throwsWhenNotConnected() {
            // given
            User user = givenUser();
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gmailUseCase.analyzeGmailPayments(userId))
                .isInstanceOf(GmailConnectionNotFoundException.class)
                .satisfies(e -> assertErrorCode(e, GmailErrorCode.GMAIL_CONNECTION_NOT_FOUND));
            verifyNoInteractions(gmailMailReader, gmailPaymentAnalyzer);
        }

        @Test
        @DisplayName("Gmail 권한이 끊겼으면 GMAIL_RECONNECT_REQUIRED를 던진다")
        void throwsWhenReconnectRequired() {
            // given
            User user = givenUser();
            givenConnection(user, REFRESH_TOKEN);
            given(gmailMailReader.readPaymentMails(REFRESH_TOKEN)).willThrow(new GmailReconnectRequiredException());

            // when & then
            assertThatThrownBy(() -> gmailUseCase.analyzeGmailPayments(userId))
                .isInstanceOf(GmailReconnectRequiredException.class)
                .satisfies(e -> assertErrorCode(e, GmailErrorCode.GMAIL_RECONNECT_REQUIRED));
            verifyNoInteractions(gmailPaymentAnalyzer);
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND를 던진다")
        void throwsWhenUserNotFound() {
            // given
            givenUserNotFound();

            // when & then
            assertThatThrownBy(() -> gmailUseCase.analyzeGmailPayments(userId))
                .isInstanceOf(UserNotFoundException.class)
                .satisfies(e -> assertErrorCode(e, UserErrorCode.USER_NOT_FOUND));
        }
    }
}
