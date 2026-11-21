-- 이슈 #43 푸시 알림 — 배포 전에 개발 · 운영 DB에 직접 실행
-- 운영(ddl-auto: validate)은 platform 컬럼이 없으면 앱이 뜨지 않음
-- 개발(ddl-auto: update)은 platform 컬럼만 자동 추가되고, 기본값 변경 · 기존 회원 값 변경은 자동 반영되지 않음

-- 토큰 등록 때 기기 종류(IOS / ANDROID) 저장, 로그아웃 · 탈퇴 시 NULL
-- Hibernate가 @Enumerated(STRING)을 MySQL ENUM으로 만들므로(기존 social_provider · type 컬럼과 동일) validate 통과하도록 ENUM으로 추가
ALTER TABLE users ADD COLUMN platform ENUM('ANDROID','IOS') NULL;

-- 결제 3일 전 점검 알림(D-3) 수신 여부 기본값 OFF (노션 알림 설정 조회 기준)
ALTER TABLE users ALTER COLUMN is_check_notification_enabled SET DEFAULT FALSE;
-- 기존 회원은 설정 API가 없어 모두 예전 기본값 true — 새 기본값 OFF로 맞춤
UPDATE users SET is_check_notification_enabled = FALSE;
