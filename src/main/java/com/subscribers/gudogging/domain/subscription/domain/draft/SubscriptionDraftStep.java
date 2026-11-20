package com.subscribers.gudogging.domain.subscription.domain.draft;

/**
 * 구독 등록 임시저장을 불러올 때 복원할 화면 (Figma A-01s '이전에 등록을 중단한 화면으로 이동').
 * SELECT_SERVICES = A-02 서비스 선택, CONFIRM_SUBSCRIPTIONS = A-04 선택한 구독 확인,
 * INPUT_SUBSCRIPTION = A-03 / A-03s 구독 정보 입력 (입력 중이던 항목은 SubscriptionDraft.editingItemIndex).
 */
public enum SubscriptionDraftStep {
    SELECT_SERVICES,
    CONFIRM_SUBSCRIPTIONS,
    INPUT_SUBSCRIPTION
}
