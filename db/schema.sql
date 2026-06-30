CREATE SCHEMA IF NOT EXISTS port_gate;

SET search_path TO port_gate;

CREATE TABLE IF NOT EXISTS roles (
    role_code VARCHAR(20) PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    status CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_roles_status CHECK (status IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role_code VARCHAR(20) NOT NULL,
    status CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_roles
        FOREIGN KEY (role_code) REFERENCES roles(role_code),
    CONSTRAINT chk_users_status CHECK (status IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS carriers (
    carrier_id BIGSERIAL PRIMARY KEY,
    carrier_name VARCHAR(100) NOT NULL,
    business_no VARCHAR(20) NOT NULL UNIQUE,
    phone VARCHAR(20),
    manager_name VARCHAR(50),
    address VARCHAR(200),
    approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    status CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_carriers_approval_status
        CHECK (approval_status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT chk_carriers_status CHECK (status IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id BIGSERIAL PRIMARY KEY,
    vehicle_no VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(30) NOT NULL,
    tonnage NUMERIC(5, 2),
    is_registered CHAR(1) NOT NULL DEFAULT 'Y',
    vehicle_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    tractor_no VARCHAR(30),
    chassis_no VARCHAR(30),
    carrier_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicles_carriers
        FOREIGN KEY (carrier_id) REFERENCES carriers(carrier_id),
    CONSTRAINT chk_vehicles_is_registered CHECK (is_registered IN ('Y', 'N')),
    CONSTRAINT chk_vehicles_status
        CHECK (vehicle_status IN ('NORMAL', 'SUSPENDED', 'RESTRICTED'))
);

CREATE TABLE IF NOT EXISTS drivers (
    driver_id BIGSERIAL PRIMARY KEY,
    driver_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    is_registered CHAR(1) NOT NULL DEFAULT 'Y',
    carrier_id BIGINT NOT NULL,
    can_enter CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drivers_carriers
        FOREIGN KEY (carrier_id) REFERENCES carriers(carrier_id),
    CONSTRAINT uq_drivers_name_phone_carrier UNIQUE (driver_name, phone, carrier_id),
    CONSTRAINT chk_drivers_is_registered CHECK (is_registered IN ('Y', 'N')),
    CONSTRAINT chk_drivers_can_enter CHECK (can_enter IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS yard_sectors (
    sector_id BIGSERIAL PRIMARY KEY,
    sector_code VARCHAR(20) NOT NULL UNIQUE,
    sector_name VARCHAR(100) NOT NULL,
    block_name VARCHAR(30) NOT NULL,
    sector_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    waiting_vehicle_count INTEGER NOT NULL DEFAULT 0,
    guide_message VARCHAR(300),
    alternative_waiting_area VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_yard_sectors_status
        CHECK (sector_status IN ('AVAILABLE', 'CONGESTED', 'CLOSED', 'WORKING')),
    CONSTRAINT chk_yard_sectors_waiting_count CHECK (waiting_vehicle_count >= 0)
);

CREATE TABLE IF NOT EXISTS containers (
    container_id BIGSERIAL PRIMARY KEY,
    container_no VARCHAR(20) NOT NULL UNIQUE,
    size_type VARCHAR(10) NOT NULL,
    container_type VARCHAR(20) NOT NULL,
    current_location VARCHAR(100),
    sector_id BIGINT,
    block_name VARCHAR(30),
    bay VARCHAR(10),
    row_no VARCHAR(10),
    tier VARCHAR(10),
    can_release CHAR(1) NOT NULL DEFAULT 'Y',
    is_hold CHAR(1) NOT NULL DEFAULT 'N',
    seal_no VARCHAR(50),
    shipping_company VARCHAR(100),
    status CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_containers_sectors
        FOREIGN KEY (sector_id) REFERENCES yard_sectors(sector_id),
    CONSTRAINT chk_containers_size_type
        CHECK (size_type IN ('20FT', '40FT', '45FT')),
    CONSTRAINT chk_containers_type
        CHECK (container_type IN ('DRY', 'REEFER', 'DANGER', 'OT', 'TANK')),
    CONSTRAINT chk_containers_can_release CHECK (can_release IN ('Y', 'N')),
    CONSTRAINT chk_containers_is_hold CHECK (is_hold IN ('Y', 'N')),
    CONSTRAINT chk_containers_status CHECK (status IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS gates (
    gate_id BIGSERIAL PRIMARY KEY,
    gate_code VARCHAR(20) NOT NULL UNIQUE,
    gate_name VARCHAR(100) NOT NULL,
    gate_type VARCHAR(10) NOT NULL,
    status CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_gates_type CHECK (gate_type IN ('IN', 'OUT')),
    CONSTRAINT chk_gates_status CHECK (status IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS work_orders (
    order_id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE,
    work_type VARCHAR(20) NOT NULL,
    vehicle_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    carrier_id BIGINT NOT NULL,
    container_id BIGINT NOT NULL,
    sector_id BIGINT,
    gate_in_id BIGINT,
    gate_out_id BIGINT,
    reservation_time TIMESTAMP NOT NULL,
    work_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    is_approved CHAR(1) NOT NULL DEFAULT 'N',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_work_orders_vehicles
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    CONSTRAINT fk_work_orders_drivers
        FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
    CONSTRAINT fk_work_orders_carriers
        FOREIGN KEY (carrier_id) REFERENCES carriers(carrier_id),
    CONSTRAINT fk_work_orders_containers
        FOREIGN KEY (container_id) REFERENCES containers(container_id),
    CONSTRAINT fk_work_orders_sectors
        FOREIGN KEY (sector_id) REFERENCES yard_sectors(sector_id),
    CONSTRAINT fk_work_orders_gate_in
        FOREIGN KEY (gate_in_id) REFERENCES gates(gate_id),
    CONSTRAINT fk_work_orders_gate_out
        FOREIGN KEY (gate_out_id) REFERENCES gates(gate_id),
    CONSTRAINT fk_work_orders_approved_by
        FOREIGN KEY (approved_by) REFERENCES users(user_id),
    CONSTRAINT fk_work_orders_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT chk_work_orders_type
        CHECK (work_type IN ('LOAD', 'UNLOAD', 'INBOUND', 'OUTBOUND')),
    CONSTRAINT chk_work_orders_status
        CHECK (work_status IN ('WAITING', 'GATE_IN', 'MOVING', 'LOADING_DONE', 'GATE_OUT', 'CANCELLED')),
    CONSTRAINT chk_work_orders_is_approved CHECK (is_approved IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS gate_logs (
    gate_log_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    gate_id BIGINT NOT NULL,
    vehicle_id BIGINT,
    in_out_type VARCHAR(10) NOT NULL,
    gate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_result VARCHAR(30) NOT NULL,
    need_admin_check CHAR(1) NOT NULL DEFAULT 'N',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gate_logs_orders
        FOREIGN KEY (order_id) REFERENCES work_orders(order_id),
    CONSTRAINT fk_gate_logs_gates
        FOREIGN KEY (gate_id) REFERENCES gates(gate_id),
    CONSTRAINT fk_gate_logs_vehicles
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    CONSTRAINT chk_gate_logs_in_out_type CHECK (in_out_type IN ('IN', 'OUT')),
    CONSTRAINT chk_gate_logs_process_result
        CHECK (process_result IN ('APPROVED', 'REJECTED', 'ADMIN_CHECK', 'FAILED')),
    CONSTRAINT chk_gate_logs_admin_check CHECK (need_admin_check IN ('Y', 'N'))
);

CREATE TABLE IF NOT EXISTS plate_recognitions (
    recognition_id BIGSERIAL PRIMARY KEY,
    gate_log_id BIGINT,
    gate_id BIGINT NOT NULL,
    vehicle_image_path VARCHAR(300),
    plate_image_path VARCHAR(300),
    recognized_vehicle_no VARCHAR(20),
    is_success CHAR(1) NOT NULL DEFAULT 'N',
    confidence NUMERIC(5, 2),
    corrected_vehicle_no VARCHAR(20),
    error_message VARCHAR(300),
    recognized_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plate_recognitions_gate_logs
        FOREIGN KEY (gate_log_id) REFERENCES gate_logs(gate_log_id),
    CONSTRAINT fk_plate_recognitions_gates
        FOREIGN KEY (gate_id) REFERENCES gates(gate_id),
    CONSTRAINT chk_plate_recognitions_success CHECK (is_success IN ('Y', 'N')),
    CONSTRAINT chk_plate_recognitions_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 100))
);

CREATE TABLE IF NOT EXISTS work_status_histories (
    history_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(20),
    changed_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT,
    change_reason VARCHAR(200),
    memo VARCHAR(300),
    CONSTRAINT fk_work_status_histories_orders
        FOREIGN KEY (order_id) REFERENCES work_orders(order_id),
    CONSTRAINT fk_work_status_histories_users
        FOREIGN KEY (changed_by) REFERENCES users(user_id),
    CONSTRAINT chk_work_status_histories_previous_status
        CHECK (previous_status IS NULL OR previous_status IN ('WAITING', 'GATE_IN', 'MOVING', 'LOADING_DONE', 'GATE_OUT', 'CANCELLED')),
    CONSTRAINT chk_work_status_histories_changed_status
        CHECK (changed_status IN ('WAITING', 'GATE_IN', 'MOVING', 'LOADING_DONE', 'GATE_OUT', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS exception_logs (
    exception_id BIGSERIAL PRIMARY KEY,
    exception_type VARCHAR(50) NOT NULL,
    exception_message VARCHAR(300) NOT NULL,
    vehicle_no VARCHAR(20),
    order_id BIGINT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_status VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED',
    admin_action VARCHAR(300),
    processed_by BIGINT,
    processed_at TIMESTAMP,
    CONSTRAINT fk_exception_logs_orders
        FOREIGN KEY (order_id) REFERENCES work_orders(order_id),
    CONSTRAINT fk_exception_logs_users
        FOREIGN KEY (processed_by) REFERENCES users(user_id),
    CONSTRAINT chk_exception_logs_type
        CHECK (exception_type IN ('PLATE_RECOGNITION_FAILED', 'UNREGISTERED_VEHICLE', 'NO_WORK_ORDER', 'CONTAINER_HOLD', 'SECTOR_CONGESTED', 'CONTAINER_MISMATCH')),
    CONSTRAINT chk_exception_logs_process_status
        CHECK (process_status IN ('UNRESOLVED', 'PROCESSING', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_users_role_code ON users(role_code);
CREATE INDEX IF NOT EXISTS idx_vehicles_vehicle_no ON vehicles(vehicle_no);
CREATE INDEX IF NOT EXISTS idx_vehicles_carrier_id ON vehicles(carrier_id);
CREATE INDEX IF NOT EXISTS idx_drivers_carrier_id ON drivers(carrier_id);
CREATE INDEX IF NOT EXISTS idx_containers_container_no ON containers(container_no);
CREATE INDEX IF NOT EXISTS idx_containers_sector_id ON containers(sector_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_vehicle_id ON work_orders(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_driver_id ON work_orders(driver_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_container_id ON work_orders(container_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_status ON work_orders(work_status);
CREATE INDEX IF NOT EXISTS idx_work_orders_reservation_time ON work_orders(reservation_time);
CREATE INDEX IF NOT EXISTS idx_gate_logs_order_id ON gate_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_gate_logs_gate_id ON gate_logs(gate_id);
CREATE INDEX IF NOT EXISTS idx_plate_recognitions_vehicle_no ON plate_recognitions(recognized_vehicle_no);
CREATE INDEX IF NOT EXISTS idx_work_status_histories_order_id ON work_status_histories(order_id);
CREATE INDEX IF NOT EXISTS idx_exception_logs_order_id ON exception_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_exception_logs_vehicle_no ON exception_logs(vehicle_no);
