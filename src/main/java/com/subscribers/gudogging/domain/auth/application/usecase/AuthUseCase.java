package com.subscribers.gudogging.domain.auth.application.usecase;

import com.subscribers.gudogging.domain.auth.application.dto.request.GoogleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.response.LoginResponse;
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

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.saveRefreshToken(
            RefreshToken.of(user.getId(), refreshToken, jwtProvider.getRefreshExpirationSeconds())
        );

        return LoginResponse.of(accessToken, refreshToken, isNewUser);
    }
}
