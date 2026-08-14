-- 안테나 예지보전 최소 스키마 (PostgreSQL)
-- 구성: 장비 1개 + 센서 시계열 1개 + 사건/알림 이력 1개 = 총 3개 테이블
-- 현재 프로젝트에서는 CSV를 pm_sensor_data에 적재하고, 판정 결과로 pm_event를 생성한다.

CREATE TABLE IF NOT EXISTS pm_equipment (
    equipment_id BIGSERIAL PRIMARY KEY,
    equipment_code VARCHAR(50) NOT NULL UNIQUE,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(30) NOT NULL DEFAULT 'ANTENNA',
    location_code VARCHAR(50),
    operation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    installed_at DATE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pm_equipment_status
        CHECK (operation_status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE', 'RETIRED'))
);

CREATE TABLE IF NOT EXISTS pm_sensor_data (
    sensor_data_id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES pm_equipment(equipment_id),
    collected_at TIMESTAMP NOT NULL,

    -- CSV의 실제 센서값
    traffic_load NUMERIC(10, 4),
    temperature_c NUMERIC(10, 3),
    voltage_v NUMERIC(10, 3),
    signal_strength_dbm NUMERIC(10, 3),
    success_rate NUMERIC(10, 4),
    response_time_ms NUMERIC(12, 3),
    retry_count INTEGER,
    disconnect_count INTEGER,
    packet_loss_rate NUMERIC(10, 4),
    error_count INTEGER,
    days_since_maintenance INTEGER,

    -- 같은 시점의 모델/운영정책 판정값
    current_fault_probability NUMERIC(12, 10),
    anomaly_count INTEGER NOT NULL DEFAULT 0,
    abnormal_sensors JSONB NOT NULL DEFAULT '[]'::JSONB,
    operational_state VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    current_failure BOOLEAN NOT NULL DEFAULT FALSE,
    precursor_entry_condition BOOLEAN NOT NULL DEFAULT FALSE,
    needs_attention BOOLEAN NOT NULL DEFAULT FALSE,
    risk_score NUMERIC(7, 3),
    risk_level VARCHAR(30),

    source_type VARCHAR(20) NOT NULL DEFAULT 'CSV',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pm_sensor_equipment_time UNIQUE (equipment_id, collected_at),
    CONSTRAINT ck_pm_sensor_state CHECK (
        operational_state IN (
            'NORMAL', 'SUSPECT', 'RISK', 'FAILURE_EXPECTED',
            'FAILURE', 'POST_FAILURE_RECOVERY'
        )
    ),
    CONSTRAINT ck_pm_sensor_probability CHECK (
        current_fault_probability IS NULL
        OR current_fault_probability BETWEEN 0 AND 1
    ),
    CONSTRAINT ck_pm_sensor_source
        CHECK (source_type IN ('CSV', 'API', 'DEVICE', 'DEMO'))
);

CREATE TABLE IF NOT EXISTS pm_event (
    event_id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES pm_equipment(equipment_id),
    sensor_data_id BIGINT REFERENCES pm_sensor_data(sensor_data_id),
    event_type VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,

    -- 사건 당시 화면에 표시할 판정 근거의 요약
    anomaly_count INTEGER,
    current_fault_probability NUMERIC(12, 10),
    abnormal_sensors JSONB NOT NULL DEFAULT '[]'::JSONB,
    event_message VARCHAR(500),
    source_type VARCHAR(20) NOT NULL DEFAULT 'MODEL',

    -- 카카오 알림 이력도 이 테이블에 함께 저장한다.
    notification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED',
    notification_requested_at TIMESTAMP,
    notification_sent_at TIMESTAMP,
    notification_failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_pm_event_equipment_type_time
        UNIQUE (equipment_id, event_type, occurred_at),
    CONSTRAINT ck_pm_event_type CHECK (
        event_type IN (
            'FAILURE_EXPECTED', 'FAILURE',
            'MAINTENANCE_COMPLETED', 'RECOVERY'
        )
    ),
    CONSTRAINT ck_pm_event_source
        CHECK (source_type IN ('MODEL', 'DATASET', 'ADMIN', 'DEMO')),
    CONSTRAINT ck_pm_event_notification CHECK (
        notification_status IN (
            'NOT_REQUESTED', 'PENDING', 'SENT', 'FAILED', 'DEMO_NOT_SENT'
        )
    ),
    CONSTRAINT ck_pm_event_probability CHECK (
        current_fault_probability IS NULL
        OR current_fault_probability BETWEEN 0 AND 1
    )
);

CREATE INDEX IF NOT EXISTS idx_pm_sensor_equipment_time
    ON pm_sensor_data(equipment_id, collected_at DESC);

CREATE INDEX IF NOT EXISTS idx_pm_event_equipment_time
    ON pm_event(equipment_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_pm_event_type_time
    ON pm_event(event_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_pm_event_notification
    ON pm_event(notification_status, occurred_at)
    WHERE notification_status IN ('PENDING', 'FAILED');

-- 현재 V3 CSV의 24개 안테나 기본정보.
-- 같은 코드를 다시 실행해도 중복 생성되지 않는다.
INSERT INTO pm_equipment (
    equipment_code,
    equipment_name,
    equipment_type,
    location_code
)
SELECT
    'ANT-' || LPAD(number::TEXT, 3, '0'),
    '안테나 ' || LPAD(number::TEXT, 3, '0'),
    'ANTENNA',
    'TG-' || LPAD((((number - 1) / 4) + 1)::TEXT, 2, '0')
FROM generate_series(1, 24) AS number
ON CONFLICT (equipment_code) DO UPDATE SET
    equipment_name = EXCLUDED.equipment_name,
    equipment_type = EXCLUDED.equipment_type,
    location_code = EXCLUDED.location_code,
    updated_at = CURRENT_TIMESTAMP;

-- 관계 확인용 조회 예시
-- SELECT e.equipment_code, s.collected_at, s.success_rate, s.operational_state
-- FROM pm_sensor_data s
-- JOIN pm_equipment e ON e.equipment_id = s.equipment_id
-- WHERE e.equipment_code = 'ANT-018'
-- ORDER BY s.collected_at;

-- 점검·정비 화면 조회 예시
-- SELECT e.equipment_code, v.event_type, v.occurred_at, v.notification_status
-- FROM pm_event v
-- JOIN pm_equipment e ON e.equipment_id = v.equipment_id
-- ORDER BY v.occurred_at DESC;
