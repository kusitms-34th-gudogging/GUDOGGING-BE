package com.subscribers.gudogging.domain.user.application.dto.response;

import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.util.Optional;
import java.util.UUID;

public record UserAccountResponse(
    UUID userId,
    SocialProvider provider,
    String email,
    boolean gmailLinked,
    String gmailEmail
) {

    public static UserAccountResponse of(User user, Optional<GmailConnection> gmailConnection) {
        return new UserAccountResponse(
            user.getId(),
            user.getSocialProvider(),
            user.getEmail(),
            gmailConnection.isPresent(),
            gmailConnection.map(GmailConnection::getGmailAddress).orElse(null)
        );
    }
}
