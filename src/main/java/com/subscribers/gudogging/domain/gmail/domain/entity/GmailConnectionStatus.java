package com.subscribers.gudogging.domain.gmail.domain.entity;

/**
 * Gmail 연동 상태. 연결이 있으면 CONNECTED, 없으면 NOT_CONNECTED (GMAIL-002).
 */
public enum GmailConnectionStatus {
    NOT_CONNECTED,
    CONNECTED
}
