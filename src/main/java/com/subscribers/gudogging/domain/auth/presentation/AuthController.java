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
            """
    )
    @PostMapping("/google")
    public CommonResponse<LoginResponse> loginWithGoogle(@RequestBody @Valid GoogleLoginRequest request) {
        LoginResponse response = authUseCase.loginWithGoogle(request);
        return CommonResponse.success(AuthResponseCode.LOGIN_SUCCESS, response);
    }
}
