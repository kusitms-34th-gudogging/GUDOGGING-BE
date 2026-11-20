package com.subscribers.gudogging.domain.user.application.usecase;

import com.subscribers.gudogging.domain.auth.domain.AppleAuthClient;
import com.subscribers.gudogging.domain.auth.domain.service.RefreshTokenService;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionGetService;
import com.subscribers.gudogging.domain.user.application.dto.response.UserAccountResponse;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserUseCase {

    private final UserGetService userGetService;
    private final AppleAuthClient appleAuthClient;
    private final RefreshTokenService refreshTokenService;
    private final GmailConnectionGetService gmailConnectionGetService;

    /**
     * 마이페이지(E-01)에 표시할 로그인 계정(이메일 · 로그인 방식)과, 로그인 계정과 별개인 Gmail 연동 여부 · 연동 계정을 조회한다.
     */
    public UserAccountResponse getUserAccount(UUID userId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);
        Optional<GmailConnection> gmailConnection = gmailConnectionGetService.findGmailConnection(user);
        return UserAccountResponse.of(user, gmailConnection);
    }

    /**
     * Apple 회원이면 Apple 토큰을 먼저 revoke한 뒤 회원을 Soft Delete·익명화하고 Refresh Token을 삭제한다. revoke가 실패하면 DB·Redis를 바꾸지 않고 예외를
     * 던지므로 그대로 재시도할 수 있다.
     */
    @Transactional
    public void withdrawUser(UUID userId) {
        User user = userGetService.findUserById(userId)
            .orElseThrow(UserNotFoundException::new);

        revokeAppleToken(user);
        user.withdraw();
        refreshTokenService.deleteRefreshToken(userId);
    }

    private void revokeAppleToken(User user) {
        if (user.getSocialProvider() != SocialProvider.APPLE) {
            return;
        }
        if (user.getAppleRefreshToken() == null) {
            log.warn("Apple 회원의 appleRefreshToken이 없어 revoke 없이 탈퇴합니다: userId={}", user.getId());
            return;
        }
        appleAuthClient.revokeRefreshToken(user.getAppleRefreshToken());
    }
}
