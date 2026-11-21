package com.subscribers.gudogging.domain.notification.domain;

/**
 * 푸시 문구 (화면설계서 NOT-001 N-01 · N-02). 금액은 천 단위 쉼표.
 */
public record NotificationContent(String title, String body) {

    private static final String CHECK_REMINDER_BODY = "7일 안에 빠져나갈 구독료를 확인하세요.";
    private static final String BILLING_REMINDER_BODY = "해지 안 했다면 오늘이 마지막 해지 기회예요.";

    /**
     * D-3 점검 알림 — 1건: '{서비스명} {금액}원 결제 예정' / 여러 건: '{서비스명} 외 N건, {7일 합계}원 결제 예정'.
     */
    public static NotificationContent checkReminder(String serviceName, int otherCount, long totalAmount) {
        String title = otherCount == 0
            ? "%s %s원 결제 예정".formatted(serviceName, won(totalAmount))
            : "%s 외 %d건, %s원 결제 예정".formatted(serviceName, otherCount, won(totalAmount));
        return new NotificationContent(title, CHECK_REMINDER_BODY);
    }

    public static NotificationContent billingReminder(String serviceName, int amount) {
        return new NotificationContent("내일 %s %s원이 결제돼요".formatted(serviceName, won(amount)),
            BILLING_REMINDER_BODY);
    }

    public static NotificationContent cancelConfirm(String serviceName, int amount) {
        return new NotificationContent("%s, 해지하셨나요?".formatted(serviceName),
            "아직이라면 내일 %s원이 결제돼요. 오늘 해지하세요.".formatted(won(amount)));
    }

    private static String won(long amount) {
        return "%,d".formatted(amount);
    }
}
