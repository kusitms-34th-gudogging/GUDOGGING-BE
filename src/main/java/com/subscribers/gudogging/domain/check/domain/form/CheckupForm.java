package com.subscribers.gudogging.domain.check.domain.form;

import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import java.time.LocalDate;
import java.util.List;

/**
 * 카테고리별 점검 문항 문구 상수 (ERD 5.2 문항 · 보조 문구 · 선택지, 화면설계서 K-11~K-70 메모 예시 문구 · 칩).
 * 문구의 {서비스명} · {을/를} · {으로/로}와 금액 · 날짜는 questions()에서 채운다.
 */
public enum CheckupForm {

    VIDEO_OTT(
        "에피소드, 영화 모두 1편으로 세어 주세요",
        List.of("안 봤어요", "1~2편", "3~5편", "6~10편", "11편 이상"),
        "{서비스명}에서 본 콘텐츠, 구독료만큼 가치 있었나요?",
        "예: 시즌 3 공개돼서 이번 달은 유지할래요",
        List.of("보고 싶은 작품이 있어요", "이번 달은 바빴어요", "비싸지면 해지할래요")
    ),
    MUSIC(
        Usage.DAILY_HINT,
        Usage.DAILY_OPTIONS,
        "{서비스명}{으로/로} 들은 음악, 구독료만큼 가치 있었나요?",
        "예: 출퇴근길에 매일 들어서 계속 쓸래요",
        List.of("매일 듣고 있어요", "다른 음악 앱이랑 겹쳐요", "가족 요금제로 바꿀래요")
    ),
    SHOPPING_DELIVERY(
        "주문, 배달, 혜택 사용을 모두 1번으로 세어 주세요",
        List.of("안 썼어요", "1~2번", "3~5번", "6~9번", "10번 이상"),
        "{서비스명} 혜택으로 아낀 돈, 구독료만큼 됐나요?",
        "예: 이번 달 배달을 많이 시켜서 본전은 뽑았어요",
        List.of("배송비로 본전 뽑았어요", "이번 달은 주문이 적었어요", "할인 끝난 뒤 해지할래요")
    ),
    EDUCATION(
        Usage.DAILY_HINT,
        Usage.DAILY_OPTIONS,
        "{서비스명}{으로/로} 한 공부, 구독료만큼 가치 있었나요?",
        "예: 자격증 시험 끝날 때까지만 쓸래요",
        List.of("목표 달성까지 계속할래요", "이번 달은 공부를 못 했어요", "시험 끝나면 해지할래요")
    ),
    PRODUCTIVITY_AI(
        Usage.DAILY_HINT,
        Usage.DAILY_OPTIONS,
        "{서비스명} 유료 기능, 구독료만큼 가치 있었나요?",
        "예: 프로젝트 끝나는 다음 달까지만 쓸래요",
        List.of("업무에 꼭 필요해요", "무료 버전으로도 충분해요", "다른 AI 툴과 겹쳐요")
    ),
    ENTERTAINMENT(
        Usage.DAILY_HINT,
        Usage.DAILY_OPTIONS,
        "{서비스명}에서 즐긴 콘텐츠, 구독료만큼 가치 있었나요?",
        "예: 읽고 있는 책 다 읽으면 해지할래요",
        List.of("즐겨 보는 콘텐츠가 있어요", "이번 달은 거의 못 썼어요", "이벤트 끝나면 해지할래요")
    ),
    ETC(
        Usage.DAILY_HINT + "\n자동으로 쓴 날도 포함해요",
        Usage.DAILY_OPTIONS,
        "{서비스명}, 구독료만큼 가치 있었나요?",
        "예: 사진 백업용이라 계속 써야 해요",
        List.of("자동으로 쓰고 있어요", "이번 달은 안 썼어요", "비싸지면 해지할래요")
    );

    private static final String USAGE_LABEL = "최근 한 달 동안 {서비스명}{을/를} 얼마나 자주 이용했나요?";
    private static final String SATISFACTION_HINT = "월 %s원 · 1년이면 %s원";
    private static final List<String> SATISFACTION_OPTIONS =
        List.of("많이 아까워요", "조금 아까워요", "보통이에요", "구독료만큼 가치 있어요", "구독료보다 더 가치 있어요");
    private static final String PLAN_LABEL = "다음 달에도 {서비스명}{을/를} 이용할 계획이 있나요?";
    private static final String PLAN_HINT = "다음 결제 %d월 %d일 · %s원";
    private static final List<String> PLAN_OPTIONS =
        List.of("안 쓸 거예요", "거의 안 쓸 것 같아요", "아직 잘 모르겠어요", "아마 계속 쓸 것 같아요", "꼭 계속 쓸 거예요");

    private final String usageHint;
    private final List<String> usageOptions;
    private final String satisfactionLabel;
    private final String memoPlaceholder;
    private final List<String> memoChips;

    CheckupForm(String usageHint, List<String> usageOptions, String satisfactionLabel, String memoPlaceholder,
                List<String> memoChips) {
        this.usageHint = usageHint;
        this.usageOptions = usageOptions;
        this.satisfactionLabel = satisfactionLabel;
        this.memoPlaceholder = memoPlaceholder;
        this.memoChips = memoChips;
    }

    public static CheckupForm of(Category category) {
        return valueOf(category.name());
    }

    /**
     * 이용도 → 만족도 → 다음 달 계획 문항. 만족도 보조 문구는 월 환산 · 연 환산 금액, 계획 보조 문구는 다음 결제일과 실제 1회 결제 금액을 쓴다.
     *
     * @param objectParticle 서비스명 뒤 목적격 조사 (을/를) — 영문 등은 으로/로 판단에도 쓴다
     */
    public List<CheckupQuestion> questions(String serviceName, String objectParticle, long monthlyAmount,
                                           long yearlyAmount, LocalDate nextBillingDate, int billingAmount) {
        return List.of(
            new CheckupQuestion(CheckupQuestionType.USAGE,
                fill(USAGE_LABEL, serviceName, objectParticle), usageHint, usageOptions),
            new CheckupQuestion(CheckupQuestionType.SATISFACTION,
                fill(satisfactionLabel, serviceName, objectParticle),
                SATISFACTION_HINT.formatted(money(monthlyAmount), money(yearlyAmount)), SATISFACTION_OPTIONS),
            new CheckupQuestion(CheckupQuestionType.PLAN,
                fill(PLAN_LABEL, serviceName, objectParticle),
                PLAN_HINT.formatted(nextBillingDate.getMonthValue(), nextBillingDate.getDayOfMonth(),
                    money(billingAmount)),
                PLAN_OPTIONS)
        );
    }

    /**
     * 저장한 점수(1~5)에 해당하는 선택지 문구. 이용도는 이 카테고리의 선택지, 만족도 · 계획은 공통 선택지다.
     */
    public String answerOf(CheckupQuestionType type, int score) {
        List<String> options = switch (type) {
            case USAGE -> usageOptions;
            case SATISFACTION -> SATISFACTION_OPTIONS;
            case PLAN -> PLAN_OPTIONS;
        };
        return options.get(score - 1);
    }

    public String memoPlaceholder() {
        return memoPlaceholder;
    }

    public List<String> memoChips() {
        return memoChips;
    }

    private static String fill(String template, String serviceName, String objectParticle) {
        return template
            .replace("{서비스명}", serviceName)
            .replace("{을/를}", objectParticle)
            .replace("{으로/로}", instrumentalParticle(serviceName, objectParticle));
    }

    /**
     * 으로/로 — 한글 음절로 끝나면 받침이 없거나 ㄹ받침이면 '로', 그 외 받침은 '으로'. 영문 등은 목적격 조사가 '을'이면 '으로', '를'이면 '로'.
     */
    private static String instrumentalParticle(String serviceName, String objectParticle) {
        char last = serviceName.charAt(serviceName.length() - 1);
        if (last >= '가' && last <= '힣') {
            int finalConsonant = (last - '가') % 28;
            return finalConsonant == 0 || finalConsonant == Usage.RIEUL ? "로" : "으로";
        }
        return "을".equals(objectParticle) ? "으로" : "로";
    }

    private static String money(long amount) {
        return "%,d".formatted(amount);
    }

    private static final class Usage {

        private static final String DAILY_HINT = "하루에 여러 번 써도 1일로 세어 주세요";
        private static final List<String> DAILY_OPTIONS =
            List.of("안 썼어요", "한 달에 1~3일", "주 1~2일", "주 3~5일", "거의 매일");
        private static final int RIEUL = 8;
    }
}
