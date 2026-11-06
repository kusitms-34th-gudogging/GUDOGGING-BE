package com.subscribers.gudogging.domain.auth.application.usecase;

import com.subscribers.gudogging.domain.auth.application.dto.request.GoogleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.ReissueTokenRequest;
import com.subscribers.gudogging.domain.auth.application.dto.response.LoginResponse;
import com.subscribers.gudogging.domain.auth.application.dto.response.TokenResponse;
import com.subscribers.gudogging.domain.auth.application.exception.InvalidRefreshTokenException;
import com.subscribers.gudogging.domain.auth.domain.GoogleTokenVerifier;
import com.subscribers.gudogging.domain.auth.domain.GoogleUserInfo;
import com.subscribers.gudogging.domain.auth.domain.RefreshToken;
import com.subscribers.gudogging.domain.auth.domain.service.RefreshTokenService;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserCreateService;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.security.jwt.JwtProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthUseCase {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserGetService userGetService;
    private final UserCreateService userCreateService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(request.idToken());

        Optional<User> existingUser = userGetService.findUserBySocialAccount(googleUserInfo);
        boolean isNewUser = existingUser.isEmpty();
        User user = existingUser.orElseGet(() -> userCreateService.createUser(
            User.create(SocialProvider.GOOGLE, googleUserInfo.sub(), googleUserInfo.email(), googleUserInfo.name())
        ));

        TokenResponse tokens = issueTokens(user.getId());
        return LoginResponse.of(tokens.accessToken(), tokens.refreshToken(), isNewUser);
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
}
