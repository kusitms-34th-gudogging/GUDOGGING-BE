package com.subscribers.gudogging.domain.subscription.domain.draft;

/**
 * 구독 등록 임시저장을 불러올 때 복원할 화면. SELECT_SERVICES = A-02 서비스 선택, CONFIRM_SUBSCRIPTIONS = A-04 선택한 구독 확인.
 */
public enum SubscriptionDraftStep {
    SELECT_SERVICES,
    CONFIRM_SUBSCRIPTIONS
}
