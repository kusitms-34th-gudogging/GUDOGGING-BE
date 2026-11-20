package com.subscribers.gudogging.domain.check.domain.result;

/**
 * 점검 결과 단계 (ERD 6.4). 활용도 구간 80 이상 / 60~79 / 40~59 / 40 미만 순으로 대응하며, 유형명(본전 뽑는 중 · 아끼는 최애 · 환승 대기 중 · 텅장 범인)은 클라이언트가 표시한다.
 */
public enum CheckResultStage {
    KEEP_RECOMMENDED,
    KEEP_CONSIDER,
    CANCEL_CONSIDER,
    CANCEL_RECOMMENDED;

    private static final int KEEP_RECOMMENDED_MIN = 80;
    private static final int KEEP_CONSIDER_MIN = 60;
    private static final int CANCEL_CONSIDER_MIN = 40;

    public static CheckResultStage of(int scorePercent) {
        if (scorePercent >= KEEP_RECOMMENDED_MIN) {
            return KEEP_RECOMMENDED;
        }
        if (scorePercent >= KEEP_CONSIDER_MIN) {
            return KEEP_CONSIDER;
        }
        if (scorePercent >= CANCEL_CONSIDER_MIN) {
            return CANCEL_CONSIDER;
        }
        return CANCEL_RECOMMENDED;
    }
}
