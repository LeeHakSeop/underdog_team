ALTER TABLE yard_sector
ADD COLUMN IF NOT EXISTS capacity INTEGER NOT NULL DEFAULT 40;

UPDATE yard_sector
SET capacity = 40
WHERE capacity IS NULL;

INSERT INTO users (
    login_id,
    password,
    user_name,
    role_code,
    status
)
VALUES (
    'admin',
    '1234',
    '시스템 관리자',
    'ADMIN',
    'ACTIVE'
)
ON CONFLICT (login_id) DO UPDATE SET
    user_name = EXCLUDED.user_name,
    role_code = EXCLUDED.role_code,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;
