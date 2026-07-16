-- =====================================================================
-- 20260715_exception_and_trailer_seed_unified.sql
-- PostgreSQL / UTF-8
--
-- 통합 대상
--   1) 기존 exception_log 설계 및 예외 더미데이터 100건
--   2) train(4).txt 기반 추가 트레일러 100대와 연계 더미데이터
--
-- 특징
--   - 기존 데이터를 삭제하지 않음
--   - 재실행 가능하도록 NOT EXISTS 중심으로 구성
--   - occured_time 오타를 occurred_time으로 자동 정리
--   - 존재하지 않는 work_order_id / gate_log_id는 NULL로 안전 처리
--   - OCR 결과 저장 시 exception_log 실시간 생성 트리거 포함
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. 기존 운영 테이블 확장
-- ---------------------------------------------------------------------
ALTER TABLE vehicle
    ADD COLUMN IF NOT EXISTS driver_id BIGINT;

ALTER TABLE vehicle
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE work_order
    ADD COLUMN IF NOT EXISTS tractor_vehicle_id BIGINT;

ALTER TABLE work_order
    ADD COLUMN IF NOT EXISTS trailer_vehicle_id BIGINT;

ALTER TABLE gate_log
    ADD COLUMN IF NOT EXISTS tractor_vehicle_id BIGINT;

ALTER TABLE gate_log
    ADD COLUMN IF NOT EXISTS trailer_vehicle_id BIGINT;

ALTER TABLE plate_recognition
    ADD COLUMN IF NOT EXISTS plate_type VARCHAR(20);

-- train.txt 원본 라벨 저장 테이블.
CREATE TABLE IF NOT EXISTS trailer_plate_dataset (
    dataset_id BIGSERIAL PRIMARY KEY,
    image_path VARCHAR(255) NOT NULL UNIQUE,
    plate_number VARCHAR(30) NOT NULL,
    source_name VARCHAR(100) NOT NULL DEFAULT 'train(4).txt',
    is_operational_seed BOOLEAN NOT NULL DEFAULT FALSE,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trailer_plate_dataset_plate_number
    ON trailer_plate_dataset(plate_number);

-- ---------------------------------------------------------------------
-- 2. exception_log 통합 스키마
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exception_log (
    exception_id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT,
    gate_log_id BIGINT,
    vehicle_id BIGINT,
    exception_type VARCHAR(50) NOT NULL,
    exception_message VARCHAR(255) NOT NULL,
    plate_number VARCHAR(30),
    occurred_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_status VARCHAR(30) NOT NULL DEFAULT '미처리',
    manager_action VARCHAR(255),
    processed_time TIMESTAMP
);

-- 기존 오타 컬럼 occured_time만 있으면 표준명으로 변경.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'occured_time'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'exception_log'
          AND column_name = 'occurred_time'
    ) THEN
        ALTER TABLE exception_log
            RENAME COLUMN occured_time TO occurred_time;
    END IF;
END $$;

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS vehicle_id BIGINT;

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS plate_number VARCHAR(30);

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS occurred_time TIMESTAMP;

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS process_status VARCHAR(30);

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS manager_action VARCHAR(255);

ALTER TABLE exception_log
    ADD COLUMN IF NOT EXISTS processed_time TIMESTAMP;

UPDATE exception_log
SET occurred_time = COALESCE(occurred_time, CURRENT_TIMESTAMP)
WHERE occurred_time IS NULL;

UPDATE exception_log
SET process_status = COALESCE(process_status, '미처리')
WHERE process_status IS NULL;

ALTER TABLE exception_log
    ALTER COLUMN occurred_time SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE exception_log
    ALTER COLUMN occurred_time SET NOT NULL;

ALTER TABLE exception_log
    ALTER COLUMN process_status SET DEFAULT '미처리';

ALTER TABLE exception_log
    ALTER COLUMN process_status SET NOT NULL;

-- 기존 복합 UNIQUE는 실시간 반복 예외 저장과 충돌할 수 있으므로 제거.
ALTER TABLE exception_log
    DROP CONSTRAINT IF EXISTS uk_exception_log;

-- FK는 존재 여부를 검사한 뒤 한 번만 생성.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_exception_work_order'
    ) THEN
        ALTER TABLE exception_log
            ADD CONSTRAINT fk_exception_work_order
            FOREIGN KEY (work_order_id)
            REFERENCES work_order(work_order_id)
            ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_exception_gate_log'
    ) THEN
        ALTER TABLE exception_log
            ADD CONSTRAINT fk_exception_gate_log
            FOREIGN KEY (gate_log_id)
            REFERENCES gate_log(gate_log_id)
            ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_exception_vehicle'
    ) THEN
        ALTER TABLE exception_log
            ADD CONSTRAINT fk_exception_vehicle
            FOREIGN KEY (vehicle_id)
            REFERENCES vehicle(vehicle_id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_exception_log_status_time
    ON exception_log(process_status, occurred_time DESC);

CREATE INDEX IF NOT EXISTS idx_exception_log_gate
    ON exception_log(gate_log_id);

CREATE INDEX IF NOT EXISTS idx_exception_log_vehicle
    ON exception_log(vehicle_id);


-- ---------------------------------------------------------------------
-- 3. 기존 예외 더미데이터 100건
--    참조 ID가 실제 DB에 없으면 FK 오류 대신 NULL로 저장한다.
-- ---------------------------------------------------------------------
WITH legacy_seed (
    seed_no,
    requested_work_order_id,
    requested_gate_log_id,
    exception_type,
    exception_message,
    plate_number,
    occurred_time,
    process_status,
    manager_action,
    processed_time
) AS (
    VALUES
(1, 1, 1, '번호판 인식 실패', '번호판을 정상적으로 인식하지 못했습니다.', '부산12가1009', '2026-07-03 08:40:10'::timestamp, '처리완료', '관리자가 수동으로 번호판 입력', '2026-07-03 08:42:30'::timestamp),
(2, 2, 2, '미등록 차량', '등록되지 않은 차량입니다.', '부산99가4321', '2026-07-03 09:10:15'::timestamp, '처리완료', '출입 거부 후 운송사 확인', '2026-07-03 09:18:20'::timestamp),
(3, 3, 3, '작업오더 없음', '해당 차량의 작업오더가 존재하지 않습니다.', '부산12가1015', '2026-07-03 09:30:00'::timestamp, '처리완료', '작업오더 재등록', '2026-07-03 09:40:00'::timestamp),
(4, 4, 4, '컨테이너 불일치', '컨테이너 번호가 작업정보와 일치하지 않습니다.', '부산12가1022', '2026-07-03 10:00:00'::timestamp, '처리완료', '컨테이너 재확인', '2026-07-03 10:12:00'::timestamp),
(5, 5, 5, '게이트 통신 오류', '게이트 장비와 통신이 끊어졌습니다.', '부산12가1030', '2026-07-03 10:30:00'::timestamp, '처리중', '장비 점검 진행', NULL::timestamp),
(6, 6, 6, '번호판 훼손', '번호판 일부가 훼손되어 인식 실패', '부산12가1035', '2026-07-03 11:00:00'::timestamp, '처리완료', '관리자 육안 확인', '2026-07-03 11:05:00'::timestamp),
(7, 7, 7, '중복 입차', '이미 입차 처리된 차량입니다.', '부산12가1040', '2026-07-03 11:30:00'::timestamp, '처리완료', '중복 기록 삭제', '2026-07-03 11:37:00'::timestamp),
(8, 8, 8, '야드 포화', '배정된 야드 섹터가 포화상태입니다.', '부산12가1048', '2026-07-03 12:00:00'::timestamp, '처리완료', '대체 섹터 배정', '2026-07-03 12:10:00'::timestamp),
(9, 9, 9, '기사 인증 실패', '기사 출입 인증에 실패했습니다.', '부산12가1055', '2026-07-03 12:30:00'::timestamp, '처리완료', '신분 확인 후 승인', '2026-07-03 12:40:00'::timestamp),
(10, 10, 10, 'RFID 오류', 'RFID 태그를 읽을 수 없습니다.', '부산12가1060', '2026-07-03 13:00:00'::timestamp, '처리중', 'RFID 장비 점검', NULL::timestamp),
(11, 11, 11, '번호판 인식 실패', 'OCR 인식률이 기준 이하입니다.', '부산12가1065', '2026-07-03 13:30:00'::timestamp, '처리완료', '수동 입력 완료', '2026-07-03 13:34:00'::timestamp),
(12, 12, 12, '작업시간 초과', '예정 작업시간을 초과했습니다.', '부산12가1070', '2026-07-03 14:00:00'::timestamp, '처리완료', '작업 연장 승인', '2026-07-03 14:08:00'::timestamp),
(13, 13, 13, '컨테이너 봉인번호 불일치', 'Seal Number가 일치하지 않습니다.', '부산12가1075', '2026-07-03 14:30:00'::timestamp, '처리완료', '봉인번호 재확인', '2026-07-03 14:40:00'::timestamp),
(14, 14, 14, '차량 정보 불일치', '등록된 차량정보와 실제 차량이 다릅니다.', '부산12가1080', '2026-07-03 15:00:00'::timestamp, '처리중', '운송사 확인 중', NULL::timestamp),
(15, 15, 15, '게이트 센서 오류', '차량 감지 센서 오류 발생', '부산12가1088', '2026-07-03 15:30:00'::timestamp, '처리완료', '센서 재부팅', '2026-07-03 15:36:00'::timestamp),
(16, 16, 16, '출입 제한 차량', '출입 제한 상태의 차량입니다.', '부산12가1095', '2026-07-03 16:00:00'::timestamp, '처리완료', '출입 차단', '2026-07-03 16:02:00'::timestamp),
(17, 17, 17, '작업 승인 취소', '관리자가 작업을 취소했습니다.', '부산12가1100', '2026-07-03 16:30:00'::timestamp, '처리완료', '작업오더 종료', '2026-07-03 16:35:00'::timestamp),
(18, 18, 18, '야드 위치 오류', '배정 섹터와 실제 위치가 다릅니다.', '부산12가1107', '2026-07-03 17:00:00'::timestamp, '처리중', '현장 확인 중', NULL::timestamp),
(19, 19, 19, '번호판 인식 실패', '강한 햇빛으로 OCR 실패', '부산12가1115', '2026-07-03 17:30:00'::timestamp, '처리완료', '수동 입력 완료', '2026-07-03 17:34:00'::timestamp),
(20, 20, 20, '시스템 오류', 'DB 연결 오류가 발생했습니다.', '부산12가1120', '2026-07-03 18:00:00'::timestamp, '처리완료', '시스템 재시작', '2026-07-03 18:12:00'::timestamp),
(21, 21, 21, '번호판 인식 실패', '야간 조명으로 OCR 인식 실패', '부산12가1125', '2026-07-03 18:30:00'::timestamp, '처리완료', '수동 번호판 입력', '2026-07-03 18:34:00'::timestamp),
(22, 22, 22, '미등록 기사', '등록되지 않은 기사입니다.', '부산12가1130', '2026-07-03 19:00:00'::timestamp, '처리완료', '기사 등록 후 승인', '2026-07-03 19:08:00'::timestamp),
(23, 23, 23, '작업오더 만료', '예약 시간이 초과되었습니다.', '부산12가1135', '2026-07-03 19:30:00'::timestamp, '처리완료', '예약 재등록', '2026-07-03 19:42:00'::timestamp),
(24, 24, 24, '게이트 차단기 오류', '차단기가 열리지 않았습니다.', '부산12가1140', '2026-07-03 20:00:00'::timestamp, '처리중', '시설팀 점검', NULL::timestamp),
(25, 25, 25, '컨테이너 번호 불일치', '컨테이너 정보가 일치하지 않습니다.', '부산12가1145', '2026-07-03 20:30:00'::timestamp, '처리완료', '컨테이너 재스캔', '2026-07-03 20:39:00'::timestamp),
(26, 26, 26, '번호판 훼손', '번호판 식별이 어렵습니다.', '부산12가1150', '2026-07-03 21:00:00'::timestamp, '처리완료', '관리자 확인 후 승인', '2026-07-03 21:04:00'::timestamp),
(27, 27, 27, '중복 출차', '이미 출차 처리된 차량입니다.', '부산12가1155', '2026-07-03 21:30:00'::timestamp, '처리완료', '중복 데이터 삭제', '2026-07-03 21:36:00'::timestamp),
(28, 28, 28, '야드 혼잡', '배정 섹터 진입 불가', '부산12가1160', '2026-07-03 22:00:00'::timestamp, '처리완료', '대체 섹터 배정', '2026-07-03 22:12:00'::timestamp),
(29, 29, 29, 'RFID 태그 오류', 'RFID 태그 인식 실패', '부산12가1165', '2026-07-03 22:30:00'::timestamp, '처리중', '태그 재인식 시도', NULL::timestamp),
(30, 30, 30, '출입 제한', '출입 제한 차량입니다.', '부산12가1170', '2026-07-03 23:00:00'::timestamp, '처리완료', '출입 차단', '2026-07-03 23:02:00'::timestamp),
(31, 31, 31, '차량 정보 불일치', '등록된 트랙터 번호와 다릅니다.', '부산12가1175', '2026-07-03 23:30:00'::timestamp, '처리완료', '차량정보 수정', '2026-07-03 23:39:00'::timestamp),
(32, 32, 32, '게이트 센서 오류', '센서 감지 실패', '부산12가1180', '2026-07-04 00:00:00'::timestamp, '처리완료', '센서 재시작', '2026-07-04 00:06:00'::timestamp),
(33, 33, 33, '번호판 인식 실패', '비로 인해 번호판이 흐립니다.', '부산12가1185', '2026-07-04 00:30:00'::timestamp, '처리완료', '수동 입력', '2026-07-04 00:34:00'::timestamp),
(34, 34, 34, '작업 승인 취소', '작업 요청이 취소되었습니다.', '부산12가1190', '2026-07-04 01:00:00'::timestamp, '처리완료', '작업 종료', '2026-07-04 01:05:00'::timestamp),
(35, 35, 35, 'Seal Number 불일치', '봉인번호가 다릅니다.', '부산12가1195', '2026-07-04 01:30:00'::timestamp, '처리중', '현장 확인', NULL::timestamp),
(36, 36, 36, '시스템 응답 지연', '관제 서버 응답 지연', '부산12가1200', '2026-07-04 02:00:00'::timestamp, '처리완료', '서버 재시작', '2026-07-04 02:10:00'::timestamp),
(37, 37, 37, '야드 위치 오류', '잘못된 블록으로 이동', '부산12가1205', '2026-07-04 02:30:00'::timestamp, '처리완료', '재안내 완료', '2026-07-04 02:36:00'::timestamp),
(38, 38, 38, 'OCR 인식 오류', '문자 일부를 잘못 인식했습니다.', '부산12가1210', '2026-07-04 03:00:00'::timestamp, '처리완료', '수동 수정', '2026-07-04 03:03:00'::timestamp),
(39, 39, 39, '통신 장애', '게이트와 서버 연결 실패', '부산12가1215', '2026-07-04 03:30:00'::timestamp, '처리중', '네트워크 점검', NULL::timestamp),
(40, 40, 40, '미등록 차량', '차량 등록 정보가 존재하지 않습니다.', '부산12가1220', '2026-07-04 04:00:00'::timestamp, '처리완료', '등록 후 승인', '2026-07-04 04:08:00'::timestamp),
(41, 41, 41, '번호판 인식 실패', '역광으로 번호판 인식 실패', '부산12가1225', '2026-07-04 04:30:00'::timestamp, '처리완료', '관리자 수동 입력', '2026-07-04 04:34:00'::timestamp),
(42, 42, 42, '작업오더 없음', '유효한 작업오더가 존재하지 않습니다.', '부산12가1230', '2026-07-04 05:00:00'::timestamp, '처리완료', '작업오더 재생성', '2026-07-04 05:09:00'::timestamp),
(43, 43, 43, '게이트 장비 오류', '차단기 제어 실패', '부산12가1235', '2026-07-04 05:30:00'::timestamp, '처리중', '시설팀 출동', NULL::timestamp),
(44, 44, 44, 'RFID 인식 실패', 'RFID 태그 손상', '부산12가1240', '2026-07-04 06:00:00'::timestamp, '처리완료', '태그 교체', '2026-07-04 06:12:00'::timestamp),
(45, 45, 45, '야드 섹터 포화', '배정된 섹터가 만차입니다.', '부산12가1245', '2026-07-04 06:30:00'::timestamp, '처리완료', '대체 섹터 배정', '2026-07-04 06:40:00'::timestamp),
(46, 46, 46, '컨테이너 위치 불일치', '실제 위치와 DB 정보가 다릅니다.', '부산12가1250', '2026-07-04 07:00:00'::timestamp, '처리완료', '위치정보 수정', '2026-07-04 07:11:00'::timestamp),
(47, 47, 47, '번호판 훼손', '번호판 일부 손상', '부산12가1255', '2026-07-04 07:30:00'::timestamp, '처리완료', '육안 확인 후 승인', '2026-07-04 07:35:00'::timestamp),
(48, 48, 48, '출입 권한 없음', '출입 승인되지 않은 차량입니다.', '부산12가1260', '2026-07-04 08:00:00'::timestamp, '처리완료', '출입 차단', '2026-07-04 08:03:00'::timestamp),
(49, 49, 49, 'OCR 인식 오류', '문자를 숫자로 잘못 인식', '부산12가1265', '2026-07-04 08:30:00'::timestamp, '처리완료', '수동 수정', '2026-07-04 08:33:00'::timestamp),
(50, 50, 50, '네트워크 장애', '게이트 서버 연결 실패', '부산12가1270', '2026-07-04 09:00:00'::timestamp, '처리중', '네트워크 점검', NULL::timestamp),
(51, 51, 51, '중복 작업오더', '동일 차량 작업오더가 중복 생성', '부산12가1275', '2026-07-04 09:30:00'::timestamp, '처리완료', '중복 오더 삭제', '2026-07-04 09:39:00'::timestamp),
(52, 52, 52, '차량번호 불일치', '등록 번호와 인식 번호가 다릅니다.', '부산12가1280', '2026-07-04 10:00:00'::timestamp, '처리완료', '관리자 확인', '2026-07-04 10:05:00'::timestamp),
(53, 53, 53, '기사 인증 실패', '기사 정보 인증 실패', '부산12가1285', '2026-07-04 10:30:00'::timestamp, '처리완료', '기사 신원 확인', '2026-07-04 10:36:00'::timestamp),
(54, 54, 54, '장비 점검', '야드 크레인 점검 중', '부산12가1290', '2026-07-04 11:00:00'::timestamp, '처리중', '작업 일시 중지', NULL::timestamp),
(55, 55, 55, 'Seal Number 오류', '봉인번호가 등록 정보와 다릅니다.', '부산12가1295', '2026-07-04 11:30:00'::timestamp, '처리완료', '봉인번호 수정', '2026-07-04 11:39:00'::timestamp),
(56, 56, 56, '게이트 센서 오류', '센서 감지 불량', '부산12가1300', '2026-07-04 12:00:00'::timestamp, '처리완료', '센서 재설정', '2026-07-04 12:06:00'::timestamp),
(57, 57, 57, '시스템 오류', 'DB 조회 실패', '부산12가1108', '2026-07-04 12:30:00'::timestamp, '처리완료', 'DB 재연결', '2026-07-04 12:37:00'::timestamp),
(58, 58, 58, '작업시간 초과', '허용 작업시간을 초과했습니다.', '부산12가1116', '2026-07-04 13:00:00'::timestamp, '처리완료', '작업 연장 승인', '2026-07-04 13:08:00'::timestamp),
(59, 59, 59, '야드 위치 오류', '배정 위치와 다른 블록에 진입', '부산12가1124', '2026-07-04 13:30:00'::timestamp, '처리완료', '경로 재안내', '2026-07-04 13:36:00'::timestamp),
(60, 60, 60, '번호판 인식 실패', '카메라 렌즈 오염', '부산12가1132', '2026-07-04 14:00:00'::timestamp, '처리완료', '카메라 청소 후 수동 입력', '2026-07-04 14:07:00'::timestamp),
(61, 61, 61, '번호판 인식 실패', '강한 햇빛으로 OCR 인식 실패', '부산12가1140', '2026-07-04 14:30:00'::timestamp, '처리완료', '수동 번호판 입력', '2026-07-04 14:34:00'::timestamp),
(62, 62, 62, '미등록 차량', '등록되지 않은 차량입니다.', '부산12가1148', '2026-07-04 15:00:00'::timestamp, '처리완료', '운송사 확인 후 등록', '2026-07-04 15:09:00'::timestamp),
(63, 63, 63, '작업오더 없음', '유효한 작업오더가 없습니다.', '부산12가1156', '2026-07-04 15:30:00'::timestamp, '처리완료', '작업오더 신규 생성', '2026-07-04 15:41:00'::timestamp),
(64, 64, 64, '게이트 통신 오류', '게이트 서버 응답 없음', '부산12가1164', '2026-07-04 16:00:00'::timestamp, '처리중', '통신장비 점검', NULL::timestamp),
(65, 65, 65, 'RFID 인식 실패', 'RFID 태그 인식 오류', '부산12가1172', '2026-07-04 16:30:00'::timestamp, '처리완료', 'RFID 태그 교체', '2026-07-04 16:38:00'::timestamp),
(66, 66, 66, '컨테이너 번호 불일치', '컨테이너 번호가 일치하지 않습니다.', '부산12가1180', '2026-07-04 17:00:00'::timestamp, '처리완료', '컨테이너 재확인', '2026-07-04 17:10:00'::timestamp),
(67, 67, 67, '출입 권한 없음', '출입 승인되지 않은 차량', '부산12가1188', '2026-07-04 17:30:00'::timestamp, '처리완료', '출입 차단', '2026-07-04 17:33:00'::timestamp),
(68, 68, 68, '야드 혼잡', '배정된 섹터가 포화 상태입니다.', '부산12가1196', '2026-07-04 18:00:00'::timestamp, '처리완료', '대체 섹터 배정', '2026-07-04 18:09:00'::timestamp),
(69, 69, 69, '번호판 훼손', '번호판 식별 불가', '부산12가1204', '2026-07-04 18:30:00'::timestamp, '처리완료', '관리자 육안 확인', '2026-07-04 18:35:00'::timestamp),
(70, 70, 70, '장비 점검', '게이트 차단기 점검', '부산12가1212', '2026-07-04 19:00:00'::timestamp, '처리중', '시설팀 점검 진행', NULL::timestamp),
(71, 71, 71, 'OCR 인식 오류', '문자를 잘못 인식했습니다.', '부산12가1220', '2026-07-04 19:30:00'::timestamp, '처리완료', 'OCR 결과 수정', '2026-07-04 19:34:00'::timestamp),
(72, 72, 72, 'Seal Number 오류', '봉인번호 불일치', '부산12가1228', '2026-07-04 20:00:00'::timestamp, '처리완료', '봉인번호 재입력', '2026-07-04 20:07:00'::timestamp),
(73, 73, 73, '중복 입차', '이미 입차된 차량입니다.', '부산12가1236', '2026-07-04 20:30:00'::timestamp, '처리완료', '중복 데이터 삭제', '2026-07-04 20:38:00'::timestamp),
(74, 74, 74, '야드 위치 오류', '잘못된 블록에 진입했습니다.', '부산12가1244', '2026-07-04 21:00:00'::timestamp, '처리완료', '경로 재안내', '2026-07-04 21:06:00'::timestamp),
(75, 75, 75, '차량 정보 불일치', '등록 정보와 실제 차량이 다릅니다.', '부산12가1252', '2026-07-04 21:30:00'::timestamp, '처리완료', '차량 정보 수정', '2026-07-04 21:39:00'::timestamp),
(76, 76, 76, '시스템 오류', 'DB 조회 실패', '부산12가1260', '2026-07-04 22:00:00'::timestamp, '처리완료', 'DB 재시작', '2026-07-04 22:11:00'::timestamp),
(77, 77, 77, '기사 인증 실패', '기사 인증에 실패했습니다.', '부산12가1268', '2026-07-04 22:30:00'::timestamp, '처리완료', '신분 확인 후 승인', '2026-07-04 22:37:00'::timestamp),
(78, 78, 78, '게이트 센서 오류', '차량 감지 실패', '부산12가1276', '2026-07-04 23:00:00'::timestamp, '처리중', '센서 점검', NULL::timestamp),
(79, 79, 79, '번호판 인식 실패', '카메라 렌즈 오염', '부산12가1284', '2026-07-04 23:30:00'::timestamp, '처리완료', '렌즈 청소 후 재인식', '2026-07-04 23:36:00'::timestamp),
(80, 80, 80, '작업 승인 취소', '관리자 요청으로 작업 취소', '부산12가1292', '2026-07-05 00:00:00'::timestamp, '처리완료', '작업 종료 처리', '2026-07-05 00:05:00'::timestamp),
(81, 81, 81, '번호판 인식 실패', '비로 인해 번호판 인식 실패', '부산12가1300', '2026-07-05 00:30:00'::timestamp, '처리완료', '수동 번호판 입력', '2026-07-05 00:34:00'::timestamp),
(82, 82, 82, '미등록 차량', '차량 등록 정보가 없습니다.', '부산12가1101', '2026-07-05 01:00:00'::timestamp, '처리완료', '차량 등록 후 승인', '2026-07-05 01:08:00'::timestamp),
(83, 83, 83, '작업오더 없음', '배정된 작업오더가 존재하지 않습니다.', '부산12가1109', '2026-07-05 01:30:00'::timestamp, '처리완료', '작업오더 생성', '2026-07-05 01:39:00'::timestamp),
(84, 84, 84, '게이트 장비 오류', '차단기 동작 실패', '부산12가1117', '2026-07-05 02:00:00'::timestamp, '처리중', '시설팀 점검', NULL::timestamp),
(85, 85, 85, '컨테이너 번호 불일치', '컨테이너 번호가 시스템과 다릅니다.', '부산12가1125', '2026-07-05 02:30:00'::timestamp, '처리완료', '컨테이너 재확인', '2026-07-05 02:39:00'::timestamp),
(86, 86, 86, '번호판 훼손', '번호판 일부 손상', '부산12가1133', '2026-07-05 03:00:00'::timestamp, '처리완료', '육안 확인', '2026-07-05 03:05:00'::timestamp),
(87, 87, 87, 'RFID 인식 실패', 'RFID 태그 손상', '부산12가1141', '2026-07-05 03:30:00'::timestamp, '처리완료', 'RFID 교체', '2026-07-05 03:38:00'::timestamp),
(88, 88, 88, '출입 권한 없음', '출입 제한 차량입니다.', '부산12가1149', '2026-07-05 04:00:00'::timestamp, '처리완료', '출입 차단', '2026-07-05 04:03:00'::timestamp),
(89, 89, 89, '야드 포화', '배정 섹터 만차', '부산12가1157', '2026-07-05 04:30:00'::timestamp, '처리완료', '대체 섹터 배정', '2026-07-05 04:40:00'::timestamp),
(90, 90, 90, 'OCR 인식 오류', '문자 일부 오인식', '부산12가1165', '2026-07-05 05:00:00'::timestamp, '처리완료', 'OCR 수정', '2026-07-05 05:04:00'::timestamp),
(91, 91, 91, 'Seal Number 오류', '봉인번호 불일치', '부산12가1173', '2026-07-05 05:30:00'::timestamp, '처리완료', '봉인번호 수정', '2026-07-05 05:36:00'::timestamp),
(92, 92, 92, '중복 출차', '이미 출차 처리된 차량입니다.', '부산12가1181', '2026-07-05 06:00:00'::timestamp, '처리완료', '중복기록 삭제', '2026-07-05 06:08:00'::timestamp),
(93, 93, 93, '야드 위치 오류', '실제 위치와 DB 정보 불일치', '부산12가1189', '2026-07-05 06:30:00'::timestamp, '처리완료', '위치정보 수정', '2026-07-05 06:38:00'::timestamp),
(94, 94, 94, '시스템 오류', 'DB 연결 실패', '부산12가1197', '2026-07-05 07:00:00'::timestamp, '처리완료', 'DB 재시작', '2026-07-05 07:09:00'::timestamp),
(95, 95, 95, '게이트 센서 오류', '센서 감지 실패', '부산12가1205', '2026-07-05 07:30:00'::timestamp, '처리중', '센서 교체 예정', NULL::timestamp),
(96, 96, 96, '기사 인증 실패', '기사 신원 확인 실패', '부산12가1213', '2026-07-05 08:00:00'::timestamp, '처리완료', '신분증 확인 후 승인', '2026-07-05 08:06:00'::timestamp),
(97, 97, 97, '번호판 인식 실패', '렌즈 오염으로 OCR 실패', '부산12가1221', '2026-07-05 08:30:00'::timestamp, '처리완료', '렌즈 청소 후 재인식', '2026-07-05 08:36:00'::timestamp),
(98, 98, 98, '작업시간 초과', '예정 작업시간을 초과했습니다.', '부산12가1229', '2026-07-05 09:00:00'::timestamp, '처리완료', '작업 연장 승인', '2026-07-05 09:10:00'::timestamp),
(99, 99, 99, '네트워크 장애', '게이트 서버 연결 끊김', '부산12가1237', '2026-07-05 09:30:00'::timestamp, '처리중', '네트워크 복구 진행', NULL::timestamp),
(100, 100, 100, '미등록 기사', '등록되지 않은 기사입니다.', '부산12가1245', '2026-07-05 10:00:00'::timestamp, '처리완료', '기사 등록 후 출입 승인', '2026-07-05 10:12:00'::timestamp)
),
safe_seed AS (
    SELECT
        CASE
            WHEN EXISTS (
                SELECT 1 FROM work_order w
                WHERE w.work_order_id = s.requested_work_order_id
            )
            THEN s.requested_work_order_id
            ELSE NULL
        END AS work_order_id,
        CASE
            WHEN EXISTS (
                SELECT 1 FROM gate_log g
                WHERE g.gate_log_id = s.requested_gate_log_id
            )
            THEN s.requested_gate_log_id
            ELSE NULL
        END AS gate_log_id,
        (
            SELECT v.vehicle_id
            FROM vehicle v
            WHERE REPLACE(v.plate_number, ' ', '') =
                  REPLACE(s.plate_number, ' ', '')
            ORDER BY v.vehicle_id
            LIMIT 1
        ) AS vehicle_id,
        s.exception_type,
        s.exception_message,
        s.plate_number,
        s.occurred_time,
        s.process_status,
        s.manager_action,
        s.processed_time
    FROM legacy_seed s
)
INSERT INTO exception_log (
    work_order_id,
    gate_log_id,
    vehicle_id,
    exception_type,
    exception_message,
    plate_number,
    occurred_time,
    process_status,
    manager_action,
    processed_time
)
SELECT
    s.work_order_id,
    s.gate_log_id,
    s.vehicle_id,
    s.exception_type,
    s.exception_message,
    s.plate_number,
    s.occurred_time,
    s.process_status,
    s.manager_action,
    s.processed_time
FROM safe_seed s
WHERE NOT EXISTS (
    SELECT 1
    FROM exception_log e
    WHERE e.exception_type = s.exception_type
      AND COALESCE(e.plate_number, '') = COALESCE(s.plate_number, '')
      AND e.occurred_time = s.occurred_time
);


-- ---------------------------------------------------------------------
-- 4. 실제 번호판 기반 추가 트레일러 100대
-- ---------------------------------------------------------------------
WITH trailer_seed(seq, plate_number) AS (
    VALUES
(201, '11어6537'),
(202, '38모7592'),
(203, '40거3786'),
(204, '294모8707'),
(205, '19보5201'),
(206, '172호7297'),
(207, '283너5299'),
(208, '29다7772'),
(209, '24구4618'),
(210, '05마8082'),
(211, '184머8212'),
(212, '236고4718'),
(213, '113오6461'),
(214, '815어7150'),
(215, '152거1787'),
(216, '395서5321'),
(217, '333누7117'),
(218, '02가5438'),
(219, '154마8310'),
(220, '05모2402'),
(221, '83저8909'),
(222, '258부5164'),
(223, '111노1947'),
(224, '06러2097'),
(225, '137서4330'),
(226, '67주7597'),
(227, '359머3954'),
(228, '36주9788'),
(229, '29도2482'),
(230, '89너8667'),
(231, '40주4728'),
(232, '201수9010'),
(233, '73호8930'),
(234, '39나6109'),
(235, '309너1187'),
(236, '27저3396'),
(237, '95라4804'),
(238, '29저4053'),
(239, '14부0850'),
(240, '278나9379'),
(241, '151로5306'),
(242, '163가7411'),
(243, '73조0190'),
(244, '343너9292'),
(245, '95머5880'),
(246, '11로0982'),
(247, '68어8137'),
(248, '73고6730'),
(249, '24어9391'),
(250, '220호7872'),
(251, '43서4182'),
(252, '202두6437'),
(253, '49노6878'),
(254, '37더4771'),
(255, '142서9291'),
(256, '125루7682'),
(257, '101하7785'),
(258, '179모4904'),
(259, '38루9016'),
(260, '37라1528'),
(261, '05마3337'),
(262, '237어3833'),
(263, '299거2334'),
(264, '69저8799'),
(265, '67서8842'),
(266, '39어8013'),
(267, '116주1161'),
(268, '03저2117'),
(269, '52고0504'),
(270, '49우7781'),
(271, '242구8828'),
(272, '41수3572'),
(273, '324서8227'),
(274, '38무9205'),
(275, '162허4062'),
(276, '02부1170'),
(277, '334노4224'),
(278, '383오7000'),
(279, '85조4836'),
(280, '32보8132'),
(281, '203다7410'),
(282, '68오1803'),
(283, '102루1712'),
(284, '286도1482'),
(285, '06구0995'),
(286, '50도3535'),
(287, '28구6091'),
(288, '59다1496'),
(289, '94부8437'),
(290, '11두7185'),
(291, '192다5044'),
(292, '28주6391'),
(293, '179마5950'),
(294, '40버5967'),
(295, '106오3006'),
(296, '66버9361'),
(297, '22머8938'),
(298, '08나5976'),
(299, '88거5112'),
(300, '372보3544')
)
INSERT INTO vehicle (
    plate_number,
    vehicle_type,
    tonnage,
    is_registered,
    vehicle_status,
    tractor_no,
    chassis_no,
    carrier_id
)
SELECT
    s.plate_number,
    '트레일러',
    CASE WHEN s.seq % 3 = 0 THEN '18톤' ELSE '25톤' END,
    TRUE,
    CASE
        WHEN s.seq % 31 = 0 THEN '출입제한'
        WHEN s.seq % 17 = 0 THEN '점검중'
        ELSE '정상'
    END,
    'TL-' || LPAD(s.seq::text, 5, '0'),
    'TRL-CH-' || LPAD(s.seq::text, 6, '0'),
    (
        SELECT c.carrier_id
        FROM carrier c
        ORDER BY c.carrier_id
        OFFSET (
            (s.seq - 1) %
            NULLIF((SELECT COUNT(*) FROM carrier), 0)
        )
        LIMIT 1
    )
FROM trailer_seed s
WHERE NOT EXISTS (
    SELECT 1
    FROM vehicle v
    WHERE v.plate_number = s.plate_number
);

-- 이미 존재하는 동일 번호판은 트레일러 유형만 정합화한다.
WITH trailer_seed(seq, plate_number) AS (
    VALUES
(201, '11어6537'),
(202, '38모7592'),
(203, '40거3786'),
(204, '294모8707'),
(205, '19보5201'),
(206, '172호7297'),
(207, '283너5299'),
(208, '29다7772'),
(209, '24구4618'),
(210, '05마8082'),
(211, '184머8212'),
(212, '236고4718'),
(213, '113오6461'),
(214, '815어7150'),
(215, '152거1787'),
(216, '395서5321'),
(217, '333누7117'),
(218, '02가5438'),
(219, '154마8310'),
(220, '05모2402'),
(221, '83저8909'),
(222, '258부5164'),
(223, '111노1947'),
(224, '06러2097'),
(225, '137서4330'),
(226, '67주7597'),
(227, '359머3954'),
(228, '36주9788'),
(229, '29도2482'),
(230, '89너8667'),
(231, '40주4728'),
(232, '201수9010'),
(233, '73호8930'),
(234, '39나6109'),
(235, '309너1187'),
(236, '27저3396'),
(237, '95라4804'),
(238, '29저4053'),
(239, '14부0850'),
(240, '278나9379'),
(241, '151로5306'),
(242, '163가7411'),
(243, '73조0190'),
(244, '343너9292'),
(245, '95머5880'),
(246, '11로0982'),
(247, '68어8137'),
(248, '73고6730'),
(249, '24어9391'),
(250, '220호7872'),
(251, '43서4182'),
(252, '202두6437'),
(253, '49노6878'),
(254, '37더4771'),
(255, '142서9291'),
(256, '125루7682'),
(257, '101하7785'),
(258, '179모4904'),
(259, '38루9016'),
(260, '37라1528'),
(261, '05마3337'),
(262, '237어3833'),
(263, '299거2334'),
(264, '69저8799'),
(265, '67서8842'),
(266, '39어8013'),
(267, '116주1161'),
(268, '03저2117'),
(269, '52고0504'),
(270, '49우7781'),
(271, '242구8828'),
(272, '41수3572'),
(273, '324서8227'),
(274, '38무9205'),
(275, '162허4062'),
(276, '02부1170'),
(277, '334노4224'),
(278, '383오7000'),
(279, '85조4836'),
(280, '32보8132'),
(281, '203다7410'),
(282, '68오1803'),
(283, '102루1712'),
(284, '286도1482'),
(285, '06구0995'),
(286, '50도3535'),
(287, '28구6091'),
(288, '59다1496'),
(289, '94부8437'),
(290, '11두7185'),
(291, '192다5044'),
(292, '28주6391'),
(293, '179마5950'),
(294, '40버5967'),
(295, '106오3006'),
(296, '66버9361'),
(297, '22머8938'),
(298, '08나5976'),
(299, '88거5112'),
(300, '372보3544')
)
UPDATE vehicle v
SET vehicle_type = '트레일러'
FROM trailer_seed s
WHERE v.plate_number = s.plate_number
  AND v.vehicle_type NOT IN ('트레일러', 'TRAILER');

-- 데이터셋 테이블에 해당 번호판 행이 이미 있다면 운영용으로 표시.
WITH trailer_seed(seq, plate_number) AS (
    VALUES
(201, '11어6537'),
(202, '38모7592'),
(203, '40거3786'),
(204, '294모8707'),
(205, '19보5201'),
(206, '172호7297'),
(207, '283너5299'),
(208, '29다7772'),
(209, '24구4618'),
(210, '05마8082'),
(211, '184머8212'),
(212, '236고4718'),
(213, '113오6461'),
(214, '815어7150'),
(215, '152거1787'),
(216, '395서5321'),
(217, '333누7117'),
(218, '02가5438'),
(219, '154마8310'),
(220, '05모2402'),
(221, '83저8909'),
(222, '258부5164'),
(223, '111노1947'),
(224, '06러2097'),
(225, '137서4330'),
(226, '67주7597'),
(227, '359머3954'),
(228, '36주9788'),
(229, '29도2482'),
(230, '89너8667'),
(231, '40주4728'),
(232, '201수9010'),
(233, '73호8930'),
(234, '39나6109'),
(235, '309너1187'),
(236, '27저3396'),
(237, '95라4804'),
(238, '29저4053'),
(239, '14부0850'),
(240, '278나9379'),
(241, '151로5306'),
(242, '163가7411'),
(243, '73조0190'),
(244, '343너9292'),
(245, '95머5880'),
(246, '11로0982'),
(247, '68어8137'),
(248, '73고6730'),
(249, '24어9391'),
(250, '220호7872'),
(251, '43서4182'),
(252, '202두6437'),
(253, '49노6878'),
(254, '37더4771'),
(255, '142서9291'),
(256, '125루7682'),
(257, '101하7785'),
(258, '179모4904'),
(259, '38루9016'),
(260, '37라1528'),
(261, '05마3337'),
(262, '237어3833'),
(263, '299거2334'),
(264, '69저8799'),
(265, '67서8842'),
(266, '39어8013'),
(267, '116주1161'),
(268, '03저2117'),
(269, '52고0504'),
(270, '49우7781'),
(271, '242구8828'),
(272, '41수3572'),
(273, '324서8227'),
(274, '38무9205'),
(275, '162허4062'),
(276, '02부1170'),
(277, '334노4224'),
(278, '383오7000'),
(279, '85조4836'),
(280, '32보8132'),
(281, '203다7410'),
(282, '68오1803'),
(283, '102루1712'),
(284, '286도1482'),
(285, '06구0995'),
(286, '50도3535'),
(287, '28구6091'),
(288, '59다1496'),
(289, '94부8437'),
(290, '11두7185'),
(291, '192다5044'),
(292, '28주6391'),
(293, '179마5950'),
(294, '40버5967'),
(295, '106오3006'),
(296, '66버9361'),
(297, '22머8938'),
(298, '08나5976'),
(299, '88거5112'),
(300, '372보3544')
)
UPDATE trailer_plate_dataset d
SET is_operational_seed = TRUE
FROM trailer_seed s
WHERE d.plate_number = s.plate_number;

-- ---------------------------------------------------------------------
-- 5. 컨테이너 40개 추가
-- ---------------------------------------------------------------------
WITH sector_pool AS (
    SELECT
        sector_id,
        sector_name,
        block_name,
        ROW_NUMBER() OVER (ORDER BY sector_id) AS rn,
        COUNT(*) OVER () AS cnt
    FROM yard_sector
),
numbers AS (
    SELECT generate_series(61, 100) AS rn
)
INSERT INTO container (
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
)
SELECT
    'TRAINU' || LPAD(n.rn::text, 7, '0'),
    CASE WHEN n.rn % 2 = 0 THEN '40FT' ELSE '20FT' END,
    sp.sector_name,
    sp.sector_id,
    sp.block_name,
    LPAD((((n.rn - 1) % 20) + 1)::text, 2, '0'),
    LPAD((((n.rn - 1) % 8) + 1)::text, 2, '0'),
    n.rn % 11 <> 0,
    'TRAIN-SEAL-' || LPAD(n.rn::text, 5, '0'),
    (ARRAY['HMM','MSC','ONE','OOCL','MAERSK'])[
        ((n.rn - 1) % 5) + 1
    ]
FROM numbers n
JOIN sector_pool sp
  ON sp.rn = ((n.rn - 1) % sp.cnt) + 1
WHERE NOT EXISTS (
    SELECT 1
    FROM container c
    WHERE c.container_number =
          'TRAINU' || LPAD(n.rn::text, 7, '0')
);

-- ---------------------------------------------------------------------
-- 6. 작업오더 40건 추가
--    기사/트랙터/컨테이너가 없는 DB에서는 오류 없이 0건 처리된다.
-- ---------------------------------------------------------------------
WITH trailer_pool AS (
    SELECT
        v.vehicle_id,
        ROW_NUMBER() OVER (ORDER BY v.vehicle_id) AS rn,
        COUNT(*) OVER () AS cnt
    FROM vehicle v
    WHERE v.plate_number IN (
        SELECT plate_number
        FROM (VALUES
('11어6537'),
('38모7592'),
('40거3786'),
('294모8707'),
('19보5201'),
('172호7297'),
('283너5299'),
('29다7772'),
('24구4618'),
('05마8082'),
('184머8212'),
('236고4718'),
('113오6461'),
('815어7150'),
('152거1787'),
('395서5321'),
('333누7117'),
('02가5438'),
('154마8310'),
('05모2402'),
('83저8909'),
('258부5164'),
('111노1947'),
('06러2097'),
('137서4330'),
('67주7597'),
('359머3954'),
('36주9788'),
('29도2482'),
('89너8667'),
('40주4728'),
('201수9010'),
('73호8930'),
('39나6109'),
('309너1187'),
('27저3396'),
('95라4804'),
('29저4053'),
('14부0850'),
('278나9379'),
('151로5306'),
('163가7411'),
('73조0190'),
('343너9292'),
('95머5880'),
('11로0982'),
('68어8137'),
('73고6730'),
('24어9391'),
('220호7872'),
('43서4182'),
('202두6437'),
('49노6878'),
('37더4771'),
('142서9291'),
('125루7682'),
('101하7785'),
('179모4904'),
('38루9016'),
('37라1528'),
('05마3337'),
('237어3833'),
('299거2334'),
('69저8799'),
('67서8842'),
('39어8013'),
('116주1161'),
('03저2117'),
('52고0504'),
('49우7781'),
('242구8828'),
('41수3572'),
('324서8227'),
('38무9205'),
('162허4062'),
('02부1170'),
('334노4224'),
('383오7000'),
('85조4836'),
('32보8132'),
('203다7410'),
('68오1803'),
('102루1712'),
('286도1482'),
('06구0995'),
('50도3535'),
('28구6091'),
('59다1496'),
('94부8437'),
('11두7185'),
('192다5044'),
('28주6391'),
('179마5950'),
('40버5967'),
('106오3006'),
('66버9361'),
('22머8938'),
('08나5976'),
('88거5112'),
('372보3544')
        ) AS p(plate_number)
    )
      AND v.vehicle_type IN ('트레일러', 'TRAILER')
      AND COALESCE(v.is_registered, TRUE) = TRUE
      AND COALESCE(v.vehicle_status, '정상') = '정상'
),
tractor_pool AS (
    SELECT
        vehicle_id,
        ROW_NUMBER() OVER (ORDER BY vehicle_id) AS rn,
        COUNT(*) OVER () AS cnt
    FROM vehicle
    WHERE vehicle_type IN ('트랙터', 'TRACTOR')
      AND COALESCE(is_registered, TRUE) = TRUE
),
driver_pool AS (
    SELECT
        driver_id,
        ROW_NUMBER() OVER (ORDER BY driver_id) AS rn,
        COUNT(*) OVER () AS cnt
    FROM driver
    WHERE COALESCE(is_registered, FALSE) = TRUE
      AND COALESCE(can_enter, FALSE) = TRUE
),
container_pool AS (
    SELECT
        container_id,
        ROW_NUMBER() OVER (ORDER BY container_id) AS rn,
        COUNT(*) OVER () AS cnt
    FROM container
    WHERE container_number LIKE 'TRAINU%'
),
scenario AS (
    SELECT
        n,
        (ARRAY[
            'DISPATCH_WAITING',
            'APPROVED',
            'GATE_IN',
            'IN_PROGRESS',
            'COMPLETED',
            'GATE_OUT'
        ])[((n - 1) % 6) + 1] AS work_status
    FROM generate_series(61, 100) AS n
)
INSERT INTO work_order (
    work_type,
    vehicle_id,
    tractor_vehicle_id,
    trailer_vehicle_id,
    driver_id,
    container_id,
    reserved_time,
    work_status,
    is_approved
)
SELECT
    CASE WHEN s.n % 2 = 1 THEN '반출 상차' ELSE '반입 하차' END,
    tr.vehicle_id,
    tc.vehicle_id,
    tr.vehicle_id,
    dr.driver_id,
    co.container_id,
    TIMESTAMP '2026-07-15 08:00:00'
        + (s.n - 61) * INTERVAL '15 minutes',
    s.work_status,
    s.work_status <> 'DISPATCH_WAITING'
FROM scenario s
JOIN trailer_pool tr
  ON tr.rn = ((s.n - 1) % tr.cnt) + 1
JOIN tractor_pool tc
  ON tc.rn = ((s.n - 1) % tc.cnt) + 1
JOIN driver_pool dr
  ON dr.rn = ((s.n - 1) % dr.cnt) + 1
JOIN container_pool co
  ON co.rn = ((s.n - 1) % co.cnt) + 1
WHERE NOT EXISTS (
    SELECT 1
    FROM work_order w
    WHERE w.reserved_time =
          TIMESTAMP '2026-07-15 08:00:00'
          + (s.n - 61) * INTERVAL '15 minutes'
      AND w.trailer_vehicle_id = tr.vehicle_id
);

-- ---------------------------------------------------------------------
-- 7. 게이트 로그
-- ---------------------------------------------------------------------
WITH trailer_pool AS (
    SELECT
        v.vehicle_id,
        ROW_NUMBER() OVER (ORDER BY v.vehicle_id) AS rn
    FROM vehicle v
    WHERE v.plate_number IN (
        SELECT plate_number
        FROM (VALUES
('11어6537'),
('38모7592'),
('40거3786'),
('294모8707'),
('19보5201'),
('172호7297'),
('283너5299'),
('29다7772'),
('24구4618'),
('05마8082'),
('184머8212'),
('236고4718'),
('113오6461'),
('815어7150'),
('152거1787'),
('395서5321'),
('333누7117'),
('02가5438'),
('154마8310'),
('05모2402'),
('83저8909'),
('258부5164'),
('111노1947'),
('06러2097'),
('137서4330'),
('67주7597'),
('359머3954'),
('36주9788'),
('29도2482'),
('89너8667'),
('40주4728'),
('201수9010'),
('73호8930'),
('39나6109'),
('309너1187'),
('27저3396'),
('95라4804'),
('29저4053'),
('14부0850'),
('278나9379'),
('151로5306'),
('163가7411'),
('73조0190'),
('343너9292'),
('95머5880'),
('11로0982'),
('68어8137'),
('73고6730'),
('24어9391'),
('220호7872'),
('43서4182'),
('202두6437'),
('49노6878'),
('37더4771'),
('142서9291'),
('125루7682'),
('101하7785'),
('179모4904'),
('38루9016'),
('37라1528'),
('05마3337'),
('237어3833'),
('299거2334'),
('69저8799'),
('67서8842'),
('39어8013'),
('116주1161'),
('03저2117'),
('52고0504'),
('49우7781'),
('242구8828'),
('41수3572'),
('324서8227'),
('38무9205'),
('162허4062'),
('02부1170'),
('334노4224'),
('383오7000'),
('85조4836'),
('32보8132'),
('203다7410'),
('68오1803'),
('102루1712'),
('286도1482'),
('06구0995'),
('50도3535'),
('28구6091'),
('59다1496'),
('94부8437'),
('11두7185'),
('192다5044'),
('28주6391'),
('179마5950'),
('40버5967'),
('106오3006'),
('66버9361'),
('22머8938'),
('08나5976'),
('88거5112'),
('372보3544')
        ) AS p(plate_number)
    )
),
tractor_pool AS (
    SELECT
        vehicle_id,
        ROW_NUMBER() OVER (ORDER BY vehicle_id) AS rn,
        COUNT(*) OVER () AS cnt
    FROM vehicle
    WHERE vehicle_type IN ('트랙터', 'TRACTOR')
)
INSERT INTO gate_log (
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
)
SELECT
    tr.vehicle_id,
    tc.vehicle_id,
    tr.vehicle_id,
    (ARRAY['G-IN-01','G-IN-02','G-OUT-01','G-OUT-02'])[
        ((tr.rn - 1) % 4) + 1
    ],
    (ARRAY[
        '입차 게이트 1',
        '입차 게이트 2',
        '출차 게이트 1',
        '출차 게이트 2'
    ])[((tr.rn - 1) % 4) + 1],
    TIMESTAMP '2026-07-15 06:00:00'
        + (tr.rn - 1) * INTERVAL '3 minutes',
    CASE
        WHEN tr.rn % 4 IN (3, 0)
        THEN TIMESTAMP '2026-07-15 06:02:00'
             + (tr.rn - 1) * INTERVAL '3 minutes'
        ELSE NULL
    END,
    CASE WHEN tr.rn % 4 IN (1, 2) THEN 'IN' ELSE 'OUT' END,
    CASE
        WHEN tr.rn % 13 = 0 THEN '실패'
        WHEN tr.rn % 9 = 0 THEN '검토'
        ELSE '정상'
    END,
    tr.rn % 13 <> 0 AND tr.rn % 9 <> 0
FROM trailer_pool tr
JOIN tractor_pool tc
  ON tc.rn = ((tr.rn - 1) % tc.cnt) + 1
WHERE NOT EXISTS (
    SELECT 1
    FROM gate_log g
    WHERE g.trailer_vehicle_id = tr.vehicle_id
      AND g.entry_time =
          TIMESTAMP '2026-07-15 06:00:00'
          + (tr.rn - 1) * INTERVAL '3 minutes'
);

-- ---------------------------------------------------------------------
-- 8. OCR 결과 저장 → 아래 트리거가 실시간 예외를 자동 생성
-- ---------------------------------------------------------------------
WITH gate_rows AS (
    SELECT
        g.gate_log_id,
        g.trailer_vehicle_id,
        g.entry_time,
        ROW_NUMBER() OVER (ORDER BY g.gate_log_id) AS rn
    FROM gate_log g
    WHERE g.entry_time::date = DATE '2026-07-15'
)
INSERT INTO plate_recognition (
    gate_log_id,
    vehicle_image,
    recognized_plate,
    plate_type,
    is_success,
    confidence,
    manual_correction,
    error_message,
    recognition_time
)
SELECT
    g.gate_log_id,
    d.image_path,
    v.plate_number,
    'TRAILER',
    g.rn % 13 <> 0,
    CASE
        WHEN g.rn % 13 = 0 THEN 55.40
        ELSE 92 + (g.rn % 7)
    END,
    CASE WHEN g.rn % 13 = 0 THEN v.plate_number ELSE NULL END,
    CASE
        WHEN g.rn % 13 = 0 THEN 'OCR 신뢰도 기준 미달'
        ELSE NULL
    END,
    g.entry_time + INTERVAL '5 seconds'
FROM gate_rows g
JOIN vehicle v
  ON v.vehicle_id = g.trailer_vehicle_id
JOIN LATERAL (
    SELECT image_path
    FROM trailer_plate_dataset
    WHERE plate_number = v.plate_number
    ORDER BY dataset_id
    LIMIT 1
) d ON TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM plate_recognition p
    WHERE p.gate_log_id = g.gate_log_id
);

-- ---------------------------------------------------------------------
-- 9. 상태 이력
-- ---------------------------------------------------------------------
INSERT INTO work_status_history (
    work_order_id,
    prev_status,
    new_status,
    changed_time,
    changed_by,
    reason,
    remark
)
SELECT
    w.work_order_id,
    CASE w.work_status
        WHEN 'APPROVED' THEN 'DISPATCH_WAITING'
        WHEN 'GATE_IN' THEN 'APPROVED'
        WHEN 'IN_PROGRESS' THEN 'GATE_IN'
        WHEN 'COMPLETED' THEN 'IN_PROGRESS'
        WHEN 'GATE_OUT' THEN 'COMPLETED'
        ELSE NULL
    END,
    w.work_status,
    w.reserved_time - INTERVAL '10 minutes',
    CASE
        WHEN w.work_status = 'APPROVED' THEN 'ADMIN'
        WHEN w.work_status IN ('GATE_IN', 'GATE_OUT')
            THEN 'GATE_SYSTEM'
        WHEN w.work_status = 'DISPATCH_WAITING'
            THEN 'CARRIER'
        ELSE 'DRIVER'
    END,
    '추가 더미 시나리오',
    'train(4).txt 트레일러 기반'
FROM work_order w
WHERE w.reserved_time::date = DATE '2026-07-15'
  AND NOT EXISTS (
      SELECT 1
      FROM work_status_history h
      WHERE h.work_order_id = w.work_order_id
        AND h.new_status = w.work_status
  );

-- ---------------------------------------------------------------------
-- 10. 확장 예외 30건
-- ---------------------------------------------------------------------
WITH source_rows AS (
    SELECT
        g.gate_log_id,
        g.trailer_vehicle_id AS vehicle_id,
        v.plate_number,
        g.entry_time,
        ROW_NUMBER() OVER (ORDER BY g.gate_log_id) AS rn
    FROM gate_log g
    JOIN vehicle v
      ON v.vehicle_id = g.trailer_vehicle_id
    WHERE g.entry_time::date = DATE '2026-07-15'
    ORDER BY g.gate_log_id
    LIMIT 30
)
INSERT INTO exception_log (
    gate_log_id,
    vehicle_id,
    exception_type,
    exception_message,
    plate_number,
    occurred_time,
    process_status,
    manager_action,
    processed_time
)
SELECT
    s.gate_log_id,
    s.vehicle_id,
    (ARRAY[
        '번호판 인식 실패',
        '미등록 차량',
        '작업 미승인',
        '트랙터·트레일러 불일치',
        '컨테이너 출차 보류'
    ])[((s.rn - 1) % 5) + 1],
    '확장 더미 예외 시나리오',
    s.plate_number,
    s.entry_time + INTERVAL '1 minute',
    CASE WHEN s.rn % 3 = 0 THEN '처리완료' ELSE '미처리' END,
    CASE WHEN s.rn % 3 = 0 THEN '관리자 확인 완료' ELSE NULL END,
    CASE
        WHEN s.rn % 3 = 0
        THEN s.entry_time + INTERVAL '8 minutes'
        ELSE NULL
    END
FROM source_rows s
WHERE NOT EXISTS (
    SELECT 1
    FROM exception_log e
    WHERE e.gate_log_id = s.gate_log_id
      AND e.exception_type =
          (ARRAY[
              '번호판 인식 실패',
              '미등록 차량',
              '작업 미승인',
              '트랙터·트레일러 불일치',
              '컨테이너 출차 보류'
          ])[((s.rn - 1) % 5) + 1]
      AND e.occurred_time = s.entry_time + INTERVAL '1 minute'
);

-- 완료 또는 출차 완료된 작업의 트레일러 배정 초기화.
UPDATE vehicle v
SET driver_id = NULL,
    user_id = NULL
FROM work_order w
WHERE v.vehicle_id = w.trailer_vehicle_id
  AND w.reserved_time::date = DATE '2026-07-15'
  AND w.work_status IN ('COMPLETED', 'GATE_OUT');

COMMIT;

-- =====================================================================
-- 11. OCR 결과 기반 실시간 exception_log 자동 생성
--     트랜잭션 밖에서 함수와 트리거를 생성한다.
-- =====================================================================
CREATE OR REPLACE FUNCTION fn_create_exception_from_plate_recognition()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    matched_vehicle_id BIGINT;
    matched_plate VARCHAR(30);
    exception_code VARCHAR(50);
    exception_text VARCHAR(255);
BEGIN
    matched_plate := NULLIF(TRIM(NEW.recognized_plate), '');

    IF matched_plate IS NOT NULL THEN
        SELECT v.vehicle_id
        INTO matched_vehicle_id
        FROM vehicle v
        WHERE REPLACE(v.plate_number, ' ', '') =
              REPLACE(matched_plate, ' ', '')
        ORDER BY v.vehicle_id
        LIMIT 1;
    END IF;

    IF COALESCE(NEW.is_success, FALSE) = FALSE THEN
        exception_code := 'PLATE_RECOGNITION_FAILED';
        exception_text := COALESCE(
            NULLIF(NEW.error_message, ''),
            '번호판을 정상적으로 인식하지 못했습니다.'
        );
    ELSIF NEW.confidence IS NULL OR NEW.confidence < 70 THEN
        exception_code := 'LOW_CONFIDENCE';
        exception_text :=
            '번호판 인식 신뢰도가 기준치보다 낮습니다. confidence='
            || COALESCE(NEW.confidence::text, 'NULL');
    ELSIF matched_vehicle_id IS NULL THEN
        exception_code := 'UNREGISTERED_VEHICLE';
        exception_text :=
            '등록되지 않은 번호판입니다: '
            || COALESCE(matched_plate, '(인식값 없음)');
    ELSE
        RETURN NEW;
    END IF;

    -- 동일 게이트·동일 유형 예외의 30초 내 중복 저장을 방지한다.
    IF NOT EXISTS (
        SELECT 1
        FROM exception_log e
        WHERE e.gate_log_id IS NOT DISTINCT FROM NEW.gate_log_id
          AND e.exception_type = exception_code
          AND e.occurred_time >= CURRENT_TIMESTAMP - INTERVAL '30 seconds'
    ) THEN
        INSERT INTO exception_log (
            gate_log_id,
            vehicle_id,
            exception_type,
            exception_message,
            plate_number,
            occurred_time,
            process_status
        )
        VALUES (
            NEW.gate_log_id,
            matched_vehicle_id,
            exception_code,
            exception_text,
            matched_plate,
            COALESCE(NEW.recognition_time, CURRENT_TIMESTAMP),
            '미처리'
        );
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_plate_recognition_exception
ON plate_recognition;

CREATE TRIGGER trg_plate_recognition_exception
AFTER INSERT OR UPDATE OF
    recognized_plate,
    is_success,
    confidence,
    error_message
ON plate_recognition
FOR EACH ROW
EXECUTE FUNCTION fn_create_exception_from_plate_recognition();

-- =====================================================================
-- 검증용 조회
-- =====================================================================
-- SELECT COUNT(*) FROM exception_log;
-- SELECT COUNT(*) FROM vehicle
--  WHERE tractor_no BETWEEN 'TL-00201' AND 'TL-00300';
-- SELECT COUNT(*) FROM work_order
--  WHERE reserved_time::date = DATE '2026-07-15';
-- SELECT COUNT(*) FROM gate_log
--  WHERE entry_time::date = DATE '2026-07-15';
-- SELECT COUNT(*) FROM plate_recognition
--  WHERE recognition_time::date = DATE '2026-07-15';
