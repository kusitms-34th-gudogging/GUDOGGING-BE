package com.subscribers.gudogging.domain.gmail.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.gmail.application.dto.request.ConnectGmailRequest;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailConnectionResponse;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailConnectionNotFoundException;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailErrorCode;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailPermissionRequiredException;
import com.subscribers.gudogging.domain.gmail.application.exception.GoogleOAuthServerException;
import com.subscribers.gudogging.domain.gmail.application.exception.InvalidGmailAuthCodeException;
import com.subscribers.gudogging.domain.gmail.domain.GmailOAuthClient;
import com.subscribers.gudogging.domain.gmail.domain.GmailTokens;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnectionStatus;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionCreateService;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailUseCase")
class GmailUseCaseTest {

    private static final String AUTH_CODE = "4/0AX4XfWh-auth-code";
    private static final String GOOGLE_SUB = "google-sub-456";
    private static final String GMAIL_ADDRESS = "payments@gmail.com";
    private static final String REFRESH_TOKEN = "gmail-refresh-token";
    private static final String OLD_REFRESH_TOKEN = "old-gmail-refresh-token";

    @Mock
    private UserGetService userGetService;

    @Mock
    private GmailConnectionGetService gmailConnectionGetService;

    @Mock
    private GmailConnectionCreateService gmailConnectionCreateService;

    @Mock
    private GmailOAuthClient gmailOAuthClient;

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
        GmailConnection connection = GmailConnection.create(user, GOOGLE_SUB, GMAIL_ADDRESS, refreshToken);
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
        @DisplayName("이미 연결이 있으면 기존 토큰을 revoke하고 기존 연결을 삭제한 뒤 새 연결을 저장한다")
        void replacesExistingConnection() {
            // given
            User user = givenUser();
            givenTokens();
            GmailConnection old = givenConnection(user, OLD_REFRESH_TOKEN);
            givenSaved();

            // when
            gmailUseCase.connectGmail(userId, request);

            // then
            verify(gmailOAuthClient).revokeToken(OLD_REFRESH_TOKEN);
            assertThat(old.isDeleted()).isTrue();
            verify(gmailConnectionCreateService).saveGmailConnection(any());
        }

        @Test
        @DisplayName("기존 토큰 revoke가 실패해도 기존 연결을 삭제하고 새 연결을 저장한다")
        void replacesEvenWhenRevokeFails() {
            // given
            User user = givenUser();
            givenTokens();
            GmailConnection old = givenConnection(user, OLD_REFRESH_TOKEN);
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
}
