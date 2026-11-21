package com.subscribers.gudogging.domain.user.presentation;

import com.subscribers.gudogging.domain.user.application.dto.response.UserAccountResponse;
import com.subscribers.gudogging.domain.user.application.usecase.UserUseCase;
import com.subscribers.gudogging.domain.user.presentation.response.UserResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import com.subscribers.gudogging.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @Operation(
        summary = "내 정보 조회",
        description = """
            **기능**
            마이페이지(E-01) 진입 시 로그인한 회원의 계정 정보를 조회합니다.
            로그인 계정 이메일과 로그인 방식('Apple로 로그인' / 'Google로 로그인')을 내려주고,
            서비스 로그인 계정과 별개인 Gmail 연동 여부 · 연동 계정을 따로 내려줍니다.
            'Gmail 연동' 메뉴는 gmailLinked가 false면 미연동 화면(E-02), true면 연동 완료 화면(E-03)으로 이동합니다.
            프로필 이미지는 내려주지 않습니다 (앱 기본 이미지).
            - 회원이 없거나 탈퇴했으면 40201(404)

            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)

            **응답값**
            - userId (UUID): 회원 ID
            - provider (String): 로그인 방식 (APPLE, GOOGLE)
            - email (String): 로그인 계정 이메일. Apple 이메일 숨김 등으로 없으면 null
            - gmailLinked (Boolean): Gmail 결제 메일 연동 여부
            - gmailEmail (String): 연동된 Gmail 주소. 미연동이면 null
            """
    )
    @GetMapping("/me")
    public CommonResponse<UserAccountResponse> getUserAccount(@CurrentUserId UUID userId) {
        UserAccountResponse response = userUseCase.getUserAccount(userId);
        return CommonResponse.success(UserResponseCode.USER_ACCOUNT_FOUND, response);
    }

    @Operation(
        summary = "회원 탈퇴",
        description = """
            **기능**
            로그인한 회원을 탈퇴 처리합니다. 회원 정보는 익명화되고, 발급된 Refresh Token은 더 이상 재발급에 쓸 수 없습니다.
            Apple 회원이면 Apple 계정 연동도 해제합니다(revoke). Apple 서버와 통신하지 못하면 40107(502) 에러를 반환하며,
            이때 탈퇴는 처리되지 않으므로 다시 시도하면 됩니다.
            이미 탈퇴했거나 존재하지 않는 회원이면 40201(404) 에러를 반환합니다.
            
            **요청값**
            - 없음 (Authorization: Bearer Access Token 필요)
            
            **응답값**
            - 없음 (data = null)
            """
    )
    @DeleteMapping("/me")
    public CommonResponse<Void> withdrawUser(@CurrentUserId UUID userId) {
        userUseCase.withdrawUser(userId);
        return CommonResponse.success(UserResponseCode.USER_WITHDRAWN);
    }
}
