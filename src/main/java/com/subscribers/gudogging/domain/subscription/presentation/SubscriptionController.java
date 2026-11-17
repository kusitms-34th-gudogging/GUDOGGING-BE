package com.subscribers.gudogging.domain.subscription.presentation;

import com.subscribers.gudogging.domain.subscription.application.dto.request.CreateSubscriptionsRequest;
import com.subscribers.gudogging.domain.subscription.application.dto.response.SubscriptionResponse;
import com.subscribers.gudogging.domain.subscription.application.usecase.SubscriptionUseCase;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription", description = "구독 관련 API")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;

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
}
