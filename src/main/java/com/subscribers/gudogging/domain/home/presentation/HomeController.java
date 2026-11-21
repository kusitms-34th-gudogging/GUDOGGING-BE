package com.subscribers.gudogging.domain.home.presentation;

import com.subscribers.gudogging.domain.home.application.dto.response.HomeResponse;
import com.subscribers.gudogging.domain.home.application.usecase.HomeUseCase;
import com.subscribers.gudogging.domain.home.presentation.response.HomeResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 관련 API")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeUseCase homeUseCase;

    @Operation(
        summary = "홈 조회",
        description = """
            **기능**
            홈 화면 정보(이번 달 요약 · 구독 점검하기 · 잘 안 쓰는 구독 · 읽지 않은 알림)를 한 번에 조회합니다 (H-01, H-02, H-03).
            - 구독 중인 구독만 대상입니다. 구독이 없으면 금액 · 개수는 0, 목록은 빈 배열, nudgeSubscriptionId는 null입니다.
            - 이번 달 결제 예정액: 오늘 ~ 이번 달 말일 결제 금액 합계
            - 연 금액: 올해 1~12월 결제 합계. 앱 등록 전 기간도 같은 주기로 결제했다고 보고 포함합니다 ('1년에 이 정도' 예상치).
            - 구독 점검하기: 이번 결제 주기에 점검하지 않은 구독 중 결제일 가까운 순(같으면 금액 큰 순) 최대 5개, 7일 제한 없음. 꾸준히 쓰는 구독 제외
            - 잘 안 쓰는 구독: 최근 점검 활용도 60% 미만이고 유지하기를 고르지 않은 구독. 활용도 낮은 순(같으면 결제일 가까운 순) 최대 3개 미리보기, 꾸준히 쓰는 구독 제외
            - 점검 유도: 꾸준히 쓰는 구독을 뺀 구독 중 결제일이 가장 가까운 구독
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음

            **응답값**
            - summary (Object): 이번 달 요약
              - expectedAmount (Integer): 이번 달 결제 예정액 (오늘~말일)
              - yearlyAmount (Integer): 연 금액 (올해 1~12월 결제 합계, 등록 전 기간 포함)
              - subscriptionCount (Integer): 구독 중 개수
              - logos (List<String>): 구독 로고 URL (최대 10개, 결제일 가까운 순, 직접 입력 구독 제외)
            - upcomingPayments (List): 구독 점검하기 목록 (최대 5개)
              - subscriptionId (UUID), serviceName (String), logoUrl (String, nullable: 직접 입력 구독), amount (Integer)
              - nextBillingDate (String): 다음 결제일 (yyyy-MM-dd)
              - dDay (Integer): 남은 일수
              - needsCheckup (Boolean): 이번 결제 주기 점검 미완료 여부
            - usage (Object): 잘 안 쓰는 구독
              - hasCheckup (Boolean): 점검 기록 존재 여부
              - nudgeSubscriptionId (UUID, nullable): 점검 유도 대상 구독 ID
              - unusedCount (Integer): 잘 안 쓰는 구독 개수
              - monthlyAmount (Integer): 대상 구독 전체의 이번 달 결제 금액 합계
              - yearlyAmount (Integer): 대상 구독 전체의 연 금액
              - items (List): 미리보기 최대 3개 — subscriptionId, serviceName, logoUrl, amount, scorePercent (최근 점검 활용도 %)
            - hasUnreadNotification (Boolean): 읽지 않은 알림 여부
            """
    )
    @GetMapping
    public CommonResponse<HomeResponse> getHome(@CurrentUserId UUID userId) {
        HomeResponse response = homeUseCase.getHome(userId);
        return CommonResponse.success(HomeResponseCode.HOME_FOUND, response);
    }
}
