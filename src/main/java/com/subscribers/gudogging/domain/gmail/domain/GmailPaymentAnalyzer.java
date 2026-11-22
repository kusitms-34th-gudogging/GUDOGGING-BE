package com.subscribers.gudogging.domain.gmail.domain;

import java.time.LocalDate;
import java.util.List;

public interface GmailPaymentAnalyzer {

    /**
     * 메일 묶음에서 구독 결제 메일만 골라 결제 정보를 뽑는다. serviceName은 serviceNames 중 하나 또는 null이다.
     */
    List<GmailExtractedPayment> analyze(List<GmailMail> mails, List<String> serviceNames, LocalDate today);
}
