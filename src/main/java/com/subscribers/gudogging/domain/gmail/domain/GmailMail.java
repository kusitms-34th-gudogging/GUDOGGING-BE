package com.subscribers.gudogging.domain.gmail.domain;

import java.time.LocalDate;

/**
 * 분석에 보낼 결제 관련 메일 1통. body는 태그를 뺀 본문 텍스트 앞부분이다.
 */
public record GmailMail(
    String from,
    String subject,
    LocalDate receivedOn,
    String body
) {
}
