-- 작업 지시 테이블
-- 작업 상태 코드는 다음 7개만 사용합니다.
-- DISPATCH_WAITING, APPROVED, GATE_IN, IN_PROGRESS, COMPLETED, GATE_OUT, CANCELED
-- 시나리오 데이터는 20260710_seed_scenario_data.sql에서 관리합니다.
CREATE TABLE IF NOT EXISTS work_order(
    work_order_id BIGSERIAL PRIMARY KEY,
    work_type VARCHAR(30),
    vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    tractor_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    trailer_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
    driver_id BIGINT REFERENCES driver(driver_id),
    container_id BIGINT REFERENCES container(container_id),
    reserved_time TIMESTAMP,
    work_status VARCHAR(30),
    is_approved BOOLEAN
);
