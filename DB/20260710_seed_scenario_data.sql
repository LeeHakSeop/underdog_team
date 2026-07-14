BEGIN;

DELETE FROM plate_recognition;
DELETE FROM exception_log;
DELETE FROM work_status_history;
DELETE FROM gate_log;
DELETE FROM work_order;
DELETE FROM container;
DELETE FROM yard_sector;

INSERT INTO yard_sector (
    sector_id,
    sector_name,
    block_name,
    sector_status,
    waiting_vehicle_count,
    guide_message,
    alt_waiting_area
) VALUES
(1, 'A-01', 'A블록', '사용가능', 1, 'A블록 1번 섹터로 진입하세요.', 'WAIT-A'),
(2, 'B-07', 'B블록', '작업중', 2, 'B블록 7번 섹터에서 상차 작업을 진행하세요.', 'WAIT-B'),
(3, 'C-03', 'C블록', '혼잡', 5, 'C블록 혼잡으로 대기구역 안내가 필요합니다.', 'WAIT-C'),
(4, 'D-02', 'D블록', '사용가능', 0, 'D블록 2번 섹터로 이동하세요.', 'WAIT-D');

INSERT INTO container (
    container_id,
    container_number,
    container_size,
    container_location,
    sector_id,
    block,
    bay,
    row_no,
    can_exit,
    seal_number,
    shipping_line
) VALUES
(1, 'KDTU1234567', '40FT', 'B블록 / 07베이 / 03열', 2, 'B', '07', '03', true, 'SEAL-1001', 'DRY'),
(2, 'KDTU7654321', '20FT', 'C블록 / 03베이 / 01열', 3, 'C', '03', '01', false, 'SEAL-1002', 'REEFER'),
(3, 'KDTU3321904', '40FT', 'A블록 / 01베이 / 02열', 1, 'A', '01', '02', true, 'SEAL-1003', 'DRY');

INSERT INTO work_order (
    work_order_id,
    work_type,
    vehicle_id,
    tractor_vehicle_id,
    trailer_vehicle_id,
    driver_id,
    container_id,
    reserved_time,
    work_status,
    is_approved
) VALUES
(1, '반출 상차', 301, 301, 301, 301, 1, CURRENT_TIMESTAMP + INTERVAL '1 hour', 'DISPATCH_WAITING', false),
(2, '반입 하차', 301, 301, 301, 301, 2, CURRENT_TIMESTAMP + INTERVAL '2 hour', 'IN_PROGRESS', true),
(3, '반출 상차', 301, 301, 301, 301, 3, CURRENT_TIMESTAMP + INTERVAL '3 hour', 'COMPLETED', true);

INSERT INTO gate_log (
    gate_log_id,
    vehicle_id,
    tractor_vehicle_id,
    trailer_vehicle_id,
    gate_number,
    gate_name,
    entry_time,
    exit_time,
    in_out_type,
    process_result,
    manager_check
) VALUES
(1, 301, 301, 301, 'G-01', '게이트 01', CURRENT_TIMESTAMP - INTERVAL '50 minute', NULL, 'IN', '승인', true),
(2, 301, 301, 301, 'G-02', '게이트 02', CURRENT_TIMESTAMP - INTERVAL '20 minute', NULL, 'IN', '검토', false),
(3, 301, 301, 301, 'G-03', '게이트 03', NULL, CURRENT_TIMESTAMP - INTERVAL '5 minute', 'OUT', '승인', true);

INSERT INTO plate_recognition (
    plate_recognition_id,
    gate_log_id,
    vehicle_image,
    recognized_plate,
    is_success,
    confidence,
    manual_correction,
    error_message,
    recognition_time,
    plate_type
) VALUES
(1, 1, '/images/demo_gate_01.jpg', '05우1935', true, 0.96, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '50 minute', 'TRAILER'),
(2, 2, '/images/demo_gate_02.jpg', '05우1935', true, 0.91, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '20 minute', 'TRAILER'),
(3, 3, '/images/demo_gate_03.jpg', '05우1935', true, 0.94, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '5 minute', 'TRAILER');

INSERT INTO exception_log (
    exception_type,
    exception_message,
    plate_number,
    occurred_time,
    process_status,
    manager_action,
    processed_time
) VALUES
('CONTAINER_EXIT_NOT_ALLOWED', '컨테이너 반출 승인 상태가 아닙니다.', '05우1935', CURRENT_TIMESTAMP - INTERVAL '15 minute', 'UNPROCESSED', NULL, NULL),
('WORK_ORDER_NOT_APPROVED', '작업 지시가 승인되지 않았습니다.', '05우1935', CURRENT_TIMESTAMP - INTERVAL '8 minute', 'PROCESSED', '관리자 확인 완료', CURRENT_TIMESTAMP - INTERVAL '3 minute');

SELECT setval('yard_sector_sector_id_seq', COALESCE((SELECT MAX(sector_id) FROM yard_sector), 1), true);
SELECT setval('container_container_id_seq', COALESCE((SELECT MAX(container_id) FROM container), 1), true);
SELECT setval('work_order_work_order_id_seq', COALESCE((SELECT MAX(work_order_id) FROM work_order), 1), true);
SELECT setval('gate_log_gate_log_id_seq', COALESCE((SELECT MAX(gate_log_id) FROM gate_log), 1), true);
SELECT setval('plate_recognition_plate_recognition_id_seq', COALESCE((SELECT MAX(plate_recognition_id) FROM plate_recognition), 1), true);

COMMIT;
