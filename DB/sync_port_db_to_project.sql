-- 기존 port_db를 현재 1차 프로젝트 DB_Create.sql 구조에 맞춘다.
-- 데이터는 보존하고, 누락된 기본값/제약조건을 추가하며 오타 열을 정리한다.

BEGIN;

-- users
ALTER TABLE users
    ALTER COLUMN user_name SET NOT NULL;

-- 기본값 복구
ALTER TABLE driver
    ALTER COLUMN is_registered SET DEFAULT FALSE,
    ALTER COLUMN can_enter SET DEFAULT FALSE;

ALTER TABLE vehicle
    ALTER COLUMN is_registered SET DEFAULT FALSE;

ALTER TABLE yard_sector
    ALTER COLUMN sector_name TYPE VARCHAR(50),
    ALTER COLUMN alt_waiting_area TYPE VARCHAR(50),
    ALTER COLUMN waiting_vehicle_count SET DEFAULT 0;

ALTER TABLE container
    ALTER COLUMN can_exit SET DEFAULT TRUE;

ALTER TABLE work_order
    ALTER COLUMN is_approved SET DEFAULT FALSE;

ALTER TABLE work_status_history
    ALTER COLUMN changed_time SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE gate_log
    ALTER COLUMN manager_check SET DEFAULT FALSE;

-- exception_log의 과거 오타 열 데이터를 정상 열로 이동한다.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'occurred_time'
    ) THEN
        ALTER TABLE exception_log
            ADD COLUMN occurred_time TIMESTAMP;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'occured_time'
    ) THEN
        UPDATE exception_log
        SET occurred_time = COALESCE(occurred_time, occured_time)
        WHERE occurred_time IS NULL;
    END IF;
END $$;

-- 누락된 ID 시퀀스와 기본키를 복구한다.
CREATE SEQUENCE IF NOT EXISTS exception_log_exception_log_id_seq;
ALTER SEQUENCE exception_log_exception_log_id_seq
    OWNED BY exception_log.exception_log_id;

ALTER TABLE exception_log
    ALTER COLUMN exception_log_id
        SET DEFAULT nextval('exception_log_exception_log_id_seq');

UPDATE exception_log
SET exception_log_id = nextval('exception_log_exception_log_id_seq')
WHERE exception_log_id IS NULL;

SELECT setval(
    'exception_log_exception_log_id_seq',
    GREATEST(COALESCE((SELECT MAX(exception_log_id) FROM exception_log), 1), 1),
    TRUE
);

ALTER TABLE exception_log
    ALTER COLUMN exception_log_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'exception_log'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE exception_log
            ADD CONSTRAINT exception_log_pkey PRIMARY KEY (exception_log_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'exception_log'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[
              (SELECT attnum
               FROM pg_attribute
               WHERE attrelid = 'exception_log'::regclass
                 AND attname = 'vehicle_id')
          ]::SMALLINT[]
    ) THEN
        ALTER TABLE exception_log
            ADD CONSTRAINT exception_log_vehicle_id_fkey
            FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id);
    END IF;
END $$;

ALTER TABLE exception_log
    DROP COLUMN IF EXISTS occured_time;

COMMIT;
