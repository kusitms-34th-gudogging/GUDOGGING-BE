-- 이슈 #43 푸시 알림 — 운영 DB(ddl-auto: validate)에 배포 전 직접 실행
-- 개발 DB는 ddl-auto: update라 배포 시 자동 반영

-- 토큰 등록 때 기기 종류(IOS / ANDROID) 저장, 로그아웃 · 탈퇴 시 NULL
-- Hibernate가 @Enumerated(STRING)을 MySQL ENUM으로 만들므로(기존 social_provider · type 컬럼과 동일) validate 통과하도록 ENUM으로 추가
ALTER TABLE users ADD COLUMN platform ENUM('ANDROID','IOS') NULL;
