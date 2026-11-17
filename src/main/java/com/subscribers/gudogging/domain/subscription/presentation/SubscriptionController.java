package com.subscribers.gudogging.domain.subscription.presentation;

import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.DeleteSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionSortType;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDeletionResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionListResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionResponse;
import com.subscribers.gudogging.domain.subscription.application.usecase.SubscriptionUseCase;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.presentation.response.SubscriptionResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription", description = "구독 관련 API")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;

    @Operation(
        summary = "구독 목록 조회 (탭 · 정렬 · 요약)",
        description = """
            **기능**
            구독 관리 화면(M-02 구독 중 탭 / M-04 구독 종료 탭)의 구독 목록을 조회합니다.
            선택한 탭(status)의 구독을 정렬 기준(sort)대로 반환하고, 탭 뱃지용 두 탭 개수와 탭의 월 환산 합계를 함께 반환합니다.
            - 구독 중 탭 기본 정렬: 다음 결제일 순(같으면 금액 높은 순) / 구독 종료 탭 기본 정렬: 해지일 최근 순
            - 금액 높은 순(AMOUNT_DESC): 구독 중은 결제 금액(amount), 구독 종료는 월 환산 금액(monthlyAmount) 기준
            - 구독 중 탭에 CANCELED_AT, 구독 종료 탭에 NEXT_BILLING_DATE를 보내면 40403(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            구독이 없으면 개수 · 합계 0과 빈 배열을 반환합니다.
            
            **요청값**
            - status (String, 선택): ACTIVE(구독 중) / ENDED(구독 종료). 없으면 ACTIVE (query parameter)
            - sort (String, 선택): NEXT_BILLING_DATE / CANCELED_AT / AMOUNT_DESC. 없으면 탭의 기본 정렬 (query parameter)
            
            **응답값**
            - count (Integer): 선택한 탭의 구독 개수
            - activeCount (Integer): 구독 중 개수 — 탭 뱃지 '구독 중 N'
            - endedCount (Integer): 구독 종료 개수 — 탭 뱃지 '구독 종료 N'
            - monthlyTotal (Integer): 선택한 탭 구독의 월 환산 합계 — 구독 중 '월 N원' / 구독 종료 '월 N원 절약'
            - subscriptions (List): 구독 목록
              - subscriptionId (UUID): 구독 ID
              - serviceName (String): 서비스명
              - logoUrl (String, nullable): 서비스 로고 URL. null이면 기본 이미지 사용
              - amount (Integer): 1회 결제 금액 (원) — 구독 중 탭 우측 금액
              - monthlyAmount (Integer): 월 환산 금액 (원) — 구독 종료 탭 우측 '월 금액'
              - billingCycleValue (Integer): 결제 주기 값
              - billingCycleUnit (String): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
              - nextBillingDate (String, nullable): 다음 결제일 (yyyy-MM-dd). 구독 종료면 null
              - dDay (Integer, nullable): 다음 결제일까지 남은 일수 (오늘이면 0). 구독 종료면 null
              - canceledAt (String, nullable): 해지일 (yyyy-MM-dd). 구독 중이면 null
              - totalPaidAmount (Integer, nullable): 총 지출 — 등록일 ~ 해지일 결제 횟수 × 금액. 구독 중이면 null
            """
    )
    @GetMapping
    public CommonResponse<SubscriptionListResponse> getSubscriptions(
        @CurrentUserId UUID userId,
        @RequestParam(required = false) SubscriptionStatus status,
        @RequestParam(required = false) SubscriptionSortType sort
    ) {
        SubscriptionListResponse response = subscriptionUseCase.getSubscriptions(userId, status, sort);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTIONS_FOUND, response);
    }

    @Operation(
        summary = "구독 직접 등록 (일괄)",
        description = """
            **기능**
            선택한 구독 확인 화면(A-04)의 'N개 구독 등록하기'에서 여러 구독을 한 번에 등록합니다 (registrationSource = MANUAL).
            serviceId가 있으면 사전 등록 서비스로, 서비스명은 요청 name을 무시하고 서비스 목록의 이름을 사용합니다.
            serviceId가 null이면 직접 입력 서비스로, name(앞뒤 공백 제거 후 1~30자)이 필수입니다.
            하나라도 등록할 수 없으면 전체가 실패하며 아무것도 저장하지 않습니다.
            - 한 요청에 같은 serviceId가 2번 이상이면 40402(400)
            - 없거나 비활성인 serviceId가 있으면 40301(404)
            - 이미 구독 중(ACTIVE)인 서비스가 있으면 40401(409). 구독 종료(ENDED)된 서비스는 다시 등록할 수 있습니다.
            - 입력값 검증 실패 시 40001(400), 잘못된 enum·날짜 형식은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptions (List, 필수): 등록할 구독 목록 (1~50개)
              - serviceId (UUID, 선택): 사전 등록 서비스 ID. 직접 입력 서비스는 null
              - name (String, 직접 입력 시 필수): 서비스 이름 (최대 30자). serviceId가 있으면 무시
              - category (String, 필수): 카테고리 코드 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC)
              - amount (Integer, 필수): 결제 금액 (원, 1 이상)
              - billingCycleValue (Integer, 필수): 결제 주기 값 (1~31)
              - billingCycleUnit (String, 필수): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
              - nextBillingOn (String, 필수): 다음 결제일 (yyyy-MM-dd, 오늘 이후)
              - paymentMethod (String, 필수): 결제 수단 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY)
            
            **응답값** (요청 순서와 동일)
            - subscriptionId (UUID): 구독 ID
            - serviceId (UUID, nullable): 사전 등록 서비스 ID. 직접 입력 서비스는 null
            - name (String): 서비스 이름
            - logoUrl (String, nullable): 서비스 로고 URL. null이면 기본 이미지 사용
            - category (String): 카테고리 코드
            - amount (Integer): 결제 금액 (원)
            - billingCycleValue (Integer): 결제 주기 값
            - billingCycleUnit (String): 결제 주기 단위
            - nextBillingOn (String): 다음 결제일 (yyyy-MM-dd)
            - paymentMethod (String): 결제 수단
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<List<SubscriptionResponse>> createSubscriptions(
        @CurrentUserId UUID userId,
        @RequestBody @Valid CreateSubscriptionsRequest request
    ) {
        List<SubscriptionResponse> response = subscriptionUseCase.createSubscriptions(userId, request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTIONS_CREATED, response);
    }

    @Operation(
        summary = "구독 선택 삭제 (일괄)",
        description = """
            **기능**
            구독 관리 목록의 삭제 선택 모드(M-02.2)에서 고른 구독을 삭제 확인 모달(M-02.3)의 '삭제하기'로 한 번에 삭제합니다.
            구독 중 · 구독 종료 구독 모두 삭제할 수 있고, 캘린더 구독 상세(C-03, C-04)의 삭제도 구독 1건으로 이 API를 사용합니다.
            구독과 함께 해당 구독의 점검 기록 · 해지 이유 · 알림도 모두 삭제합니다 (Soft Delete).
            같은 ID가 여러 번 있으면 한 번으로 처리합니다.
            하나라도 삭제할 수 없으면 전체가 실패하며 아무것도 삭제하지 않습니다.
            - 없거나 이미 삭제됐거나 다른 회원의 구독이 포함되면 40404(404)
            - subscriptionIds가 비어 있거나 100개를 넘거나 null 요소가 있으면 40001(400), UUID 형식 오류는 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionIds (List<UUID>, 필수): 삭제할 구독 ID 목록 (1~100개)
            
            **응답값**
            - deletedCount (Integer): 삭제된 구독 개수 (중복 제외)
            """
    )
    @DeleteMapping
    public CommonResponse<SubscriptionDeletionResponse> deleteSubscriptions(
        @CurrentUserId UUID userId,
        @RequestBody @Valid DeleteSubscriptionsRequest request
    ) {
        SubscriptionDeletionResponse response = subscriptionUseCase.deleteSubscriptions(userId, request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTIONS_DELETED, response);
    }
}
