-- exception_log 테이블을 현재 백엔드 ExceptionLogMapper 형식에 맞춘다.
-- 기존 행은 삭제하지 않고 컬럼명과 누락 컬럼만 보정한다.

CREATE TABLE IF NOT EXISTS exception_log (
    exception_log_id BIGSERIAL PRIMARY KEY,
    gate_log_id BIGINT,
    vehicle_id BIGINT,
    plate_number VARCHAR(30),
    exception_type VARCHAR(50),
    exception_message VARCHAR(255),
    occurred_time TIMESTAMP,
    process_status VARCHAR(30),
    manager_action VARCHAR(255),
    processed_time TIMESTAMP
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'exception_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'exception_log_id'
    ) THEN
        ALTER TABLE exception_log RENAME COLUMN exception_id TO exception_log_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'occured_time'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'occurred_time'
    ) THEN
        ALTER TABLE exception_log RENAME COLUMN occured_time TO occurred_time;
    END IF;
END $$;

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS exception_log_id BIGSERIAL,
    ADD COLUMN IF NOT EXISTS gate_log_id BIGINT,
    ADD COLUMN IF NOT EXISTS vehicle_id BIGINT,
    ADD COLUMN IF NOT EXISTS plate_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS exception_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS exception_message VARCHAR(255),
    ADD COLUMN IF NOT EXISTS occurred_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS process_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS manager_action VARCHAR(255),
    ADD COLUMN IF NOT EXISTS processed_time TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.exception_log'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE exception_log
            ADD CONSTRAINT pk_exception_log PRIMARY KEY (exception_log_id);
    END IF;
END $$;
