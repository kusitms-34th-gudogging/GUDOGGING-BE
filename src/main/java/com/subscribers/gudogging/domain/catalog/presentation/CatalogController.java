package com.subscribers.gudogging.domain.catalog.presentation;

import com.subscribers.gudogging.domain.catalog.application.dto.response.ServiceListResponse;
import com.subscribers.gudogging.domain.catalog.application.usecase.CatalogUseCase;
import com.subscribers.gudogging.domain.catalog.presentation.response.CatalogResponseCode;
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

@Tag(name = "Catalog", description = "서비스 목록 관련 API")
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogUseCase catalogUseCase;

    @Operation(
        summary = "직접 등록 서비스 목록 조회·검색",
        description = """
            **기능**
            구독 직접 등록 화면(A-02)에서 고를 수 있는 사전 등록 서비스 목록을 조회합니다.
            로그인한 회원이 이미 구독 중(ACTIVE)인 서비스는 제외하며, 구독 종료(ENDED)된 서비스는 다시 노출합니다.
            검색어가 없거나 공백이면 전체 목록을 노출 순서(sort_order)대로 반환합니다.
            검색어가 있으면 서비스명·영문명·별칭 부분 일치, 한글 초성(예: ㄴㅍ), 카테고리명으로 매칭하고
            정확 일치 > 앞부분 일치 > 부분 일치 > 초성 > 카테고리명 순으로 정렬합니다. 대소문자와 공백은 무시합니다.
            매칭되는 서비스가 없으면 services에 빈 배열을 반환합니다.
            회원이 없거나 탈퇴했으면 40201(404) 에러를 반환합니다.
            
            **요청값**
            - keyword (String, 선택): 검색어 (query parameter)
            
            **응답값**
            - services (List): 서비스 목록 (없으면 빈 배열)
              - serviceId (UUID): 서비스 ID — 구독 일괄 등록 요청에 사용
              - serviceName (String): 서비스명
              - category (String): 카테고리 코드 (VIDEO_OTT, MUSIC, SHOPPING_DELIVERY, EDUCATION, PRODUCTIVITY_AI, ENTERTAINMENT, ETC)
              - logoUrl (String, nullable): 서비스 로고 URL. null이면 기본 이미지 사용
              - defaultAmount (Integer, nullable): 사전 등록 요금제 기본 금액 (원). 구독 정보 입력(A-03) 결제 금액 기본값. 없으면 null
            """
    )
    @GetMapping
    public CommonResponse<ServiceListResponse> getServices(
        @CurrentUserId UUID userId,
        @RequestParam(required = false) String keyword
    ) {
        ServiceListResponse response = catalogUseCase.getServices(userId, keyword);
        return CommonResponse.success(CatalogResponseCode.SERVICES_FOUND, response);
    }
}
