-- users 테이블 생성
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO users (
    user_id,
    login_id,
    password,
    user_name,
    role_code,
    status,
    created_at
)
VALUES
(
    1,
    'admin',
    '1234',
    '관리자',
    'ADMIN',
    'ACTIVE',
    '2026-07-02 18:17:50.604773'
),
(
    2,
    'carrier01',
    '1234',
    '한진물류',
    'CARRIER',
    'ACTIVE',
    '2026-07-02 20:34:05.070939'
);

CREATE SEQUENCE users_user_id_seq;

ALTER TABLE users
ALTER COLUMN user_id
SET DEFAULT nextval('users_user_id_seq');

SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));