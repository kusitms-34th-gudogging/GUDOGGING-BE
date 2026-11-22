package com.subscribers.gudogging.domain.gmail.presentation;

import com.subscribers.gudogging.domain.gmail.application.dto.request.ConnectGmailRequest;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailAnalysisResponse;
import com.subscribers.gudogging.domain.gmail.application.dto.response.GmailConnectionResponse;
import com.subscribers.gudogging.domain.gmail.application.usecase.GmailUseCase;
import com.subscribers.gudogging.domain.gmail.presentation.response.GmailResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Gmail", description = "Gmail 연동 관련 API")
@RestController
@RequestMapping("/api/v1/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailUseCase gmailUseCase;

    @Operation(
        summary = "Gmail 연동",
        description = """
            **기능**
            Google 계정 선택 · Gmail 읽기 권한 동의 후 앱이 받은 서버용 인증 코드로 Gmail을 연동합니다 (A-11, E-02).
            로그인 계정과 다른 Google 계정이어도 됩니다. 이미 연동돼 있으면 기존 연결을 해제하고 새 계정으로 교체합니다.
            - 앱은 웹 클라이언트 ID로 gmail.readonly · email 범위의 서버용 인증 코드를 받아야 합니다 (한 번만 사용 가능, 몇 분 안에 만료)
            - 인증 코드가 만료 · 이미 사용 · 잘못됨이면 40801(400)
            - Gmail 읽기 권한에 동의하지 않았거나 refresh token · 계정 정보가 없으면 40802(400)
            - Google 서버 오류 · 타임아웃이면 40804(502)
            - 회원이 없거나 탈퇴했으면 40201(404)
            - 실패하면 앱은 E-04(A-11에서는 A-12e)를 표시합니다

            **요청값**
            - authCode (String, 필수): Google 서버용 인증 코드

            **응답값**
            - status (String): CONNECTED
            - connected (Boolean): true
            - email (String): 연동된 Gmail 주소
            """
    )
    @PostMapping("/connection")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<GmailConnectionResponse> connectGmail(@CurrentUserId UUID userId,
                                                                @RequestBody @Valid ConnectGmailRequest request) {
        GmailConnectionResponse response = gmailUseCase.connectGmail(userId, request);
        return CommonResponse.success(GmailResponseCode.GMAIL_CONNECTED, response);
    }

    @Operation(
        summary = "Gmail 연동 상태 조회",
        description = """
            **기능**
            Gmail 연동 여부와 연동된 Gmail 주소를 조회합니다. 미연동이면 E-02, 연동이면 E-03을 표시합니다.
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)

            **응답값**
            - status (String): NOT_CONNECTED / CONNECTED
            - connected (Boolean): status가 CONNECTED일 때 true
            - email (String, nullable): 연동된 Gmail 주소, 미연동이면 null
            """
    )
    @GetMapping("/connection")
    public CommonResponse<GmailConnectionResponse> getGmailConnection(@CurrentUserId UUID userId) {
        GmailConnectionResponse response = gmailUseCase.getGmailConnection(userId);
        return CommonResponse.success(GmailResponseCode.GMAIL_CONNECTION_FOUND, response);
    }

    @Operation(
        summary = "Gmail 연동 해제",
        description = """
            **기능**
            Google에서 Gmail 읽기 권한(refresh token)을 무효화하고 연결을 삭제합니다 (E-03b '연결 해제하기').
            - 연동된 Gmail이 없으면 40803(404)
            - Google 서버 오류 · 타임아웃이면 40804(502) — 기존 연결은 그대로 유지
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)

            **응답값**
            - 없음 (data = null)
            """
    )
    @DeleteMapping("/connection")
    public CommonResponse<Void> disconnectGmail(@CurrentUserId UUID userId) {
        gmailUseCase.disconnectGmail(userId);
        return CommonResponse.success(GmailResponseCode.GMAIL_DISCONNECTED);
    }

    @Operation(
        summary = "Gmail 결제 메일 분석 (구독 후보 추출)",
        description = """
            **기능**
            연동된 Gmail에서 최근 60일 결제 · 구독 관련 메일(최대 50통)을 읽어 AI(Claude)로 구독 결제만 골라 후보를 만듭니다 (A-12 → A-13).
            서비스 목록에 있는 요금제만 후보가 되고, 이미 구독 중인 서비스는 빠지며, 같은 서비스는 가장 최근 결제 1건만 남습니다. 저장하지 않습니다.
            최종 등록은 POST /api/v1/subscriptions (source = GMAIL)를 사용합니다. 응답까지 10초 이상 걸릴 수 있습니다.
            - 후보가 없으면 200 + candidates 빈 배열 → 앱은 A-12e
            - 연동된 Gmail이 없으면 40803(404)
            - Gmail 권한이 끊겼으면 40805(409) — 다시 연결 필요
            - Gmail · AI 분석 오류 · 타임아웃이면 40806(502)
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)

            **응답값**
            - candidates (Array): 구독 후보 목록, 결제일 최신순
              - serviceId (UUID): 서비스 ID (항상 있음)
              - serviceName (String): 서비스명 (요금제)
              - logoUrl (String, nullable): 로고 URL
              - category (String): 카테고리
              - amount (Integer, nullable): 결제 금액(원), 못 읽으면 null
              - billingCycleValue (Integer): 결제 주기 숫자
              - billingCycleUnit (String): DAY / WEEK / MONTH / YEAR
              - nextBillingDate (Date, nullable): 다음 결제일 yyyy-MM-dd, 못 정하면 null
              - paymentMethod (String): APP_STORE / GOOGLE_PLAY / WEB_DIRECT
              - needsReview (Boolean): 확인 필요 여부 (A-13 '확인 필요' 묶음)
              - reviewFields (Array): A-15g 오류 문구를 띄울 필드 (AMOUNT / BILLING_CYCLE / NEXT_BILLING_DATE)
            """
    )
    @PostMapping("/analyses")
    public CommonResponse<GmailAnalysisResponse> analyzeGmailPayments(@CurrentUserId UUID userId) {
        GmailAnalysisResponse response = gmailUseCase.analyzeGmailPayments(userId);
        return CommonResponse.success(GmailResponseCode.GMAIL_ANALYZED, response);
    }
}
