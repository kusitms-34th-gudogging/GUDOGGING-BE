package com.subscribers.gudogging.domain.auth.presentation;

import com.subscribers.gudogging.domain.auth.application.dto.request.AppleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.GoogleLoginRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.LogoutRequest;
import com.subscribers.gudogging.domain.auth.application.dto.request.ReissueTokenRequest;
import com.subscribers.gudogging.domain.auth.application.dto.response.LoginResponse;
import com.subscribers.gudogging.domain.auth.application.dto.response.TokenResponse;
import com.subscribers.gudogging.domain.auth.application.usecase.AuthUseCase;
import com.subscribers.gudogging.domain.auth.presentation.response.AuthResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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

    @Operation(
        summary = "애플 로그인",
        description = """
            **기능**
            앱이 Sign in with Apple로 받은 identityToken과 authorizationCode로 로그인합니다. 처음 로그인한 Apple 계정이면 자동으로 가입합니다.
            authorizationCode는 1회용이고 발급 후 5분 동안만 유효하므로 받은 즉시 호출해야 합니다.
            identityToken이 유효하지 않으면 40105, authorizationCode가 만료·재사용됐거나 identityToken과 다른 사용자의 코드면 40106,
            Apple 서버와 통신하지 못하면 40107(502) 에러를 반환합니다.
            
            **요청값**
            - identityToken (String, 필수): ASAuthorizationAppleIDCredential.identityToken (JWT 문자열)
            - authorizationCode (String, 필수): ASAuthorizationAppleIDCredential.authorizationCode
            - name (String, 선택): 사용자 이름 (최대 100자). Apple은 첫 인가 때만 fullName을 주므로 그때만 보내고 이후에는 생략. 가입할 때만 저장됨
            
            **응답값**
            - accessToken (String): API 호출 시 Authorization: Bearer 헤더에 넣는 토큰 (유효기간 1시간)
            - refreshToken (String): Access Token 재발급용 토큰 (유효기간 14일)
            - isNewUser (boolean): 이번 요청으로 가입했으면 true (온보딩 화면으로 이동), 기존 회원이면 false (홈으로 이동)
            """
    )
    @PostMapping("/apple")
    public CommonResponse<LoginResponse> loginWithApple(@RequestBody @Valid AppleLoginRequest request) {
        LoginResponse response = authUseCase.loginWithApple(request);
        return CommonResponse.success(AuthResponseCode.LOGIN_SUCCESS, response);
    }

    @Operation(
        summary = "토큰 재발급",
        description = """
            **기능**
            Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다. Access Token이 만료됐을 때(40103) 호출합니다.
            재발급하면 이전 Refresh Token은 무효가 되므로, 응답의 refreshToken으로 교체해 저장해야 합니다.
            Refresh Token이 만료됐거나 이미 사용한 토큰이면 40102 에러를 반환합니다. 이 경우 다시 로그인해야 합니다.
            
            **요청값**
            - refreshToken (String, 필수): 로그인 또는 이전 재발급 응답으로 받은 Refresh Token
            
            **응답값**
            - accessToken (String): 새 Access Token (유효기간 1시간)
            - refreshToken (String): 새 Refresh Token (유효기간 14일)
            """
    )
    @PostMapping("/reissue")
    public CommonResponse<TokenResponse> reissueToken(@RequestBody @Valid ReissueTokenRequest request) {
        TokenResponse response = authUseCase.reissueToken(request);
        return CommonResponse.success(AuthResponseCode.TOKEN_REISSUED, response);
    }

    @Operation(
        summary = "로그아웃",
        description = """
            **기능**
            현재 로그인 세션을 종료합니다. 로그아웃 확인 팝업(E-06)에서 '로그아웃 하기'를 누르면 호출합니다.
            요청한 refreshToken이 서버에 저장된 토큰과 같으면 무효화해, 이 토큰으로는 더 이상 재발급할 수 없습니다.
            이미 만료됐거나 다른 기기 로그인·재발급으로 교체된 토큰이어도 이미 무효이므로 성공(200)을 반환합니다.
            Access Token은 서버에 저장하지 않으므로, 성공하면 앱에서 accessToken·refreshToken을 모두 삭제하고 로그인 화면으로 이동합니다.
            Authorization 헤더(Bearer accessToken)가 필요하며, Access Token이 만료됐으면 40103 에러를 반환합니다.

            **요청값**
            - refreshToken (String, 필수): 무효화할 Refresh Token (이 기기에 저장된 값)

            **응답값**
            - 없음 (data = null)
            """
    )
    @PostMapping("/logout")
    public CommonResponse<Void> logoutUser(
        @CurrentUserId UUID userId,
        @RequestBody @Valid LogoutRequest request
    ) {
        authUseCase.logoutUser(userId, request);
        return CommonResponse.success(AuthResponseCode.LOGOUT_SUCCESS);
    }
}
