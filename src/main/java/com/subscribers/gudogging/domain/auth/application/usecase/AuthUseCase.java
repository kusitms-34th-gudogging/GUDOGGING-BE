package com.subscribers.gudogging.domain.auth.application.usecase;

import com.subscribers.gudogging.domain.auth.application.dto.request.AppleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.GoogleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.ReissueTokenRequest;
import com.subscribers.gudogging.domain.auth.application.dto.response.LoginResponse;
import com.subscribers.gudogging.domain.auth.application.dto.response.TokenResponse;
import com.subscribers.gudogging.domain.auth.application.exception.InvalidAppleAuthorizationCodeException;
import com.subscribers.gudogging.domain.auth.application.exception.InvalidRefreshTokenException;
import com.subscribers.gudogging.domain.auth.domain.AppleAuthClient;
import com.subscribers.gudogging.domain.auth.domain.AppleTokenVerifier;
import com.subscribers.gudogging.domain.auth.domain.AppleTokens;
import com.subscribers.gudogging.domain.auth.domain.AppleUserInfo;
import com.subscribers.gudogging.domain.auth.domain.GoogleTokenVerifier;
import com.subscribers.gudogging.domain.auth.domain.GoogleUserInfo;
import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import com.subscribers.gudogging.domain.auth.domain.service.RefreshTokenService;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserCreateService;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.security.jwt.JwtProvider;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthUseCase {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;
    private final AppleAuthClient appleAuthClient;
    private final UserGetService userGetService;
    private final UserCreateService userCreateService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(request.idToken());

        SocialLoginUser loginUser = findOrCreateUser(SocialProvider.GOOGLE, googleUserInfo.sub(), () ->
            User.create(SocialProvider.GOOGLE, googleUserInfo.sub(), googleUserInfo.email(), googleUserInfo.name())
        );

        return issueLoginTokens(loginUser);
    }

    @Transactional
    public LoginResponse loginWithApple(AppleLoginRequest request) {
        AppleUserInfo appleUserInfo = appleTokenVerifier.verify(request.identityToken());
        AppleTokens appleTokens = appleAuthClient.issueTokens(request.authorizationCode());
        if (!appleUserInfo.sub().equals(appleTokens.sub())) {
            throw new InvalidAppleAuthorizationCodeException();
        }

        SocialLoginUser loginUser = findOrCreateUser(SocialProvider.APPLE, appleUserInfo.sub(), () ->
            User.create(SocialProvider.APPLE, appleUserInfo.sub(), appleUserInfo.email(), request.nameOrNull())
        );
        loginUser.user().updateAppleRefreshToken(appleTokens.refreshToken());

        return issueLoginTokens(loginUser);
    }

    @Transactional
    public TokenResponse reissueToken(ReissueTokenRequest request) {
        UUID userId = jwtProvider.parseRefreshToken(request.refreshToken());
        refreshTokenService.validateRefreshToken(userId, request.refreshToken());

        userGetService.findUserById(userId)
            .orElseThrow(InvalidRefreshTokenException::new);

        return issueTokens(userId);
    }

    /**
     * 소셜 계정으로 가입된 회원을 찾고, 없으면 newUser로 가입시킨다.
     */
    private SocialLoginUser findOrCreateUser(SocialProvider socialProvider, String socialId, Supplier<User> newUser) {
        return userGetService.findUserBySocialAccount(socialProvider, socialId)
            .map(user -> new SocialLoginUser(user, false))
            .orElseGet(() -> new SocialLoginUser(userCreateService.createUser(newUser.get()), true));
    }

    private LoginResponse issueLoginTokens(SocialLoginUser loginUser) {
        TokenResponse tokens = issueTokens(loginUser.user().getId());
        return LoginResponse.of(tokens.accessToken(), tokens.refreshToken(), loginUser.isNewUser());
    }

    /**
     * Access/Refresh Token을 발급하고 Refresh Token을 저장한다. 회원당 1개만 저장되므로 이전 Refresh Token은 무효가 된다.
     */
    private TokenResponse issueTokens(UUID userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenService.saveRefreshToken(
            RefreshToken.of(userId, refreshToken, jwtProvider.getRefreshExpirationSeconds())
        );
        return TokenResponse.of(accessToken, refreshToken);
    }

    private record SocialLoginUser(User user, boolean isNewUser) {
    }
}
