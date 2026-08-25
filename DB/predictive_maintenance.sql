-- 항만 운영장비 예지보전 스키마 (PostgreSQL)
-- 운영 데이터 3개 테이블과 카카오 OAuth 다중 연결정보 테이블로 구성한다.

-- 카카오 OAuth 토큰은 서버 암호화 후 계정별로 저장한다.
CREATE TABLE IF NOT EXISTS kakao_oauth_connection (
    user_id VARCHAR(100) PRIMARY KEY,
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT NOT NULL,
    access_expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 현재 프로젝트에서는 CSV를 pm_sensor_data에 적재하고, 판정 결과로 pm_event를 생성한다.

CREATE TABLE IF NOT EXISTS pm_equipment (
    equipment_id BIGSERIAL PRIMARY KEY,
    equipment_code VARCHAR(50) NOT NULL UNIQUE,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(30) NOT NULL DEFAULT 'PORT_EQUIPMENT',
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
    -- 고장 전 전조가 실제 고장으로 진행될 가능성. 공식 경보를 취소하지 않는 보조값이다.
    progression_probability NUMERIC(12, 10),
    progression_model VARCHAR(100),
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
    CONSTRAINT ck_pm_sensor_progression_probability CHECK (
        progression_probability IS NULL
        OR progression_probability BETWEEN 0 AND 1
    ),
    CONSTRAINT ck_pm_sensor_source
        CHECK (source_type IN ('CSV', 'API', 'DEVICE', 'DEMO'))
);

-- 이미 생성된 DB에도 보조 모델 열을 안전하게 추가한다.
ALTER TABLE pm_sensor_data
    ADD COLUMN IF NOT EXISTS progression_probability NUMERIC(12, 10),
    ADD COLUMN IF NOT EXISTS progression_model VARCHAR(100);

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

-- 기존 안테나 코드로 구축된 DB라면 센서·이벤트 FK를 유지한 채 새 장비 코드로 전환한다.
-- psql의 현재 스크립트 위치 기준으로 실행되므로 다른 컴퓨터에서도 같은 폴더 구조를 사용한다.
\ir 20260819_reframe_predictive_equipment.sql

-- 프로젝트에서 관찰하는 항만 운영장비 24대의 기본정보.
-- 같은 코드를 다시 실행해도 중복 생성되지 않는다.
INSERT INTO pm_equipment (
    equipment_code,
    equipment_name,
    equipment_type,
    location_code
)
SELECT * FROM (VALUES
    ('GAT-001', '게이트 자동인식 장치 01', 'GATE_RECOGNITION', 'GATE-01'),
    ('GAT-002', '게이트 자동인식 장치 02', 'GATE_RECOGNITION', 'GATE-02'),
    ('GAT-003', '게이트 자동인식 장치 03', 'GATE_RECOGNITION', 'GATE-03'),
    ('GAT-004', '게이트 자동인식 장치 04', 'GATE_RECOGNITION', 'GATE-04'),
    ('QC-001', '안벽 컨테이너 크레인 제어장치 01', 'QUAY_CRANE', 'QUAY-01'),
    ('QC-002', '안벽 컨테이너 크레인 제어장치 02', 'QUAY_CRANE', 'QUAY-02'),
    ('QC-003', '안벽 컨테이너 크레인 제어장치 03', 'QUAY_CRANE', 'QUAY-03'),
    ('QC-004', '안벽 컨테이너 크레인 제어장치 04', 'QUAY_CRANE', 'QUAY-04'),
    ('QC-005', '안벽 컨테이너 크레인 제어장치 05', 'QUAY_CRANE', 'QUAY-05'),
    ('QC-006', '안벽 컨테이너 크레인 제어장치 06', 'QUAY_CRANE', 'QUAY-06'),
    ('QC-007', '안벽 컨테이너 크레인 제어장치 07', 'QUAY_CRANE', 'QUAY-07'),
    ('QC-008', '안벽 컨테이너 크레인 제어장치 08', 'QUAY_CRANE', 'QUAY-08'),
    ('TC-001', '트랜스퍼 크레인 제어장치 01', 'TRANSFER_CRANE', 'YARD-TC-01'),
    ('TC-002', '트랜스퍼 크레인 제어장치 02', 'TRANSFER_CRANE', 'YARD-TC-02'),
    ('TC-003', '트랜스퍼 크레인 제어장치 03', 'TRANSFER_CRANE', 'YARD-TC-03'),
    ('TC-004', '트랜스퍼 크레인 제어장치 04', 'TRANSFER_CRANE', 'YARD-TC-04'),
    ('TC-005', '트랜스퍼 크레인 제어장치 05', 'TRANSFER_CRANE', 'YARD-TC-05'),
    ('TC-006', '트랜스퍼 크레인 제어장치 06', 'TRANSFER_CRANE', 'YARD-TC-06'),
    ('TC-007', '트랜스퍼 크레인 제어장치 07', 'TRANSFER_CRANE', 'YARD-TC-07'),
    ('TC-008', '트랜스퍼 크레인 제어장치 08', 'TRANSFER_CRANE', 'YARD-TC-08'),
    ('YT-001', '야드 트랙터 운행 제어장치 01', 'YARD_TRACTOR', 'YARD-YT-01'),
    ('YT-002', '야드 트랙터 운행 제어장치 02', 'YARD_TRACTOR', 'YARD-YT-02'),
    ('YT-003', '야드 트랙터 운행 제어장치 03', 'YARD_TRACTOR', 'YARD-YT-03'),
    ('YT-004', '야드 트랙터 운행 제어장치 04', 'YARD_TRACTOR', 'YARD-YT-04')
) AS seed(equipment_code, equipment_name, equipment_type, location_code)
ON CONFLICT (equipment_code) DO UPDATE SET
    equipment_name = EXCLUDED.equipment_name,
    equipment_type = EXCLUDED.equipment_type,
    location_code = EXCLUDED.location_code,
    updated_at = CURRENT_TIMESTAMP;

-- 관계 확인용 조회 예시
-- SELECT e.equipment_code, s.collected_at, s.success_rate, s.operational_state
-- FROM pm_sensor_data s
-- JOIN pm_equipment e ON e.equipment_id = s.equipment_id
-- WHERE e.equipment_code = 'TC-006'
-- ORDER BY s.collected_at;

-- 점검·정비 화면 조회 예시
-- SELECT e.equipment_code, v.event_type, v.occurred_at, v.notification_status
-- FROM pm_event v
-- JOIN pm_equipment e ON e.equipment_id = v.equipment_id
-- ORDER BY v.occurred_at DESC;
