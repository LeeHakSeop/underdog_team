-- 카카오 OAuth 다중 계정 토큰 저장소 (PostgreSQL)
-- Spring 백엔드가 시작될 때 자동 생성하지만, DB 계정에 CREATE 권한이 없으면
-- DB 관리자 계정으로 이 파일을 한 번 적용한다.

CREATE TABLE IF NOT EXISTS kakao_oauth_connection (
    user_id VARCHAR(100) PRIMARY KEY,
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT NOT NULL,
    access_expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
