package com.subscribers.gudogging.domain.gmail.application.usecase;

import com.subscribers.gudogging.domain.gmail.application.dto.request.ConnectGmailRequest;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailConnectionResponse;
import com.subscribers.gudogging.domain.gmail.application.exception.GmailConnectionNotFoundException;
import com.subscribers.gudogging.domain.gmail.domain.GmailOAuthClient;
import com.subscribers.gudogging.domain.gmail.domain.GmailTokens;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionCreateService;
import com.subscribers.gudogging.domain.gmail.domain.service.GmailConnectionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import java.util.UUID;
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
