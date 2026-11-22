package com.subscribers.gudogging.domain.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.auth.application.exception.AppleServerException;
import com.subscribers.gudogging.domain.auth.application.exception.AuthErrorCode;
import com.subscribers.gudogging.domain.auth.domain.AppleAuthClient;
import com.subscribers.gudogging.domain.auth.domain.service.RefreshTokenService;
import com.subscribers.gudogging.domain.gmail.application.exception.GoogleOAuthServerException;
import com.subscribers.gudogging.domain.gmail.domain.GmailOAuthClient;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionGetService;
import com.subscribers.gudogging.domain.user.application.dto.response.UserAccountResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserUseCase")
class UserUseCaseTest {

    private static final String SOCIAL_ID = "social-sub-123";
    private static final String EMAIL = "user@example.com";
    private static final String NAME = "홍길동";
    private static final String APPLE_REFRESH_TOKEN = "apple-refresh-token";
    private static final String WITHDRAWN_SOCIAL_ID_PREFIX = "withdrawn_";

    @Mock
    private UserGetService userGetService;

    @Mock
    private AppleAuthClient appleAuthClient;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private GmailConnectionGetService gmailConnectionGetService;

    @Mock
    private GmailOAuthClient gmailOAuthClient;

    @InjectMocks
    private UserUseCase userUseCase;

    @Nested
    @DisplayName("getUserAccount")
    class GetUserAccount {

        private static final String GMAIL_ADDRESS = "payments@gmail.com";

        private final UUID userId = UUID.randomUUID();

        private User givenUser(SocialProvider socialProvider, String email) {
            User user = User.create(socialProvider, SOCIAL_ID, email, NAME);
            ReflectionTestUtils.setField(user, "id", userId);
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            return user;
        }

        private void givenGmailConnection(User user, String gmailAddress) {
            GmailConnection gmailConnection = GmailConnection.create(user, "google-sub-456", gmailAddress,
                "gmail-refresh-token");
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.of(gmailConnection));
        }

        @Test
        @DisplayName("Gmail을 연동한 Google 회원이면 회원 ID · 로그인 방식 · 로그인 이메일 · Gmail 연동 여부 · 연동 Gmail 주소를 반환한다")
        void returnsAccountWithGmail() {
            // given
            User user = givenUser(SocialProvider.GOOGLE, EMAIL);
            givenGmailConnection(user, GMAIL_ADDRESS);

            // when
            UserAccountResponse result = userUseCase.getUserAccount(userId);

            // then
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.gmailLinked()).isTrue();
            assertThat(result.gmailEmail()).isEqualTo(GMAIL_ADDRESS);
        }

        @Test
        @DisplayName("Gmail을 연동하지 않았으면 Gmail 연동 여부 false · 연동 Gmail 주소 null을 반환한다")
        void returnsAccountWithoutGmail() {
            // given
            User user = givenUser(SocialProvider.GOOGLE, EMAIL);
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.empty());

            // when
            UserAccountResponse result = userUseCase.getUserAccount(userId);

            // then
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.gmailLinked()).isFalse();
            assertThat(result.gmailEmail()).isNull();
        }

        @Test
        @DisplayName("로그인 이메일이 없는 Apple 회원이면 로그인 방식 APPLE · 이메일 null을 반환한다")
        void returnsAppleAccountWithoutEmail() {
            // given
            User user = givenUser(SocialProvider.APPLE, null);
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.empty());

            // when
            UserAccountResponse result = userUseCase.getUserAccount(userId);

            // then
            assertThat(result.provider()).isEqualTo(SocialProvider.APPLE);
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("로그인 이메일과 다른 Gmail 계정을 연동했으면 두 값을 각각 반환한다")
        void returnsLoginEmailAndGmailSeparately() {
            // given
            User user = givenUser(SocialProvider.APPLE, "user@privaterelay.appleid.com");
            givenGmailConnection(user, GMAIL_ADDRESS);

            // when
            UserAccountResponse result = userUseCase.getUserAccount(userId);

            // then
            assertThat(result.email()).isEqualTo("user@privaterelay.appleid.com");
            assertThat(result.gmailLinked()).isTrue();
            assertThat(result.gmailEmail()).isEqualTo(GMAIL_ADDRESS);
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 Gmail 연결을 조회하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userUseCase.getUserAccount(userId))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(gmailConnectionGetService);
        }
    }

    @Nested
    @DisplayName("withdrawUser")
    class WithdrawUser {

        private final UUID userId = UUID.randomUUID();

        private User givenUser(SocialProvider socialProvider, String appleRefreshToken) {
            User user = User.create(socialProvider, SOCIAL_ID, EMAIL, NAME);
            user.updateAppleRefreshToken(appleRefreshToken);
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            return user;
        }

        private void assertWithdrawn(User user) {
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getSocialId()).startsWith(WITHDRAWN_SOCIAL_ID_PREFIX);
            assertThat(user.getEmail()).isNull();
            assertThat(user.getName()).isNull();
            assertThat(user.getAppleRefreshToken()).isNull();
        }

        @Test
        @DisplayName("Google 회원이면 revoke를 호출하지 않고 탈퇴 처리한 뒤 Refresh Token을 삭제한다")
        void withdrawsGoogleUserWithoutRevoke() {
            // given
            User user = givenUser(SocialProvider.GOOGLE, null);

            // when
            userUseCase.withdrawUser(userId);

            // then
            assertWithdrawn(user);
            verify(appleAuthClient, never()).revokeRefreshToken(any());
            verify(refreshTokenService).deleteRefreshToken(userId);
        }

        @Test
        @DisplayName("Apple 회원이면 저장된 appleRefreshToken으로 revoke한 뒤 탈퇴 처리하고 Refresh Token을 삭제한다")
        void revokesAndWithdrawsAppleUser() {
            // given
            User user = givenUser(SocialProvider.APPLE, APPLE_REFRESH_TOKEN);

            // when
            userUseCase.withdrawUser(userId);

            // then
            verify(appleAuthClient).revokeRefreshToken(APPLE_REFRESH_TOKEN);
            assertWithdrawn(user);
            verify(refreshTokenService).deleteRefreshToken(userId);
        }

        @Test
        @DisplayName("회원이 없거나 이미 탈퇴했으면 USER_NOT_FOUND 예외를 던지고 revoke·Refresh Token 삭제를 하지 않는다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userUseCase.withdrawUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(appleAuthClient, never()).revokeRefreshToken(any());
            verify(refreshTokenService, never()).deleteRefreshToken(any());
        }

        @Test
        @DisplayName("Apple revoke가 APPLE_SERVER_ERROR로 실패하면 회원은 탈퇴되지 않고 Refresh Token도 삭제하지 않는다")
        void doesNotWithdrawWhenRevokeFails() {
            // given
            User user = givenUser(SocialProvider.APPLE, APPLE_REFRESH_TOKEN);
            willThrow(new AppleServerException()).given(appleAuthClient).revokeRefreshToken(APPLE_REFRESH_TOKEN);

            // when & then
            assertThatThrownBy(() -> userUseCase.withdrawUser(userId))
                .isInstanceOf(AppleServerException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.APPLE_SERVER_ERROR);
            assertThat(user.isDeleted()).isFalse();
            assertThat(user.getSocialId()).isEqualTo(SOCIAL_ID);
            assertThat(user.getAppleRefreshToken()).isEqualTo(APPLE_REFRESH_TOKEN);
            verify(refreshTokenService, never()).deleteRefreshToken(any());
        }

        @Test
        @DisplayName("Apple 회원인데 appleRefreshToken이 null이면 revoke 없이 탈퇴 처리한다")
        void withdrawsAppleUserWithoutTokenWithoutRevoke() {
            // given
            User user = givenUser(SocialProvider.APPLE, null);

            // when
            userUseCase.withdrawUser(userId);

            // then
            verify(appleAuthClient, never()).revokeRefreshToken(any());
            assertWithdrawn(user);
            verify(refreshTokenService).deleteRefreshToken(userId);
        }

        @Test
        @DisplayName("Gmail 연결이 있으면 Gmail 토큰을 revoke하고 연결을 삭제한 뒤 탈퇴 처리한다")
        void revokesAndDeletesGmailConnection() {
            // given
            User user = givenUser(SocialProvider.GOOGLE, null);
            GmailConnection gmailConnection = GmailConnection.create(user, "google-sub-456", "payments@gmail.com",
                "gmail-refresh-token");
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.of(gmailConnection));

            // when
            userUseCase.withdrawUser(userId);

            // then
            verify(gmailOAuthClient).revokeToken("gmail-refresh-token");
            assertThat(gmailConnection.isDeleted()).isTrue();
            assertWithdrawn(user);
        }

        @Test
        @DisplayName("Gmail 토큰 revoke가 실패해도 연결을 삭제하고 탈퇴를 진행한다")
        void deletesGmailConnectionWhenRevokeFails() {
            // given
            User user = givenUser(SocialProvider.GOOGLE, null);
            GmailConnection gmailConnection = GmailConnection.create(user, "google-sub-456", "payments@gmail.com",
                "gmail-refresh-token");
            given(gmailConnectionGetService.findGmailConnection(user)).willReturn(Optional.of(gmailConnection));
            willThrow(new GoogleOAuthServerException()).given(gmailOAuthClient).revokeToken("gmail-refresh-token");

            // when
            userUseCase.withdrawUser(userId);

            // then
            assertThat(gmailConnection.isDeleted()).isTrue();
            assertWithdrawn(user);
            verify(refreshTokenService).deleteRefreshToken(userId);
        }
    }
}
