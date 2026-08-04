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

CREATE TABLE yard_sector (
    sector_id BIGSERIAL PRIMARY KEY,
    sector_name VARCHAR(50),
    block_name VARCHAR(30),
    sector_status VARCHAR(30),
    waiting_vehicle_count INTEGER DEFAULT 0,
    guide_message VARCHAR(255),
    alt_waiting_area VARCHAR(50)
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
    reserved_time TIMESTAMP,
    work_status VARCHAR(30),
    is_approved BOOLEAN DEFAULT FALSE
);

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