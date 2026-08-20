-- 기존 ANT-001~024를 프로젝트용 항만 운영장비 24대로 전환한다.
-- pm_sensor_data와 pm_event는 equipment_id 숫자 FK를 사용하므로 기록 연결은 유지된다.
WITH mapping(old_code, new_code, new_name, new_type, new_location) AS (VALUES
    ('ANT-001', 'GAT-001', '게이트 자동인식 장치 01', 'GATE_RECOGNITION', 'GATE-01'),
    ('ANT-002', 'GAT-002', '게이트 자동인식 장치 02', 'GATE_RECOGNITION', 'GATE-02'),
    ('ANT-003', 'GAT-003', '게이트 자동인식 장치 03', 'GATE_RECOGNITION', 'GATE-03'),
    ('ANT-004', 'GAT-004', '게이트 자동인식 장치 04', 'GATE_RECOGNITION', 'GATE-04'),
    ('ANT-005', 'QC-001', '안벽 컨테이너 크레인 제어장치 01', 'QUAY_CRANE', 'QUAY-01'),
    ('ANT-006', 'QC-002', '안벽 컨테이너 크레인 제어장치 02', 'QUAY_CRANE', 'QUAY-02'),
    ('ANT-007', 'QC-003', '안벽 컨테이너 크레인 제어장치 03', 'QUAY_CRANE', 'QUAY-03'),
    ('ANT-008', 'QC-004', '안벽 컨테이너 크레인 제어장치 04', 'QUAY_CRANE', 'QUAY-04'),
    ('ANT-009', 'QC-005', '안벽 컨테이너 크레인 제어장치 05', 'QUAY_CRANE', 'QUAY-05'),
    ('ANT-010', 'QC-006', '안벽 컨테이너 크레인 제어장치 06', 'QUAY_CRANE', 'QUAY-06'),
    ('ANT-011', 'QC-007', '안벽 컨테이너 크레인 제어장치 07', 'QUAY_CRANE', 'QUAY-07'),
    ('ANT-012', 'QC-008', '안벽 컨테이너 크레인 제어장치 08', 'QUAY_CRANE', 'QUAY-08'),
    ('ANT-013', 'TC-001', '트랜스퍼 크레인 제어장치 01', 'TRANSFER_CRANE', 'YARD-TC-01'),
    ('ANT-014', 'TC-002', '트랜스퍼 크레인 제어장치 02', 'TRANSFER_CRANE', 'YARD-TC-02'),
    ('ANT-015', 'TC-003', '트랜스퍼 크레인 제어장치 03', 'TRANSFER_CRANE', 'YARD-TC-03'),
    ('ANT-016', 'TC-004', '트랜스퍼 크레인 제어장치 04', 'TRANSFER_CRANE', 'YARD-TC-04'),
    ('ANT-017', 'TC-005', '트랜스퍼 크레인 제어장치 05', 'TRANSFER_CRANE', 'YARD-TC-05'),
    ('ANT-018', 'TC-006', '트랜스퍼 크레인 제어장치 06', 'TRANSFER_CRANE', 'YARD-TC-06'),
    ('ANT-019', 'TC-007', '트랜스퍼 크레인 제어장치 07', 'TRANSFER_CRANE', 'YARD-TC-07'),
    ('ANT-020', 'TC-008', '트랜스퍼 크레인 제어장치 08', 'TRANSFER_CRANE', 'YARD-TC-08'),
    ('ANT-021', 'YT-001', '야드 트랙터 운행 제어장치 01', 'YARD_TRACTOR', 'YARD-YT-01'),
    ('ANT-022', 'YT-002', '야드 트랙터 운행 제어장치 02', 'YARD_TRACTOR', 'YARD-YT-02'),
    ('ANT-023', 'YT-003', '야드 트랙터 운행 제어장치 03', 'YARD_TRACTOR', 'YARD-YT-03'),
    ('ANT-024', 'YT-004', '야드 트랙터 운행 제어장치 04', 'YARD_TRACTOR', 'YARD-YT-04')
)
UPDATE pm_equipment equipment
SET equipment_code = mapping.new_code,
    equipment_name = mapping.new_name,
    equipment_type = mapping.new_type,
    location_code = mapping.new_location,
    updated_at = CURRENT_TIMESTAMP
FROM mapping
WHERE equipment.equipment_code = mapping.old_code;

ALTER TABLE pm_equipment ALTER COLUMN equipment_type SET DEFAULT 'PORT_EQUIPMENT';
