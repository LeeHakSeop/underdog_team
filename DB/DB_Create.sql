CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE TABLE carrier (
    carrier_id BIGSERIAL PRIMARY KEY,
    carrier_name VARCHAR(100) NOT NULL,
    carrier_contact VARCHAR(100),
    manager_name VARCHAR(100),
    carrier_status VARCHAR(30),
    user_id BIGINT REFERENCES users(user_id)
);

CREATE TABLE driver (
    driver_id BIGSERIAL PRIMARY KEY,
    driver_name VARCHAR(100),
    driver_contact VARCHAR(100),
    is_registered BOOLEAN DEFAULT FALSE,
    carrier_id BIGINT REFERENCES carrier(carrier_id),
    can_enter BOOLEAN DEFAULT FALSE,
    user_id BIGINT REFERENCES users(user_id)
);

CREATE TABLE vehicle (
    vehicle_id BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(30) UNIQUE,
    vehicle_type VARCHAR(30),
    tonnage VARCHAR(30),
    is_registered BOOLEAN DEFAULT FALSE,
    vehicle_status VARCHAR(30),
    tractor_no VARCHAR(30),
    chassis_no VARCHAR(30),
    carrier_id BIGINT REFERENCES carrier(carrier_id),
    driver_id BIGINT REFERENCES driver(driver_id),
    user_id BIGINT REFERENCES users(user_id)
);

CREATE TABLE tractor (
    vehicle_id BIGINT PRIMARY KEY REFERENCES vehicle(vehicle_id) ON DELETE CASCADE
);

CREATE TABLE trailer (
    vehicle_id BIGINT PRIMARY KEY REFERENCES vehicle(vehicle_id) ON DELETE CASCADE
);

CREATE TABLE yard_sector (
    sector_id BIGSERIAL PRIMARY KEY,
    sector_name VARCHAR(50),
    block_name VARCHAR(30),
    sector_status VARCHAR(30),
    capacity INTEGER NOT NULL DEFAULT 40,
    waiting_vehicle_count INTEGER DEFAULT 0,
    guide_message VARCHAR(255),
    alt_waiting_area VARCHAR(50),
    environment_type VARCHAR(20),
    CONSTRAINT ck_yard_sector_environment_type
        CHECK (
            environment_type IS NULL
            OR environment_type IN ('GENERAL', 'HEAVY', 'REEFER', 'DANGEROUS', 'EMPTY')
        )
);

CREATE TABLE container (
    container_id BIGSERIAL PRIMARY KEY,
    container_number VARCHAR(30) UNIQUE,
    container_size VARCHAR(20),
    container_location VARCHAR(100),
    sector_id BIGINT REFERENCES yard_sector(sector_id),
    block VARCHAR(30),
    bay VARCHAR(30),
    row_no VARCHAR(30),
    can_exit BOOLEAN DEFAULT TRUE,
    seal_number VARCHAR(50),
    shipping_line VARCHAR(100)
);

CREATE TABLE work_order (
    work_order_id BIGSERIAL PRIMARY KEY,
    work_type VARCHAR(30),
    vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    tractor_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    trailer_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    driver_id BIGINT REFERENCES driver(driver_id),
    container_id BIGINT REFERENCES container(container_id),
    start_sector_id BIGINT REFERENCES yard_sector(sector_id),
    destination_sector_id BIGINT REFERENCES yard_sector(sector_id),
    reserved_time TIMESTAMP,
    work_status VARCHAR(30),
    is_approved BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_work_order_start_sector_id
    ON work_order(start_sector_id);

CREATE INDEX IF NOT EXISTS idx_work_order_destination_sector_id
    ON work_order(destination_sector_id);

CREATE TABLE work_status_history (
    history_id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT REFERENCES work_order(work_order_id),
    prev_status VARCHAR(30),
    new_status VARCHAR(30),
    changed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100),
    reason VARCHAR(255),
    remark VARCHAR(255)
);

CREATE TABLE gate_log (
    gate_log_id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    tractor_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    trailer_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    gate_number VARCHAR(20),
    gate_name VARCHAR(50),
    entry_time TIMESTAMP,
    exit_time TIMESTAMP,
    in_out_type VARCHAR(20),
    process_result VARCHAR(30),
    manager_check BOOLEAN DEFAULT FALSE
);

CREATE TABLE exception_log (
    exception_log_id BIGSERIAL PRIMARY KEY,
    gate_log_id BIGINT REFERENCES gate_log(gate_log_id),
    vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    plate_number VARCHAR(30),
    exception_type VARCHAR(50),
    exception_message VARCHAR(255),
    occurred_time TIMESTAMP,
    process_status VARCHAR(30),
    manager_action VARCHAR(255),
    processed_time TIMESTAMP
);

CREATE TABLE plate_recognition (
    plate_recognition_id BIGSERIAL PRIMARY KEY,
    gate_log_id BIGINT REFERENCES gate_log(gate_log_id),
    vehicle_image VARCHAR(255),
    recognized_plate VARCHAR(30),
    plate_type VARCHAR(20),
    is_success BOOLEAN,
    confidence DECIMAL(5,2),
    manual_correction VARCHAR(30),
    error_message VARCHAR(255),
    recognition_time TIMESTAMP
);

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
    anomaly_count INTEGER,
    current_fault_probability NUMERIC(12, 10),
    abnormal_sensors JSONB NOT NULL DEFAULT '[]'::JSONB,
    event_message VARCHAR(500),
    source_type VARCHAR(20) NOT NULL DEFAULT 'MODEL',
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
