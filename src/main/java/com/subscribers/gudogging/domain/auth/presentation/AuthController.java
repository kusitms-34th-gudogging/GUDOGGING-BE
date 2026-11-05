package com.subscribers.gudogging.domain.auth.presentation;

import com.subscribers.gudogging.domain.auth.application.dto.request.GoogleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.response.LoginResponse;
import com.subscribers.gudogging.domain.auth.application.usecase.AuthUseCase;
import com.subscribers.gudogging.domain.auth.presentation.response.AuthResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(
        summary = "구글 로그인",
        description = """
            **기능**
            앱이 Google Sign-In SDK로 받은 ID Token을 검증해 로그인합니다. 처음 로그인한 Google 계정이면 자동으로 가입합니다.

            **요청값**
            - idToken (String, 필수): Google Sign-In SDK로 받은 ID Token

            **응답값**
            - accessToken (String): API 호출 시 Authorization: Bearer 헤더에 넣는 토큰 (유효기간 1시간)
            - refreshToken (String): Access Token 재발급용 토큰 (유효기간 14일)
            - isNewUser (boolean): 이번 요청으로 가입했으면 true (온보딩 화면으로 이동), 기존 회원이면 false (홈으로 이동)

            **로컬 테스트 방법** (앱 없이 ID Token 발급)
            1. [OAuth 2.0 Playground](https://developers.google.com/oauthplayground) 접속 → 우측 상단 톱니바퀴 → "Use your own OAuth credentials" 체크 → 서버 `GOOGLE_CLIENT_IDS`에 등록한 Web 클라이언트 ID·Secret 입력
            2. Step 1 입력칸에 `openid email profile` 입력 → Authorize APIs → Google 로그인
            3. Step 2에서 Exchange authorization code for tokens → 응답의 `id_token` 값 복사
            4. 이 API의 idToken에 붙여넣고 실행 (`id_token`은 1시간 후 만료되므로 만료 시 3번 재실행)
            5. 응답의 accessToken을 Swagger 우측 상단 Authorize에 입력하면 인증이 필요한 API 호출 가능
            """
    )
    @PostMapping("/google")
    public CommonResponse<LoginResponse> loginWithGoogle(@RequestBody @Valid GoogleLoginRequest request) {
        LoginResponse response = authUseCase.loginWithGoogle(request);
        return CommonResponse.success(AuthResponseCode.LOGIN_SUCCESS, response);
    }
}
