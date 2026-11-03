# Memory (Current Context & Status)
# Last Updated: 2026-11-03

## Project
- 구도깅(gudogging): 큐시즘 34기 구독 관리·점검 앱 백엔드
- 핵심 흐름: 구독 등록(직접/스크린샷/Gmail) → 결제 전 점검(D-3 알림) → 유지/해지 결정 → 해지 안내
- 클라이언트: 모바일 앱 (Apple/Google 소셜 로그인, FCM 푸시)

## Status
- 구현됨: global 공통 모듈 (BaseEntity, CommonResponse, ErrorCode, GlobalExceptionHandler, SecurityConfig, SwaggerConfig)
- 구현됨: example 도메인 (Post) — 계층 구조를 보여주는 예시용 API. 실제 도메인 아님. 컨벤션 판단 시 예외로 취급
- 설계 완료: docs/ERD.md v0.2 (users, gmail_connections, services, subscriptions, subscription_checks)
- 미구현: 실제 도메인 전부 (auth, user, gmail, service, subscription, subscription_check)

## Pending (ERD 수정 예정)
- ERD의 물리 삭제 설계(회원 탈퇴, Gmail 연결 해제)는 Hard Delete 금지 규칙에 맞게 수정 예정
- ERD의 Boolean 컬럼명은 `is_` 접두사 규칙에 맞게 수정 예정
- ERD 8장 "확인이 필요한 사항" 9개 미확정 (점검 점수 공식, 이용 시작일 입력 위치, 알림 시각 등)
