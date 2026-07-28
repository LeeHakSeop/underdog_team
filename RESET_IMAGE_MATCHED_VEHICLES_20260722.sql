-- 첨부된 시연 이미지 파일명의 번호판 중 DB에 실제 존재하는 차량만 제거한다.
-- 기사, 운송사, 사용자 및 대상과 무관한 차량/운영 기록은 유지한다.
-- 실행 전 영향 범위는 차량 18대, 작업 오더 7건, 게이트 기록 69건으로 확인했다.

BEGIN;

CREATE TABLE backup_vehicle_20260722 AS
SELECT *
FROM vehicle
WHERE plate_number IN (
    '006나9693', '006나9800', '006너9792', '006더6213', '006더6588', '006라9732',
    '01러2135', '04나6186', '04누8494', '04마4718', '04무0141', '06어9700',
    '09더8068', '09소5694', '09어3423', '09어4741', '175나2236', '184러1328'
);

CREATE TABLE backup_work_order_20260722 AS
SELECT DISTINCT w.*
FROM work_order w
JOIN backup_vehicle_20260722 v
  ON w.vehicle_id = v.vehicle_id
  OR w.tractor_vehicle_id = v.vehicle_id
  OR w.trailer_vehicle_id = v.vehicle_id;

CREATE TABLE backup_work_status_history_20260722 AS
SELECT h.*
FROM work_status_history h
JOIN backup_work_order_20260722 w ON w.work_order_id = h.work_order_id;

CREATE TABLE backup_gate_log_20260722 AS
SELECT DISTINCT g.*
FROM gate_log g
JOIN backup_vehicle_20260722 v
  ON g.vehicle_id = v.vehicle_id
  OR g.tractor_vehicle_id = v.vehicle_id
  OR g.trailer_vehicle_id = v.vehicle_id;

CREATE TABLE backup_plate_recognition_20260722 AS
SELECT p.*
FROM plate_recognition p
JOIN backup_gate_log_20260722 g ON g.gate_log_id = p.gate_log_id;

CREATE TABLE backup_exception_log_20260722 AS
SELECT DISTINCT e.*
FROM exception_log e
LEFT JOIN backup_vehicle_20260722 v ON v.vehicle_id = e.vehicle_id
LEFT JOIN backup_gate_log_20260722 g ON g.gate_log_id = e.gate_log_id
WHERE v.vehicle_id IS NOT NULL
   OR g.gate_log_id IS NOT NULL
   OR e.plate_number IN (SELECT plate_number FROM backup_vehicle_20260722);

DELETE FROM plate_recognition
WHERE plate_recognition_id IN (
    SELECT plate_recognition_id FROM backup_plate_recognition_20260722
);

DELETE FROM exception_log
WHERE exception_log_id IN (
    SELECT exception_log_id FROM backup_exception_log_20260722
);

DELETE FROM work_status_history
WHERE history_id IN (
    SELECT history_id FROM backup_work_status_history_20260722
);

DELETE FROM gate_log
WHERE gate_log_id IN (
    SELECT gate_log_id FROM backup_gate_log_20260722
);

DELETE FROM work_order
WHERE work_order_id IN (
    SELECT work_order_id FROM backup_work_order_20260722
);

DELETE FROM vehicle
WHERE vehicle_id IN (
    SELECT vehicle_id FROM backup_vehicle_20260722
);

COMMIT;

-- 실행 결과 확인
SELECT 'deleted_vehicle_backup' AS item, COUNT(*) AS count FROM backup_vehicle_20260722
UNION ALL SELECT 'deleted_work_order_backup', COUNT(*) FROM backup_work_order_20260722
UNION ALL SELECT 'deleted_gate_log_backup', COUNT(*) FROM backup_gate_log_20260722
UNION ALL SELECT 'deleted_plate_recognition_backup', COUNT(*) FROM backup_plate_recognition_20260722
UNION ALL SELECT 'deleted_exception_log_backup', COUNT(*) FROM backup_exception_log_20260722
UNION ALL SELECT 'remaining_vehicle', COUNT(*) FROM vehicle;
