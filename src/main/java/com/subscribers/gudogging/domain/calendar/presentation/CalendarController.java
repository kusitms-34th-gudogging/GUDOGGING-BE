package com.subscribers.gudogging.domain.calendar.presentation;

import com.subscribers.gudogging.domain.calendar.application.dto.response.CalendarResponse;
import com.subscribers.gudogging.domain.calendar.application.usecase.CalendarUseCase;
import com.subscribers.gudogging.domain.calendar.presentation.response.CalendarResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calendar", description = "결제 캘린더 관련 API")
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarUseCase calendarUseCase;

    @Operation(
        summary = "월별 결제 캘린더 조회",
        description = """
            **기능**
            선택한 연 · 월의 결제 예정 일정을 날짜별로 조회합니다 (C-01, C-02). 날짜를 누르면 이 응답의 날짜별 결제 목록을 사용합니다.
            - 결제일은 다음 결제일과 결제 주기로 계산합니다. 월말 결제일은 짧은 달에 말일로 계산합니다 (1/31 → 2/28 → 3/31).
            - 이용 시작일 전 결제일은 내려주지 않고, 해지한 구독은 해지일 당일 결제까지만 내려줍니다.
            - 결제 정보를 수정한 구독은 수정 전 결제일에 수정 전 금액 · 주기를 내려줍니다.
            - 같은 날 결제는 금액 낮은 순(같으면 서비스명 순)입니다. 날짜 칸 로고 2개 + '+n'은 이 순서로 표시합니다.
            - currentMonthTotal('이번 달 결제 합계' 배지)은 오늘이 속한 달 1일~말일 합계로, 다른 달을 조회해도 바뀌지 않습니다.
            - 날짜를 눌러 들어가는 구독 상세(C-03)는 구독 상세 조회에 baseDate를 보내 사용합니다.
            - year가 2024~2028, month가 1~12가 아니면 40601(400)
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - year (Integer, 필수): 조회 연도 2024~2028 (query parameter)
            - month (Integer, 필수): 조회 월 1~12 (query parameter)

            **응답값**
            - currentMonthTotal (Integer): 오늘이 속한 달의 결제 합계 (원, 조회 월과 무관)
            - days (List): 결제가 있는 날짜 목록 (날짜순)
              - date (String): 날짜 (yyyy-MM-dd)
              - totalAmount (Integer): 해당일 결제 합계 (건수는 payments 길이)
              - payments (List): 해당일 결제 목록
                - subscriptionId (UUID): 구독 ID
                - serviceName (String): 서비스명
                - logoUrl (String, nullable): 서비스 로고. 직접 입력한 구독은 null
                - amount (Integer): 결제 금액 (원)
                - billingCycleValue (Integer): 결제 주기 숫자
                - billingCycleUnit (String): DAY / WEEK / MONTH / YEAR
                - paymentMethod (String): 결제 경로 APP_STORE / GOOGLE_PLAY / WEB_DIRECT
            """
    )
    @GetMapping
    public CommonResponse<CalendarResponse> getMonthlyCalendar(
        @CurrentUserId UUID userId,
        @RequestParam int year,
        @RequestParam int month
    ) {
        CalendarResponse response = calendarUseCase.getMonthlyCalendar(userId, year, month);
        return CommonResponse.success(CalendarResponseCode.CALENDAR_FOUND, response);
    }
}
