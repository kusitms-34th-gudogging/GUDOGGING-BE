package com.subscribers.gudogging.domain.gmail.application.dto.response;

import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnectionStatus;
import java.util.Optional;

public record GmailConnectionResponse(
    GmailConnectionStatus status,
    boolean connected,
    String email
) {

    public static GmailConnectionResponse from(GmailConnection gmailConnection) {
        return new GmailConnectionResponse(GmailConnectionStatus.CONNECTED, true, gmailConnection.getGmailAddress());
    }

    public static GmailConnectionResponse from(Optional<GmailConnection> gmailConnection) {
        return gmailConnection
            .map(GmailConnectionResponse::from)
            .orElseGet(() -> new GmailConnectionResponse(GmailConnectionStatus.NOT_CONNECTED, false, null));
    }
}
