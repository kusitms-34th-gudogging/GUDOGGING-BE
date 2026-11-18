package com.subscribers.gudogging.domain.subscription.domain.entity;

import java.util.List;

/**
 * 결제 경로별 해지 안내 (X-01 / X-03 / X-04 / X-05). 문구는 코드 상수이며 DB에 저장하지 않는다.
 * steps의 {서비스명}{을/를}은 서비스명 + 목적격 조사로, {서비스명}은 서비스명으로 치환한다.
 */
public enum CancelGuideType {

    APP_STORE(
        List.of(
            "아래 버튼을 눌러 App Store 구독 관리 화면을 열어요",
            "목록에서 {서비스명}{을/를} 선택해요",
            "'구독 취소'를 누르면 끝!"
        ),
        "다른 Apple ID로 결제했거나 가족 공유 중인 구독은 목록에 보이지 않을 수 있어요",
        "https://apps.apple.com/account/subscriptions",
        false
    ),
    GOOGLE_PLAY(
        List.of(
            "아래 버튼을 눌러 Google Play 정기 결제 화면을 열어요",
            "목록에서 {서비스명}{을/를} 선택해요",
            "'정기 결제 취소'를 누르면 끝!"
        ),
        "다른 Google 계정으로 결제한 구독은 목록에 보이지 않을 수 있어요",
        "https://play.google.com/store/account/subscriptions",
        false
    ),
    WEB_WITH_URL(
        List.of(
            "아래 버튼을 눌러 해지 페이지로 이동해요",
            "해지를 완료해요",
            "돌아와서 '해지 완료'를 눌러요"
        ),
        "해지해도 남은 기간까지는 혜택을 쓸 수 있어요",
        null,
        true
    ),
    WEB_WITHOUT_URL(
        List.of(
            "{서비스명} 앱이나 웹에서 해지 메뉴로 이동해요",
            "해지를 완료해요",
            "돌아와서 '해지했어요'를 눌러요"
        ),
        "해지해도 남은 기간까지는 혜택을 쓸 수 있어요",
        null,
        false
    );

    private static final String SERVICE_NAME_WITH_PARTICLE = "{서비스명}{을/를}";
    private static final String SERVICE_NAME = "{서비스명}";

    private final List<String> stepTemplates;
    private final String caution;
    private final String fixedCancelUrl;
    private final boolean usesServiceCancelUrl;

    CancelGuideType(List<String> stepTemplates, String caution, String fixedCancelUrl,
                    boolean usesServiceCancelUrl) {
        this.stepTemplates = stepTemplates;
        this.caution = caution;
        this.fixedCancelUrl = fixedCancelUrl;
        this.usesServiceCancelUrl = usesServiceCancelUrl;
    }

    public List<String> steps(String serviceName, String objectParticle) {
        return stepTemplates.stream()
            .map(template -> template
                .replace(SERVICE_NAME_WITH_PARTICLE, serviceName + objectParticle)
                .replace(SERVICE_NAME, serviceName))
            .toList();
    }

    /**
     * 스토어 경로는 상수 링크, 웹 링크 있음은 서비스의 해지 링크, 웹 링크 없음은 null.
     */
    public String cancelUrl(String serviceCancelUrl) {
        return usesServiceCancelUrl ? serviceCancelUrl : fixedCancelUrl;
    }

    public String caution() {
        return caution;
    }
}
