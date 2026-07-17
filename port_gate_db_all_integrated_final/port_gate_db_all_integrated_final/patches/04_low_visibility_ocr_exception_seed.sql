-- ============================================================
-- 20260715_low_visibility_trailer_exception_seed.sql
-- 실제 vehicle에 등록된 train(4).txt 트레일러 중 OCR 저신뢰 사례를
-- plate_recognition 및 exception_log에 추가합니다.
--
-- 주의: train(4).txt에는 이미지 경로와 정답 번호판만 있고 이미지 픽셀은
-- 포함되어 있지 않으므로, 실제 밝기를 측정한 결과가 아니라 운영 테스트용
-- 저조도/역광/흐림 시나리오입니다.
-- ============================================================
BEGIN;

-- 1) 2026-07-15 게이트 로그 중 실제 트레일러 15건을 저신뢰 OCR 사례로 보정
WITH candidates AS (
    SELECT p.plate_recognition_id,
           p.gate_log_id,
           g.trailer_vehicle_id,
           v.plate_number,
           ROW_NUMBER() OVER (ORDER BY p.plate_recognition_id) AS rn
    FROM plate_recognition p
    JOIN gate_log g ON g.gate_log_id = p.gate_log_id
    JOIN vehicle v ON v.vehicle_id = g.trailer_vehicle_id
    WHERE g.entry_time::date = DATE '2026-07-15'
      AND v.vehicle_type IN ('트레일러','TRAILER')
    ORDER BY p.plate_recognition_id
    LIMIT 15
)
UPDATE plate_recognition p
SET is_success = FALSE,
    confidence = CASE
        WHEN c.rn % 3 = 1 THEN 42.80
        WHEN c.rn % 3 = 2 THEN 58.60
        ELSE 67.40
    END,
    manual_correction = c.plate_number,
    error_message = CASE
        WHEN c.rn % 3 = 1 THEN '저조도 이미지로 번호판 문자 식별 실패'
        WHEN c.rn % 3 = 2 THEN '역광 및 반사로 OCR 신뢰도 기준 미달'
        ELSE '흐림·기울어짐으로 일부 문자 오인식'
    END,
    plate_type = 'TRAILER'
FROM candidates c
WHERE p.plate_recognition_id = c.plate_recognition_id;

-- 2) 위 저신뢰 OCR 사례를 exception_log에 추가
WITH candidates AS (
    SELECT p.gate_log_id,
           g.trailer_vehicle_id AS vehicle_id,
           v.plate_number,
           g.entry_time,
           p.confidence,
           p.error_message,
           ROW_NUMBER() OVER (ORDER BY p.plate_recognition_id) AS rn
    FROM plate_recognition p
    JOIN gate_log g ON g.gate_log_id = p.gate_log_id
    JOIN vehicle v ON v.vehicle_id = g.trailer_vehicle_id
    WHERE g.entry_time::date = DATE '2026-07-15'
      AND p.is_success = FALSE
      AND p.error_message IN (
          '저조도 이미지로 번호판 문자 식별 실패',
          '역광 및 반사로 OCR 신뢰도 기준 미달',
          '흐림·기울어짐으로 일부 문자 오인식'
      )
)
INSERT INTO exception_log
(gate_log_id, exception_type, exception_message, plate_number,
 occurred_time, process_status, manager_action, processed_time)
SELECT c.gate_log_id,
       CASE
         WHEN c.error_message LIKE '저조도%' THEN '저조도 번호판 인식 실패'
         WHEN c.error_message LIKE '역광%' THEN '역광 번호판 인식 실패'
         ELSE '번호판 저신뢰 인식'
       END,
       c.error_message || ' (신뢰도 ' || c.confidence || '%)',
       c.plate_number,
       c.entry_time + INTERVAL '6 seconds',
       CASE WHEN c.rn % 4 = 0 THEN '처리완료' ELSE '미처리' END,
       CASE WHEN c.rn % 4 = 0 THEN '관리자 육안 확인 후 번호판 수동 보정' ELSE NULL END,
       CASE WHEN c.rn % 4 = 0 THEN c.entry_time + INTERVAL '4 minutes' ELSE NULL END
FROM candidates c
WHERE NOT EXISTS (
    SELECT 1
    FROM exception_log e
    WHERE e.gate_log_id = c.gate_log_id
      AND e.plate_number = c.plate_number
      AND e.exception_type IN ('저조도 번호판 인식 실패','역광 번호판 인식 실패','번호판 저신뢰 인식')
);

COMMIT;
