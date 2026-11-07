package com.subscribers.gudogging.domain.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.subscribers.gudogging.domain.auth.application.dto.request.GoogleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.ReissueTokenRequest;
import com.subscribers.gudogging.domain.auth.application.dto.response.LoginResponse;
import com.subscribers.gudogging.domain.auth.application.dto.response.TokenResponse;
import com.subscribers.gudogging.domain.auth.application.exception.AuthErrorCode;
import com.subscribers.gudogging.domain.auth.application.exception.InvalidGoogleIdTokenException;
import com.subscribers.gudogging.domain.auth.application.exception.InvalidRefreshTokenException;
import com.subscribers.gudogging.domain.auth.domain.GoogleTokenVerifier;
import com.subscribers.gudogging.domain.auth.domain.GoogleUserInfo;
import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import com.subscribers.gudogging.domain.auth.domain.service.RefreshTokenService;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserCreateService;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import com.subscribers.gudogging.global.security.jwt.JwtProvider;
import java.time.Duration;
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
@DisplayName("AuthUseCase")
class AuthUseCaseTest {

    private static final String ID_TOKEN = "google-id-token";
    private static final String SUB = "google-sub-123";
    private static final String EMAIL = "user@gmail.com";
    private static final String NAME = "홍길동";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_EXPIRATION_SECONDS = 1_209_600L;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private UserGetService userGetService;

    @Mock
    private UserCreateService userCreateService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthUseCase authUseCase;

    @Nested
    @DisplayName("loginWithGoogle")
    class LoginWithGoogle {

        private final GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);

        private void givenTokensIssued() {
            given(jwtProvider.createAccessToken(any())).willReturn(ACCESS_TOKEN);
            given(jwtProvider.createRefreshToken(any())).willReturn(REFRESH_TOKEN);
            given(jwtProvider.getRefreshExpirationSeconds()).willReturn(REFRESH_EXPIRATION_SECONDS);
        }

        @Test
        @DisplayName("기존 회원이면 isNewUser = false를 반환하고 회원을 생성하지 않는다")
        void returnsExistingUserWithoutCreating() {
            // given
            GoogleUserInfo googleUserInfo = new GoogleUserInfo(SUB, EMAIL, NAME);
            User existingUser = User.create(SocialProvider.GOOGLE, SUB, EMAIL, NAME);
            given(googleTokenVerifier.verify(ID_TOKEN)).willReturn(googleUserInfo);
            given(userGetService.findUserBySocialAccount(googleUserInfo)).willReturn(Optional.of(existingUser));
            givenTokensIssued();

            // when
            LoginResponse response = authUseCase.loginWithGoogle(request);

            // then
            assertThat(response.isNewUser()).isFalse();
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            verify(userCreateService, never()).createUser(any());
        }

        @Test
        @DisplayName("신규 회원이면 GOOGLE·sub·email·name으로 회원을 생성하고 isNewUser = true를 반환한다")
        void createsNewUser() {
            // given
            GoogleUserInfo googleUserInfo = new GoogleUserInfo(SUB, EMAIL, NAME);
            given(googleTokenVerifier.verify(ID_TOKEN)).willReturn(googleUserInfo);
            given(userGetService.findUserBySocialAccount(googleUserInfo)).willReturn(Optional.empty());
            given(userCreateService.createUser(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            givenTokensIssued();

            // when
            LoginResponse response = authUseCase.loginWithGoogle(request);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userCreateService).createUser(userCaptor.capture());
            User created = userCaptor.getValue();
            assertThat(created.getSocialProvider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(created.getSocialId()).isEqualTo(SUB);
            assertThat(created.getEmail()).isEqualTo(EMAIL);
            assertThat(created.getName()).isEqualTo(NAME);
            assertThat(response.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("발급한 Refresh Token을 만료시간과 함께 저장한다")
        void savesRefreshToken() {
            // given
            GoogleUserInfo googleUserInfo = new GoogleUserInfo(SUB, EMAIL, NAME);
            User existingUser = User.create(SocialProvider.GOOGLE, SUB, EMAIL, NAME);
            given(googleTokenVerifier.verify(ID_TOKEN)).willReturn(googleUserInfo);
            given(userGetService.findUserBySocialAccount(googleUserInfo)).willReturn(Optional.of(existingUser));
            givenTokensIssued();

            // when
            authUseCase.loginWithGoogle(request);

            // then
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenService).saveRefreshToken(tokenCaptor.capture());
            RefreshToken saved = tokenCaptor.getValue();
            assertThat(saved.token()).isEqualTo(REFRESH_TOKEN);
            assertThat(saved.timeToLive()).isEqualTo(Duration.ofSeconds(REFRESH_EXPIRATION_SECONDS));
        }

        @Test
        @DisplayName("ID Token 검증에 실패하면 INVALID_GOOGLE_ID_TOKEN 예외를 던지고 회원을 생성하지 않는다")
        void throwsWhenIdTokenInvalid() {
            // given
            given(googleTokenVerifier.verify(ID_TOKEN)).willThrow(new InvalidGoogleIdTokenException());

            // when & then
            assertThatThrownBy(() -> authUseCase.loginWithGoogle(request))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN);
            verify(userCreateService, never()).createUser(any());
            verify(refreshTokenService, never()).saveRefreshToken(any());
        }

        @Test
        @DisplayName("ID Token에 email이 없어도 email = null로 가입된다")
        void createsUserWithoutEmail() {
            // given
            GoogleUserInfo googleUserInfo = new GoogleUserInfo(SUB, null, NAME);
            given(googleTokenVerifier.verify(ID_TOKEN)).willReturn(googleUserInfo);
            given(userGetService.findUserBySocialAccount(googleUserInfo)).willReturn(Optional.empty());
            given(userCreateService.createUser(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            givenTokensIssued();

            // when
            LoginResponse response = authUseCase.loginWithGoogle(request);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userCreateService).createUser(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isNull();
            assertThat(response.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("ID Token에 name이 없어도 name = null로 가입된다")
        void createsUserWithoutName() {
            // given
            GoogleUserInfo googleUserInfo = new GoogleUserInfo(SUB, EMAIL, null);
            given(googleTokenVerifier.verify(ID_TOKEN)).willReturn(googleUserInfo);
            given(userGetService.findUserBySocialAccount(googleUserInfo)).willReturn(Optional.empty());
            given(userCreateService.createUser(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            givenTokensIssued();

            // when
            LoginResponse response = authUseCase.loginWithGoogle(request);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userCreateService).createUser(userCaptor.capture());
            assertThat(userCaptor.getValue().getName()).isNull();
            assertThat(response.isNewUser()).isTrue();
        }
    }

    @Nested
    @DisplayName("reissueToken")
    class ReissueToken {

        private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
        private static final String NEW_ACCESS_TOKEN = "new-access-token";
        private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

        private final UUID userId = UUID.randomUUID();
        private final ReissueTokenRequest request = new ReissueTokenRequest(OLD_REFRESH_TOKEN);

        private void assertInvalidRefreshToken() {
            assertThatThrownBy(() -> authUseCase.reissueToken(request))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
            verify(jwtProvider, never()).createAccessToken(any());
            verify(refreshTokenService, never()).saveRefreshToken(any());
        }

        @Test
        @DisplayName("유효한 Refresh Token이면 새 Access/Refresh Token을 반환하고 저장값을 교체한다")
        void reissuesTokens() {
            // given
            User user = User.create(SocialProvider.GOOGLE, SUB, EMAIL, NAME);
            given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN)).willReturn(userId);
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(jwtProvider.createAccessToken(userId)).willReturn(NEW_ACCESS_TOKEN);
            given(jwtProvider.createRefreshToken(userId)).willReturn(NEW_REFRESH_TOKEN);
            given(jwtProvider.getRefreshExpirationSeconds()).willReturn(REFRESH_EXPIRATION_SECONDS);

            // when
            TokenResponse response = authUseCase.reissueToken(request);

            // then
            assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
            verify(refreshTokenService).validateRefreshToken(userId, OLD_REFRESH_TOKEN);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenService).saveRefreshToken(tokenCaptor.capture());
            RefreshToken saved = tokenCaptor.getValue();
            assertThat(saved.userId()).isEqualTo(userId);
            assertThat(saved.token()).isEqualTo(NEW_REFRESH_TOKEN);
            assertThat(saved.timeToLive()).isEqualTo(Duration.ofSeconds(REFRESH_EXPIRATION_SECONDS));
        }

        @Test
        @DisplayName("파싱 실패·만료·type이 ACCESS인 토큰이면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenTokenUnparsable() {
            // given
            given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN)).willThrow(new InvalidRefreshTokenException());

            // when & then
            assertInvalidRefreshToken();
            verify(refreshTokenService, never()).validateRefreshToken(any(), any());
        }

        @Test
        @DisplayName("Redis 저장값이 없거나 저장값과 다른(이미 교체된) 토큰이면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenTokenNotMatchSavedToken() {
            // given
            given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN)).willReturn(userId);
            willThrow(new InvalidRefreshTokenException())
                .given(refreshTokenService).validateRefreshToken(userId, OLD_REFRESH_TOKEN);

            // when & then
            assertInvalidRefreshToken();
            verify(userGetService, never()).findUserById(any());
        }

        @Test
        @DisplayName("회원이 탈퇴(deleted)했으면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenUserWithdrawn() {
            // given
            given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN)).willReturn(userId);
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertInvalidRefreshToken();
        }
    }
}
