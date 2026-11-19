package com.subscribers.gudogging.domain.check.presentation;

import com.subscribers.gudogging.domain.check.application.dto.response.CheckupFormResponse;
import com.subscribers.gudogging.domain.check.application.usecase.CheckUseCase;
import com.subscribers.gudogging.domain.check.presentation.response.CheckResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
