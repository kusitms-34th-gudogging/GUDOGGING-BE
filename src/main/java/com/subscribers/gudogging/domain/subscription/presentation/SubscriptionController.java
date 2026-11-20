package com.subscribers.gudogging.domain.subscription.presentation;

import com.subscribers.gudogging.domain.subscription.application.dto.request.ChangeSubscriptionSteadyRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.DeleteSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SubscriptionSortType;
import com.subscribers.gudogging.domain.subscription.application.dto.request.ReactivateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.SaveSubscriptionDraftRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.request.UpdateSubscriptionRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.CancelGuideResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionBillingResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCancelResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionCreationResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDeletionResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDetailResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDraftResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionDraftSaveResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionListResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionReactivationResponse;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionSteadyResponse;
import com.subscribers.gudogging.domain.subscription.application.usecase.SubscriptionUseCase;
import com.subscribers.gudogging.domain.subscription.domain.entity.SubscriptionStatus;
import com.subscribers.gudogging.domain.subscription.presentation.response.SubscriptionResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        summary = "구독 상세 조회",
        description = """
            **기능**
            구독 1건의 기본 정보와 결제 계산 값을 조회합니다. 구독 중 상세(M-03) · 구독 종료 상세(M-05)와
            캘린더에서 날짜를 눌러 들어온 구독 상세(C-03)에 사용합니다.
            baseDate가 있으면 그 날짜 기준으로 상태(등록일 ~ 해지일 사이면 ACTIVE) · 다음 결제일 · D-n을 계산합니다.
            지금까지 결제 금액 · 횟수는 baseDate와 무관하게 오늘(구독 종료면 해지일)까지로 계산합니다.
            점검 기록(latestCheckup · pastCheckups · pastCheckupAverage)은 구독 점검 기능 구현 전까지 항상 null · 빈 배열입니다.
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            - baseDate (String, 선택): 기준일 (yyyy-MM-dd). 캘린더 C-03에서 선택한 날짜. 없으면 오늘 (query parameter)
            
            **응답값**
            - subscriptionId (UUID): 구독 ID
            - status (String): ACTIVE / ENDED. baseDate가 없으면 저장된 상태, 있으면 baseDate 기준 상태
            - serviceName (String): 서비스명
            - logoUrl (String, nullable): 서비스 로고 URL. null이면 기본 이미지 사용
            - category (String): 카테고리 코드
            - amount (Integer): 1회 결제 금액 (원)
            - billingCycleValue (Integer): 결제 주기 값
            - billingCycleUnit (String): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
            - nextBillingDate (String, nullable): 기준일 이후(포함) 첫 결제일 (yyyy-MM-dd). 구독 종료이고 해지일 이후면 null
            - startedAt (String): 구독 시작일 = 등록일 (yyyy-MM-dd)
            - paymentMethod (String): 결제한 곳 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY)
            - isSteady (Boolean): '꾸준히 쓰는 구독' 여부
            - canceledAt (String, nullable): 해지일 (yyyy-MM-dd). 구독 중이면 null
            - yearlyAmount (Integer): 연 환산 금액 (원)
            - totalPaidAmount (Integer): 지금까지 결제 금액 (원) = 결제 횟수 × 금액
            - paidCount (Integer): 지금까지 결제 횟수
            - dDay (Integer, nullable): 다음 결제일 − 기준일. nextBillingDate가 null이면 null
            - latestCheckup (Object, nullable): 가장 최근 점검 1건. 현재 항상 null
            - pastCheckups (List): 최근 90일 점검 기록. 현재 항상 빈 배열
            - pastCheckupAverage (Object, nullable): 최근 90일 점검 평균. 현재 항상 null
            """
    )
    @GetMapping("/{subscriptionId}")
    public CommonResponse<SubscriptionDetailResponse> getSubscription(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        SubscriptionDetailResponse response = subscriptionUseCase.getSubscription(userId, subscriptionId, baseDate);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_FOUND, response);
    }

    @Operation(
        summary = "해지 안내 조회 (결제 경로별)",
        description = """
            **기능**
            구독 해지 안내 화면(X-01 / X-03 / X-04 / X-05)에 보여줄 해지 방법 3단계 · 주의 문구 · 해지 링크를 조회합니다.
            구독의 결제 경로(paymentMethod)와 서비스 해지 링크 유무로 안내가 정해집니다 (구독 상태 ACTIVE/ENDED는 검사하지 않음).
            - APP_STORE: App Store 구독 관리 링크 / GOOGLE_PLAY: Google Play 정기 결제 링크
            - WEB_DIRECT + 서비스 해지 링크 있음: 그 해지 링크 / WEB_DIRECT + 링크 없음(직접 입력 구독 포함): cancelUrl = null
            - steps의 서비스명은 받침에 맞는 목적격 조사(을/를)와 함께 채워집니다. 버튼 문구 · 화면 제목은 클라이언트가 처리합니다.
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            
            **응답값**
            - serviceName (String): 서비스명
            - paymentMethod (String): 결제한 곳 (APP_STORE, GOOGLE_PLAY, WEB_DIRECT)
            - steps (List<String>): 해지 방법 3단계
            - cancelUrl (String, nullable): 해지 페이지 링크. 없으면 null
            - caution (String): 주의 문구
            """
    )
    @GetMapping("/{subscriptionId}/cancel-guide")
    public CommonResponse<CancelGuideResponse> getCancelGuide(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId
    ) {
        CancelGuideResponse response = subscriptionUseCase.getCancelGuide(userId, subscriptionId);
        return CommonResponse.success(SubscriptionResponseCode.CANCEL_GUIDE_FOUND, response);
    }

    @Operation(
        summary = "해지 완료 처리",
        description = """
            **기능**
            해지 완료 확인 모달(X-06)에서 '해지 완료'를 누르면 구독 중인 구독을 구독 종료(ENDED)로 바꾸고 해지일을 오늘로 저장합니다.
            결제 경로와 관계없이 같은 동작이며, 완료 후 구독 종료 상세(M-05)로 돌아갑니다.
            D-3 · D-1 알림과 홈 · 캘린더 집계는 구독 상태로 계산되므로 구독 종료 후에는 대상에서 빠집니다.
            - 이미 구독 종료된 구독이면 40406(409)
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            
            **응답값**
            - subscriptionId (UUID): 구독 ID
            - status (String): ENDED
            - canceledAt (String): 해지일 (yyyy-MM-dd)
            """
    )
    @PatchMapping("/{subscriptionId}/cancel")
    public CommonResponse<SubscriptionCancelResponse> cancelSubscription(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId
    ) {
        SubscriptionCancelResponse response = subscriptionUseCase.cancelSubscription(userId, subscriptionId);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_CANCELED, response);
    }

    @Operation(
        summary = "구독 종료 구독 재등록",
        description = """
            **기능**
            구독 종료 상세(M-05)의 '재등록하기' → 기존 정보가 채워진 구독 정보 입력 화면에서 저장하면, 같은 구독을 다시 구독 중(ACTIVE)으로 바꿉니다 (구독 ID 유지).
            요청한 구독 정보로 값을 덮어쓰고, 다음 결제일을 결제 기준일로 저장하고, 해지일을 지우고, 구독 시작일을 오늘(재등록일)로 다시 잡습니다.
            그래서 연 환산 금액 · 다음 결제일 · 지금까지 결제 횟수 · 금액과 캘린더는 재등록일부터 새 조건으로 계산합니다. 기존 점검 기록은 유지합니다.
            필드 정의와 검증은 구독 정보 수정 API와 같습니다 (부분 수정 아님, serviceName만 선택).
            - 이미 구독 중인 구독이면 40407(409)
            - 같은 서비스를 다른 구독으로 이미 구독 중이면 40401(409). 직접 입력한 구독은 검사하지 않습니다.
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 입력값 검증 실패 시 40001(400), 잘못된 enum · 날짜 형식은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            - serviceName (String, 선택): 서비스 이름 (최대 30자, 공백만 불가). 직접 입력 서비스만 반영, null이면 기존 이름 유지
            - category (String, 필수): 카테고리 코드 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC)
            - amount (Integer, 필수): 결제 금액 (원, 1 이상)
            - billingCycleValue (Integer, 필수): 결제 주기 값 (1~31)
            - billingCycleUnit (String, 필수): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
            - nextBillingDate (String, 필수): 다음 결제일 (yyyy-MM-dd, 오늘 이후)
            - paymentMethod (String, 필수): 결제한 곳 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY)
            
            **응답값**
            - subscriptionId (UUID): 구독 ID
            - status (String): ACTIVE
            """
    )
    @PatchMapping("/{subscriptionId}/reactivate")
    public CommonResponse<SubscriptionReactivationResponse> reactivateSubscription(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId,
        @RequestBody @Valid ReactivateSubscriptionRequest request
    ) {
        SubscriptionReactivationResponse response = subscriptionUseCase.reactivateSubscription(userId,
            subscriptionId, request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_REACTIVATED, response);
    }

    @Operation(
        summary = "꾸준히 쓰는 구독 설정",
        description = """
            **기능**
            구독 상세(M-03)의 '꾸준히 쓰는 구독' 토글('점검 알림 없이 그대로 유지해요') 값을 저장합니다.
            - 켜면(true): D-3 점검 알림을 보내지 않고, 홈 점검 유도 · 결제 예정 점검 버튼(홈 '구독 점검하기')에서 빠집니다.
            - 켜도 그대로: D-1 결제 알림 발송(토글과 관계없음), 구독 목록 · 캘린더 · 결제 정보 표시, 상세 '구독 점검' 버튼으로 직접 점검 가능
            - 끄면(false): 다음 결제 일정부터 점검 알림을 다시 발송합니다.
            현재 값과 같은 값을 보내도 성공합니다.
            - 구독 종료(ENDED) 구독이면 40405(409)
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - isSteady가 없거나 null이면 40001(400), 불리언이 아니면 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            - isSteady (Boolean, 필수): 꾸준히 쓰는 구독 여부
            
            **응답값**
            - subscriptionId (UUID): 구독 ID
            - isSteady (Boolean): 변경된 값
            """
    )
    @PatchMapping("/{subscriptionId}/steady")
    public CommonResponse<SubscriptionSteadyResponse> changeSubscriptionSteady(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId,
        @RequestBody @Valid ChangeSubscriptionSteadyRequest request
    ) {
        SubscriptionSteadyResponse response = subscriptionUseCase.changeSubscriptionSteady(userId, subscriptionId,
            request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_STEADY_CHANGED, response);
    }

    @Operation(
        summary = "구독 정보 수정",
        description = """
            **기능**
            구독 상세(M-03) 더보기 › 편집(M-06)에서 구독 중인 구독의 결제 정보를 수정합니다.
            편집 화면은 저장된 값을 채운 상태로 전체 값을 보내므로 serviceName 외 필드는 모두 필수입니다 (부분 수정 아님).
            서비스명은 직접 입력한 구독만 바꿀 수 있고(앞뒤 공백 제거 후 저장, null이면 기존 이름 유지), 사전 등록 서비스는 serviceName을 무시합니다.
            다음 결제일을 결제 기준일로 다시 저장하므로 연 환산 · 다음 결제일 · 캘린더 · 점검 알림 일정은 자동으로 바뀝니다.
            서비스 자체는 바꿀 수 없습니다 (삭제 후 새로 등록).
            - 구독 종료(ENDED) 구독이면 40405(409)
            - 없거나 삭제됐거나 다른 회원의 구독이면 40404(404)
            - 입력값 검증 실패 시 40001(400), 잘못된 enum · 날짜 형식은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - subscriptionId (UUID, 필수): 구독 ID (path variable)
            - serviceName (String, 선택): 서비스 이름 (최대 30자, 공백만 불가). 직접 입력 서비스만 반영
            - category (String, 필수): 카테고리 코드 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC)
            - amount (Integer, 필수): 결제 금액 (원, 1 이상)
            - billingCycleValue (Integer, 필수): 결제 주기 값 (1~31)
            - billingCycleUnit (String, 필수): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
            - nextBillingDate (String, 필수): 다음 결제일 (yyyy-MM-dd, 오늘 이후)
            - paymentMethod (String, 필수): 결제한 곳 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY)
            
            **응답값**
            - subscriptionId (UUID): 구독 ID
            - yearlyAmount (Integer): 재계산된 연 환산 금액 (원)
            - nextBillingDate (String): 다음 결제일 (yyyy-MM-dd)
            """
    )
    @PatchMapping("/{subscriptionId}")
    public CommonResponse<SubscriptionBillingResponse> updateSubscription(
        @CurrentUserId UUID userId,
        @PathVariable UUID subscriptionId,
        @RequestBody @Valid UpdateSubscriptionRequest request
    ) {
        SubscriptionBillingResponse response = subscriptionUseCase.updateSubscription(userId, subscriptionId,
            request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_UPDATED, response);
    }

    @Operation(
        summary = "구독 일괄 등록 (직접 · 스크린샷 · Gmail 공통)",
        description = """
            **기능**
            선택한 구독 확인(A-04) · 스크린샷 등록 확인(A-09) · Gmail 등록 확인(A-13)의 'N개 구독 등록하기'에서 여러 구독을 한 번에 등록합니다.
            등록 경로(source)는 구독의 등록 경로로 저장합니다.
            serviceId가 있으면 사전 등록 서비스로, 서비스명은 요청 serviceName을 무시하고 서비스 목록의 이름을 사용합니다.
            serviceId가 null이면 직접 입력 서비스로, serviceName(앞뒤 공백 제거 후 1~30자)이 필수입니다.
            하나라도 등록할 수 없으면 전체가 실패하며 아무것도 저장하지 않습니다.
            - 한 요청에 같은 serviceId가 2번 이상이면 40402(400)
            - 없거나 비활성인 serviceId가 있으면 40301(404)
            - 이미 구독 중(ACTIVE)인 서비스가 있으면 40401(409). 구독 종료(ENDED)된 서비스는 다시 등록할 수 있습니다.
            - 입력값 검증 실패 시 40001(400), 잘못된 enum·날짜 형식은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - source (String, 필수): 등록 경로 (MANUAL, SCREENSHOT, GMAIL)
            - subscriptions (List, 필수): 등록할 구독 목록 (1~50개)
              - serviceId (UUID, 선택): 사전 등록 서비스 ID. 직접 입력 서비스는 null
              - serviceName (String, 직접 입력 시 필수): 서비스 이름 (최대 30자). serviceId가 있으면 무시
              - category (String, 필수): 카테고리 코드 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC)
              - amount (Integer, 필수): 결제 금액 (원, 1 이상)
              - billingCycleValue (Integer, 필수): 결제 주기 값 (1~31)
              - billingCycleUnit (String, 필수): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
              - nextBillingDate (String, 필수): 다음 결제일 (yyyy-MM-dd, 오늘 이후)
              - paymentMethod (String, 필수): 결제 수단 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY)
            
            **응답값**
            - createdCount (Integer): 등록된 구독 개수 — '등록된 구독 N'
            - subscriptions (List): 방금 등록한 구독 목록 (요청 순서와 동일)
              - subscriptionId (UUID): 구독 ID
              - serviceName (String): 서비스 이름
              - logoUrl (String, nullable): 서비스 로고 URL. null이면 기본 이미지 사용
              - amount (Integer): 결제 금액 (원)
              - billingCycleValue (Integer): 결제 주기 값
              - billingCycleUnit (String): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR)
              - nextBillingDate (String): 다음 결제일 (yyyy-MM-dd)
              - paymentMethod (String): 결제 수단 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY)
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<SubscriptionCreationResponse> createSubscriptions(
        @CurrentUserId UUID userId,
        @RequestBody @Valid CreateSubscriptionsRequest request
    ) {
        SubscriptionCreationResponse response = subscriptionUseCase.createSubscriptions(userId, request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTIONS_CREATED, response);
    }

    @Operation(
        summary = "구독 등록 임시저장",
        description = """
            **기능**
            직접 등록 중인 선택한 구독 목록과 등록을 중단한 화면(lastStep · editingItemIndex)을 서버에 임시저장합니다.
            회원당 1건이며, 이미 임시저장이 있으면 새 내용으로 통째로 덮어씁니다. 따로 만료되지 않습니다.
            목록이 바뀔 때(A-02 선택 완료, A-03 / A-03s 저장하기, 선택 추가 · 해제)와 화면 이동 시 호출합니다.
            - A-02 ⇄ A-04 이동
            - A-04에서 항목을 눌러 A-03 / A-03s로 들어갈 때 (INPUT_SUBSCRIPTION + 그 항목 순번)
            - A-02s '직접 입력하기'로 A-03s에 들어갈 때 (INPUT_SUBSCRIPTION + editingItemIndex null)
            - A-03 / A-03s에서 A-04로 돌아올 때 (저장하기 · 뒤로가기 · 이탈 팝업 '나가기'), 새 직접 입력 A-03s에서 A-02로 돌아올 때
            A-03 / A-03s에서 저장하지 않은 입력값은 보내지 않습니다 (일부만 입력한 상태로 임시저장 불가).
            구독 등록(POST /api/v1/subscriptions)에 성공하면 서버가 임시저장을 삭제합니다.
            결제 정보(category · amount · billingCycleValue · billingCycleUnit · nextBillingDate · paymentMethod) 규칙:
            - 사전 등록 서비스(serviceId 있음): 6개 모두 null(결제 정보 미입력) 또는 모두 입력
            - 직접 입력 서비스(serviceId null): 6개 모두 입력
            - 다음 결제일은 과거 날짜도 저장할 수 있습니다 (구독 등록 때 오늘 이후인지 검사)
            - 서비스 존재 · 이미 구독 중 여부는 검사하지 않습니다 (구독 등록 때 검사)
            - 한 요청에 같은 serviceId가 2번 이상이면 40402(400)
            - editingItemIndex가 INPUT_SUBSCRIPTION이 아닌데 있거나 items 범위를 벗어나면 40001(400)
            - 입력값 검증 실패 시 40001(400), 잘못된 enum · 날짜 형식은 40002(400)
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - lastStep (String, 필수): 불러오기 때 복원할 화면 (SELECT_SERVICES = A-02 서비스 선택, CONFIRM_SUBSCRIPTIONS = A-04 선택한 구독 확인, INPUT_SUBSCRIPTION = A-03 / A-03s 구독 정보 입력)
            - editingItemIndex (Integer, 선택): lastStep이 INPUT_SUBSCRIPTION일 때 입력 · 수정 중이던 항목의 items 순번(0부터). 새 직접 입력(A-03s)이면 null, 다른 lastStep이면 보내지 않음
            - items (List, 필수): 임시저장할 구독 목록 (1~50개, 순서 유지)
              - serviceId (UUID, 선택): 사전 등록 서비스 ID. 직접 입력 서비스는 null
              - serviceName (String, 필수): 서비스명 (최대 50자, 직접 입력 서비스는 앞뒤 공백 제거 후 30자 이하)
              - category (String, 선택): 카테고리 코드 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC). 미입력이면 null
              - amount (Integer, 선택): 결제 금액 (원, 1 이상). 미입력이면 null
              - billingCycleValue (Integer, 선택): 결제 주기 값 (1~31). 미입력이면 null
              - billingCycleUnit (String, 선택): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR). 미입력이면 null
              - nextBillingDate (String, 선택): 다음 결제일 (yyyy-MM-dd). 미입력이면 null
              - paymentMethod (String, 선택): 결제 수단 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY). 미입력이면 null
            
            **응답값**
            - savedAt (String): 저장 시각 (ISO 8601, yyyy-MM-dd'T'HH:mm:ss)
            """
    )
    @PutMapping("/drafts")
    public CommonResponse<SubscriptionDraftSaveResponse> saveSubscriptionDraft(
        @CurrentUserId UUID userId,
        @RequestBody @Valid SaveSubscriptionDraftRequest request
    ) {
        SubscriptionDraftSaveResponse response = subscriptionUseCase.saveSubscriptionDraft(userId, request);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_DRAFT_SAVED, response);
    }

    @Operation(
        summary = "구독 등록 임시저장 불러오기",
        description = """
            **기능**
            로그인한 회원의 구독 등록 임시저장을 조회합니다. 구독 추가 화면(A-01)에 들어올 때 호출합니다.
            임시저장이 있으면 불러오기 팝업(A-01s)을 띄우고, 없으면 data = null이므로 등록 방법 선택 화면(A-01)을 바로 표시합니다.
            '불러오기'를 선택하면 lastStep · editingItemIndex로 등록을 중단한 화면을 복원합니다.
            - SELECT_SERVICES: A-02에 items를 선택 상태로 복원 (결제 정보 · 직접 입력 항목은 A-04로 넘어갈 때 표시)
            - CONFIRM_SUBSCRIPTIONS: A-04에 items를 복원
            - INPUT_SUBSCRIPTION + editingItemIndex: A-04 복원 후 items[editingItemIndex]의 입력 화면으로 이동 (serviceId 있으면 A-03, null이면 A-03s)
            - INPUT_SUBSCRIPTION + editingItemIndex null: A-02 복원 후 새 직접 입력(A-03s)으로 이동
            '새로 등록하기'를 선택하면 임시저장 삭제 API(DELETE /api/v1/subscriptions/drafts)를 호출합니다.
            불러오기는 임시저장을 지우지 않으며, 구독 등록(POST /api/v1/subscriptions)에 성공하면 서버가 삭제합니다.
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)
            
            **응답값** (임시저장이 없으면 data = null)
            - savedAt (String): 마지막 저장 시각 (ISO 8601, yyyy-MM-dd'T'HH:mm:ss)
            - lastStep (String): 등록을 중단한 화면 (SELECT_SERVICES, CONFIRM_SUBSCRIPTIONS, INPUT_SUBSCRIPTION)
            - editingItemIndex (Integer, nullable): lastStep이 INPUT_SUBSCRIPTION일 때 입력 · 수정 중이던 항목의 items 순번(0부터). null이면 새 직접 입력
            - items (List): 임시저장한 구독 목록 (저장한 순서 그대로)
              - serviceId (UUID, nullable): 사전 등록 서비스 ID. 직접 입력 서비스는 null
              - serviceName (String): 서비스명
              - logoUrl (String, nullable): 서비스 로고 URL. null이면 기본 이미지 사용
              - category (String, nullable): 카테고리 코드. 결제 정보 미입력이면 null
              - amount (Integer, nullable): 결제 금액 (원). 결제 정보 미입력이면 null
              - billingCycleValue (Integer, nullable): 결제 주기 값. 결제 정보 미입력이면 null
              - billingCycleUnit (String, nullable): 결제 주기 단위 (DAY, WEEK, MONTH, YEAR). 결제 정보 미입력이면 null
              - nextBillingDate (String, nullable): 다음 결제일 (yyyy-MM-dd). 결제 정보 미입력이면 null. 저장 후 날짜가 지나 과거일 수 있음
              - paymentMethod (String, nullable): 결제 수단 (WEB_DIRECT, APP_STORE, GOOGLE_PLAY). 결제 정보 미입력이면 null
            """
    )
    @GetMapping("/drafts")
    public CommonResponse<SubscriptionDraftResponse> getSubscriptionDraft(@CurrentUserId UUID userId) {
        SubscriptionDraftResponse response = subscriptionUseCase.getSubscriptionDraft(userId);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_DRAFT_FOUND, response);
    }

    @Operation(
        summary = "구독 등록 임시저장 삭제",
        description = """
            **기능**
            로그인한 회원의 구독 등록 임시저장을 삭제합니다.
            불러오기 팝업(A-01s)에서 '새로 등록하기'를 선택하면 호출합니다. 삭제 후에는 구독 추가 화면(A-01) 진입 시 불러오기 팝업이 뜨지 않습니다.
            구독 등록(POST /api/v1/subscriptions)에 성공하면 서버가 임시저장을 삭제하므로 따로 호출하지 않아도 됩니다.
            저장된 임시저장이 없어도 성공으로 응답합니다.
            - 회원이 없거나 탈퇴했으면 40201(404)
            
            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)
            
            **응답값**
            - 없음 (data = null)
            """
    )
    @DeleteMapping("/drafts")
    public CommonResponse<Void> deleteSubscriptionDraft(@CurrentUserId UUID userId) {
        subscriptionUseCase.deleteSubscriptionDraft(userId);
        return CommonResponse.success(SubscriptionResponseCode.SUBSCRIPTION_DRAFT_DELETED);
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
