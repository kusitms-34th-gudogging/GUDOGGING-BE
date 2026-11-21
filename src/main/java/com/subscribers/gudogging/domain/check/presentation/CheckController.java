package com.subscribers.gudogging.domain.check.presentation;

import com.subscribers.gudogging.domain.check.application.dto.request.DecideCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.request.SubmitCheckupRequest;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupDecisionResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupResultResponse;
import com.subscribers.gudogging.domain.check.application.dto.response.CheckupSubmissionResponse;
import com.subscribers.gudogging.domain.check.application.usecase.CheckUseCase;
import com.subscribers.gudogging.domain.check.presentation.response.CheckResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Checkup", description = "구독 점검 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckController {

    private final CheckUseCase checkUseCase;

    @Operation(
        summary = "구독 점검 문항 조회",
        description = """
            **기능**
            구독 카테고리에 맞는 점검 문항 3개(이용도 · 만족도 · 다음 달 계획)와 지난 메모, 메모 예시 문구 · 칩을 조회합니다 (K-11~K-70).
            문항 문구는 서비스명과 받침에 맞는 조사(을/를, 으로/로), 금액 · 날짜를 채운 완성 문장입니다.
            - 만족도 보조 문구 '월 ○원 · 1년이면 ○원'은 월 환산 · 연 환산 금액, 계획 보조 문구 '다음 결제 ○월 ○일 · ○원'은 실제 1회 결제 금액입니다.
            - 선택지는 문항마다 5개이고 score 1(부정) ~ 5(긍정) 순입니다.
            - 꾸준히 쓰는 구독도 직접 점검할 수 있습니다.
            - 구독 종료된 구독이면 40501(409)
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            
            **응답값**
            - category (String): 구독 카테고리 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC)
            - questions (List): 문항 3개 (USAGE → SATISFACTION → PLAN)
              - type (String): USAGE / SATISFACTION / PLAN
              - label (String): 문항 문구
              - hint (String, nullable): 보조 문구
              - options (List): 선택지 5개 — score (Integer, 1~5), text (String)
            - lastMemo (String, nullable): 가장 최근 점검의 메모. 없으면 null
            - serviceName (String): 서비스명
            - amount (Long): 월 환산 금액 (원)
            - yearlyAmount (Long): 연 환산 금액 (원)
            - nextBillingDate (String): 다음 결제일 (yyyy-MM-dd)
            - memoPlaceholder (String): 메모 입력란 예시 문구
            - memoChips (List<String>): 예시 메모 칩 3개
            """
    )
    @GetMapping("/subscriptions/{subscriptionId}/checkups/form")
    public CommonResponse<CheckupFormResponse> getCheckupForm(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId
    ) {
        CheckupFormResponse response = checkUseCase.getCheckupForm(userId, subscriptionId);
        return CommonResponse.success(CheckResponseCode.CHECKUP_FORM_FOUND, response);
    }

    @Operation(
        summary = "구독 점검 제출",
        description = """
            **기능**
            점검 4/4(K-70) '결과 보기'에서 이용도 · 만족도 · 계획 응답과 메모를 저장하고, 활용도(%)와 결과 단계를 계산해 함께 저장합니다 (→ K-80).
            활용도 = 반올림((이용도−1)÷4×40 + (만족도−1)÷4×25 + (계획−1)÷4×35). 계획이 1점이면 최대 59%입니다.
            단계: 80 이상 KEEP_RECOMMENDED / 60~79 KEEP_CONSIDER / 40~59 CANCEL_CONSIDER / 40 미만 CANCEL_RECOMMENDED.
            점검 대상 결제일(이번 다음 결제일)을 저장해 이번 결제 주기 점검 완료로 표시합니다. 같은 주기에 다시 제출하면 새 점검으로 저장됩니다.
            메모는 앞뒤 공백을 지우고, 공백뿐이면 저장하지 않습니다. 꾸준히 쓰는 구독도 상세의 '구독 점검' 버튼으로 직접 점검할 수 있어 제출을 받습니다.
            응답은 checkupId만 내려주며, 결과 화면(K-80)은 결과 조회 API로 조회합니다.
            - 이용도 · 만족도 · 계획 응답이 하나씩이 아니면(type 중복 · 누락) 40502(400)
            - 구독 종료된 구독이면 40501(409)
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 입력값 검증 실패 시 40001(400), 잘못된 type 값은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            - answers (List, 필수): 응답 3개
              - type (String, 필수): USAGE / SATISFACTION / PLAN
              - score (Integer, 필수): 선택한 점수 (1~5)
            - memo (String, 선택): 메모 (최대 100자)
            
            **응답값**
            - checkupId (UUID): 저장한 점검 ID
            """
    )
    @PostMapping("/subscriptions/{subscriptionId}/checkups")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CheckupSubmissionResponse> submitCheckup(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId,
        @RequestBody @Valid SubmitCheckupRequest request
    ) {
        CheckupSubmissionResponse response = checkUseCase.submitCheckup(userId, subscriptionId, request);
        return CommonResponse.success(CheckResponseCode.CHECKUP_SUBMITTED, response);
    }

    @Operation(
        summary = "구독 점검 결과 조회",
        description = """
            **기능**
            점검 결과 화면(K-80)에 필요한 활용도 · 단계 · 응답 문구 · 메모 · 가장 큰 영향 문항(mainFactor), 같은 카테고리 지출, 절약 제안(번들 추천)을 조회합니다.
            checkupId는 구독 점검 제출 API 응답에서 받습니다. 구독 종료된 구독의 점검도 조회할 수 있습니다(이때 nextBillingDate = null).
            - 응답 문구는 점검 당시 카테고리의 선택지 문구입니다.
            - mainFactor: 활용도 60% 이상은 가중 점수, 60% 미만은 가중 감점이 가장 큰 문항. 동점이면 계획 > 이용도 > 만족도, 계획 1점이면 PLAN.
            - categorySpending: 점검 당시 카테고리의 구독 중 구독을 월 환산 금액으로 합산합니다. 점검한 구독이 맨 위(구독 종료여도 포함)이고 나머지는 월 금액이 큰 순이며, 개수와 상관없이 모든 구독을 내려줍니다.
            - bundles: 점검한 요금제가 포함된 상품 중 확정 절약(절약액 큰 순) → 카드(최대 금액 큰 순) → 통신 요금제 순, 같은 제공사 유형 최대 2개 · 전체 최대 4개. 0개면 빈 배열(영역 숨김). 직접 입력 구독은 빈 배열
              - 확정 절약: 구독 중인 포함 서비스 월 금액 합 − 상품 가격 (0원 이하 제외, 택1은 가장 비싼 1개)
              - 카드: 구독 중인 포함 서비스 월 금액 × 할인율 (택1은 가장 비싼 1개)
              - 통신 요금제: 계산 없이 혜택(tags[1]) 표시
            - 추천 문구 · 유형명 · 보조 문구는 클라이언트가 stage · mainFactor로 고릅니다.
            - 없거나 삭제됐거나 다른 회원의 점검이면 40503(404)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - checkupId (UUID, 필수): 점검 ID (path variable)
            
            **응답값**
            - checkupId (UUID): 점검 ID
            - subscriptionId (UUID): 구독 ID
            - serviceName (String): 서비스명
            - nextBillingDate (String, nullable): 다음 결제일 (yyyy-MM-dd). 구독 종료면 null
            - yearlyCost (Long): 연 환산 비용 (원)
            - score (Integer): 활용도 (0~100, %)
            - stage (String): KEEP_RECOMMENDED / KEEP_CONSIDER / CANCEL_CONSIDER / CANCEL_RECOMMENDED
            - usageAnswer / satisfactionAnswer / planAnswer (String): 이용도 · 만족도 · 계획 응답 문구
            - memo (String, nullable): 메모
            - bundles (List): 절약 제안 카드
              - name (String): 상품명
              - includes (String): 포함 내용 한 줄
              - tags (List<String>): [제공사 · 상품군, 핵심 혜택]
              - priceType (String): FIXED(확정 절약 '월 N원') / MAX(카드 '최대 N원') / BENEFIT(통신 요금제, 큰 숫자 대신 tags[1])
              - monthlySaving (Long, nullable): 큰 숫자 금액. BENEFIT은 null
              - savingRate (Integer, nullable): 절약 막대 % (0~100). BENEFIT은 null
              - monthlyPrice (Integer, nullable): 아래 문구 상품 월 가격 (FIXED '월 N원에 이용', BENEFIT '요금제 월 N원'). MAX는 null
            - mainFactor (String): USAGE / SATISFACTION / PLAN
            - categorySpending (Object): 같은 카테고리 지출
              - category (String): 카테고리
              - monthlyTotal (Long): 월 환산 합계 (원)
              - subscriptionCount (Integer): 구독 개수
              - items (List): 같은 카테고리 구독 전부 — serviceName (String), logoUrl (String, nullable), monthlyAmount (Long)
            """
    )
    @GetMapping("/checkups/{checkupId}")
    public CommonResponse<CheckupResultResponse> getCheckupResult(
        @CurrentUserId UUID userId,
        @PathVariable UUID checkupId
    ) {
        CheckupResultResponse response = checkUseCase.getCheckupResult(userId, checkupId);
        return CommonResponse.success(CheckResponseCode.CHECKUP_RESULT_FOUND, response);
    }

    @Operation(
        summary = "구독 점검 유지/해지 결정",
        description = """
            **기능**
            점검 결과(K-80) '유지하기' 또는 해지 이유(K-90) '해지 방법 보기' · '건너뛰기'에서 결정을 저장합니다. 결정 시각도 함께 저장합니다.
            - KEEP: 유지하기를 고른 구독으로 기록합니다 (홈 사용 현황 제외는 홈 API가 판단). 해지 이유는 보내도 무시합니다.
            - CANCEL: 해지 이유를 함께 저장합니다. 여러 개 고를 수 있고, 건너뛰기면 보내지 않습니다.
              직접 입력(CUSTOM)을 고른 경우에만 cancelReasonText를 앞뒤 공백을 지워 저장합니다 (CUSTOM이 없거나 텍스트가 비면 저장하지 않음).
            - 같은 점검에 다시 결정하면 마지막 결정으로 덮어씁니다 (빠진 해지 이유는 삭제). 지난 점검 · 구독 종료된 구독의 점검도 결정할 수 있습니다.
            - 카테고리별 해지 이유 문구는 클라이언트가 표시하고, 서버는 코드만 주고받습니다.
            - 없거나 삭제됐거나 다른 회원의 점검이면 40503(404)
            - 입력값 검증 실패 시 40001(400), 잘못된 enum 값은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - checkupId (UUID, 필수): 점검 ID (path variable)
            - decision (String, 필수): KEEP / CANCEL
            - cancelReasons (List<String>, 선택): 해지 이유 코드 — RARELY_USED / PRICE_BURDEN / LACKING / SWITCHING / GOAL_ACHIEVED / CUSTOM
            - cancelReasonText (String, 선택): 직접 입력한 해지 이유 (최대 100자)
            
            **응답값**
            - checkupId (UUID): 점검 ID
            - decision (String): KEEP / CANCEL
            """
    )
    @PatchMapping("/checkups/{checkupId}/decision")
    public CommonResponse<CheckupDecisionResponse> decideCheckup(
        @CurrentUserId UUID userId,
        @PathVariable UUID checkupId,
        @RequestBody @Valid DecideCheckupRequest request
    ) {
        CheckupDecisionResponse response = checkUseCase.decideCheckup(userId, checkupId, request);
        return CommonResponse.success(CheckResponseCode.CHECKUP_DECIDED, response);
    }
}
