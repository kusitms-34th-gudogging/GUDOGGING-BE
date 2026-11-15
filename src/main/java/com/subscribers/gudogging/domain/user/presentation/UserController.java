package com.subscribers.gudogging.domain.user.presentation;

import com.subscribers.gudogging.domain.user.application.usecase.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

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
            - 없음 (204 No Content)
            """
    )
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawUser(@AuthenticationPrincipal UUID userId) {
        userUseCase.withdrawUser(userId);
    }
}
