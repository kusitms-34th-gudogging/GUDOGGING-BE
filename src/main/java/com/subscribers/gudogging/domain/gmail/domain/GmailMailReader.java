package com.subscribers.gudogging.domain.gmail.domain;

import java.util.List;

public interface GmailMailReader {

    /**
     * refresh token으로 Gmail에 접근해 최근 결제 · 구독 관련 메일을 가져온다 (최근 60일, 최대 50통, 통당 본문 2,000자).
     */
    List<GmailMail> readPaymentMails(String refreshToken);
}
