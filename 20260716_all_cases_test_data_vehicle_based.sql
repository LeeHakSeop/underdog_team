-- =====================================================================
-- vehicle(2).csv 실제 차량 데이터를 참조한 통합 테스트 데이터
-- PostgreSQL / underdog_team-hakseop
-- 생성 기준: 업로드된 vehicle(2).csv (608행)
-- 테스트 PK 범위: 920000~920999
--
-- 중요:
-- 1) 이 SQL은 vehicle 테이블의 실제 기존 차량 ID/번호판을 재사용합니다.
-- 2) vehicle 데이터 자체는 INSERT/UPDATE/DELETE하지 않습니다.
-- 3) work_order/gate_log/OCR/이력/예외 테스트 행만 920000대에 생성합니다.
-- 4) exception_log 스키마는 프로젝트의 exception_log(fix).sql
--    (exception_log_id, gate_log_id, vehicle_id, plate_number...) 기준입니다.
-- =====================================================================
BEGIN;

-- 0. 필수 컬럼/테이블 확인
DO $$
DECLARE missing text := '';
BEGIN
  IF to_regclass('public.vehicle') IS NULL THEN missing := missing || ' vehicle'; END IF;
  IF to_regclass('public.work_order') IS NULL THEN missing := missing || ' work_order'; END IF;
  IF to_regclass('public.gate_log') IS NULL THEN missing := missing || ' gate_log'; END IF;
  IF to_regclass('public.plate_recognition') IS NULL THEN missing := missing || ' plate_recognition'; END IF;
  IF to_regclass('public.work_status_history') IS NULL THEN missing := missing || ' work_status_history'; END IF;
  IF to_regclass('public.exception_log') IS NULL THEN missing := missing || ' exception_log'; END IF;
  IF missing <> '' THEN RAISE EXCEPTION '필수 테이블 누락:%', missing; END IF;
END $$;

-- 1. 업로드 CSV 기반 차량 존재/속성 검증
DO $$
DECLARE bad text := '';
BEGIN
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=307 AND plate_number='서울84사2453') THEN bad:=bad||' [307 정상트랙터]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=1 AND plate_number='01가7431') THEN bad:=bad||' [1 정상트레일러]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=4 AND plate_number='01가8709') THEN bad:=bad||' [4 정상트레일러]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=9 AND plate_number='부산80바1009') THEN bad:=bad||' [9 정상트레일러]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=6 AND plate_number='01고6421' AND vehicle_status='점검중') THEN bad:=bad||' [6 점검중]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=16 AND plate_number='01고8819' AND is_registered=false) THEN bad:=bad||' [16 미등록]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=604 AND plate_number='171머3292' AND vehicle_status='작업중') THEN bad:=bad||' [604 작업중]'; END IF;
  IF NOT EXISTS (SELECT 1 FROM vehicle WHERE vehicle_id=2 AND plate_number='부산80바1002') THEN bad:=bad||' [2 정상카고]'; END IF;
  IF bad<>'' THEN RAISE EXCEPTION 'vehicle(2).csv 기준 차량과 DB 불일치:%',bad; END IF;
END $$;

-- 2. 재실행 정리 (자식→부모)
DELETE FROM exception_log WHERE exception_log_id BETWEEN 920000 AND 920999 OR gate_log_id BETWEEN 920000 AND 920999;
DELETE FROM plate_recognition WHERE plate_recognition_id BETWEEN 920000 AND 920999 OR gate_log_id BETWEEN 920000 AND 920999;
DELETE FROM work_status_history WHERE history_id BETWEEN 920000 AND 920999 OR work_order_id BETWEEN 920000 AND 920999;
DELETE FROM gate_log WHERE gate_log_id BETWEEN 920000 AND 920999;
DELETE FROM work_order WHERE work_order_id BETWEEN 920000 AND 920999;

-- 3. 작업오더: 실제 차량 ID 사용
-- 기사/컨테이너는 기존 데이터(기사 307,309 / 컨테이너 1~12)를 사용
INSERT INTO work_order(work_order_id,work_type,vehicle_id,tractor_vehicle_id,trailer_vehicle_id,driver_id,container_id,reserved_time,work_status,is_approved) VALUES
(920001,'반출 상차',1,307,1,307,1,CURRENT_TIMESTAMP+INTERVAL '1 hour','DISPATCH_WAITING',false),
(920002,'반입 하차',4,308,4,307,2,CURRENT_TIMESTAMP+INTERVAL '2 hour','APPROVED',true),
(920003,'반출 상차',9,307,9,307,3,CURRENT_TIMESTAMP-INTERVAL '20 min','GATE_IN',true),
(920004,'반입 하차',11,308,11,307,4,CURRENT_TIMESTAMP-INTERVAL '1 hour','IN_PROGRESS',true),
(920005,'반출 상차',19,311,19,309,5,CURRENT_TIMESTAMP-INTERVAL '2 hour','COMPLETED',true),
(920006,'반입 하차',21,307,21,307,6,CURRENT_TIMESTAMP-INTERVAL '3 hour','GATE_OUT',true),
(920007,'반출 상차',26,308,26,307,7,CURRENT_TIMESTAMP+INTERVAL '3 hour','CANCELED',false),
-- 미승인 입차 시도
(920010,'반출 상차',29,307,29,307,8,CURRENT_TIMESTAMP,'DISPATCH_WAITING',false),
-- 동일 트레일러 9 중복 배정(검증용 의도적 비정상 행)
(920011,'반출 상차',9,308,9,307,9,CURRENT_TIMESTAMP+INTERVAL '4 hour','APPROVED',true),
-- 점검중/미등록/작업중 트레일러 배정 시도
(920012,'반출 상차',6,307,6,307,10,CURRENT_TIMESTAMP+INTERVAL '5 hour','DISPATCH_WAITING',false),
(920013,'반출 상차',16,307,16,307,11,CURRENT_TIMESTAMP+INTERVAL '6 hour','DISPATCH_WAITING',false),
(920014,'반출 상차',604,307,604,307,12,CURRENT_TIMESTAMP+INTERVAL '7 hour','DISPATCH_WAITING',false),
-- 카고 단독 호환 케이스
(920015,'하차',2,NULL,NULL,307,2,CURRENT_TIMESTAMP+INTERVAL '8 hour','APPROVED',true);

-- 4. 상태 이력
INSERT INTO work_status_history(history_id,work_order_id,prev_status,new_status,changed_time,changed_by,reason,remark) VALUES
(920001,920001,NULL,'DISPATCH_WAITING',CURRENT_TIMESTAMP-INTERVAL '10 min','TEST_CARRIER','작업 생성','최초 상태'),
(920002,920002,'DISPATCH_WAITING','APPROVED',CURRENT_TIMESTAMP-INTERVAL '90 min','TEST_ADMIN','관리자 승인',NULL),
(920003,920003,'APPROVED','GATE_IN',CURRENT_TIMESTAMP-INTERVAL '20 min','SYSTEM','게이트 입차',NULL),
(920004,920004,'GATE_IN','IN_PROGRESS',CURRENT_TIMESTAMP-INTERVAL '50 min','TEST_DRIVER','작업 시작',NULL),
(920005,920005,'IN_PROGRESS','COMPLETED',CURRENT_TIMESTAMP-INTERVAL '30 min','TEST_DRIVER','작업 완료',NULL),
(920006,920006,'COMPLETED','GATE_OUT',CURRENT_TIMESTAMP-INTERVAL '10 min','SYSTEM','게이트 출차',NULL),
(920007,920007,'DISPATCH_WAITING','CANCELED',CURRENT_TIMESTAMP-INTERVAL '5 min','TEST_ADMIN','작업 취소',NULL),
(920008,920004,'IN_PROGRESS','GATE_IN',CURRENT_TIMESTAMP-INTERVAL '45 min','TEST_ADMIN','오입력 상태 복구','관리자 수동 역전이'),
(920009,920004,'GATE_IN','IN_PROGRESS',CURRENT_TIMESTAMP-INTERVAL '40 min','TEST_ADMIN','검증 후 재개','사유 필수 검증');

-- 5. 게이트 로그
INSERT INTO gate_log(gate_log_id,vehicle_id,tractor_vehicle_id,trailer_vehicle_id,gate_number,gate_name,entry_time,exit_time,in_out_type,process_result,manager_check) VALUES
(920001,9,307,9,'G-IN-01','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '20 min',NULL,'IN','정상',true),
(920002,11,308,11,'G-IN-02','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '55 min',NULL,'IN','정상',true),
(920003,19,311,19,'G-IN-01','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '2 hour',NULL,'IN','정상',true),
(920004,21,307,21,'G-OUT-01','테스트 출차게이트',CURRENT_TIMESTAMP-INTERVAL '3 hour',CURRENT_TIMESTAMP-INTERVAL '10 min','OUT','정상',true),
-- 미승인 입차
(920010,29,307,29,'G-IN-01','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '8 min',NULL,'IN','실패',false),
-- 중복 입차: vehicle 9에 두 번째 열린 IN
(920011,9,307,9,'G-IN-02','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '5 min',NULL,'IN','검토',false),
-- 입차 없는 출차
(920012,4,308,4,'G-OUT-02','테스트 출차게이트',NULL,CURRENT_TIMESTAMP-INTERVAL '3 min','OUT','실패',false),
-- 트랙터/트레일러 불일치: 작업 920002는 trailer 4, 실제 1
(920013,1,308,1,'G-IN-01','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '2 min',NULL,'IN','검토',false),
-- 장비 오류
(920014,6,307,6,'G-IN-02','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '1 min',NULL,'IN','실패',false),
-- 카고 정상 입차
(920015,2,NULL,NULL,'G-IN-01','테스트 입차게이트',CURRENT_TIMESTAMP-INTERVAL '15 min',NULL,'IN','정상',true);

-- 6. OCR 결과: 실제 차량 번호판 사용
INSERT INTO plate_recognition(plate_recognition_id,gate_log_id,vehicle_image,recognized_plate,plate_type,is_success,confidence,manual_correction,error_message,recognition_time) VALUES
(920001,920001,'/test/vehicle_9_ok.jpg','부산80바1009','TRAILER',true,99.40,NULL,NULL,CURRENT_TIMESTAMP-INTERVAL '19 min'),
(920002,920002,'/test/vehicle_11_boundary.jpg','부산80바1011','TRAILER',true,80.00,NULL,NULL,CURRENT_TIMESTAMP-INTERVAL '54 min'),
(920003,920003,'/test/vehicle_19_low.jpg','01구8649','TRAILER',false,62.50,NULL,'LOW_CONFIDENCE',CURRENT_TIMESTAMP-INTERVAL '119 min'),
(920004,920010,'/test/no_detection.jpg',NULL,'UNKNOWN',false,0.00,NULL,'NO_PLATE_DETECTED',CURRENT_TIMESTAMP-INTERVAL '7 min'),
(920005,920011,'/test/vehicle_9_wrong.jpg','부산80바100O','TRAILER',false,68.20,'부산80바1009','OCR_MISREAD',CURRENT_TIMESTAMP-INTERVAL '4 min'),
(920006,920013,'/test/unregistered.jpg','부산99가9999','TRAILER',false,96.20,NULL,'UNREGISTERED_PLATE',CURRENT_TIMESTAMP-INTERVAL '90 sec'),
(920007,920014,'/test/device_error.jpg',NULL,'UNKNOWN',false,NULL,NULL,'CAMERA_TIMEOUT',CURRENT_TIMESTAMP-INTERVAL '30 sec'),
(920008,920001,'/test/tractor_307.jpg','서울84사2453','TRACTOR',true,98.80,NULL,NULL,CURRENT_TIMESTAMP-INTERVAL '18 min'),
-- 동일 gate 재인식
(920009,920003,'/test/vehicle_19_retry.jpg','01구8649','TRAILER',true,97.70,NULL,NULL,CURRENT_TIMESTAMP-INTERVAL '118 min'),
(920010,920015,'/test/cargo_2.jpg','부산80바1002','UNKNOWN',true,95.10,NULL,NULL,CURRENT_TIMESTAMP-INTERVAL '14 min');

-- 7. 예외 로그 (프로젝트 실제 컬럼명)
INSERT INTO exception_log(exception_log_id,gate_log_id,vehicle_id,plate_number,exception_type,exception_message,occurred_time,process_status,manager_action,processed_time) VALUES
(920001,920010,29,'01구9051','WORK_NOT_APPROVED','승인되지 않은 작업 차량의 입차 시도',CURRENT_TIMESTAMP-INTERVAL '8 min','PENDING',NULL,NULL),
(920002,920011,9,'부산80바1009','DUPLICATE_GATE_IN','출차 전 동일 차량 중복 입차',CURRENT_TIMESTAMP-INTERVAL '5 min','처리중','기존 입차 이력 확인 중',NULL),
(920003,920012,4,'01가8709','DUPLICATE_GATE_OUT','선행 입차 이력 없이 출차 시도',CURRENT_TIMESTAMP-INTERVAL '3 min','처리완료','출차 거부',CURRENT_TIMESTAMP-INTERVAL '2 min'),
(920004,920013,1,'01가7431','TRACTOR_TRAILER_MISMATCH','작업지시와 실제 트레일러 불일치',CURRENT_TIMESTAMP-INTERVAL '2 min','처리중','배차 정보 확인 중',NULL),
(920005,920014,6,'01고6421','GATE_DEVICE_ERROR','카메라/게이트 통신 오류',CURRENT_TIMESTAMP-INTERVAL '1 min','PENDING',NULL,NULL),
(920006,920003,19,'01구8649','PLATE_RECOGNITION_FAILED','OCR 신뢰도 기준 미달',CURRENT_TIMESTAMP-INTERVAL '119 min','처리완료','재촬영 후 정상 인식',CURRENT_TIMESTAMP-INTERVAL '118 min'),
(920007,920013,NULL,'부산99가9999','UNREGISTERED_VEHICLE','등록되지 않은 번호판 인식',CURRENT_TIMESTAMP-INTERVAL '90 sec','처리중','관리자 수동 확인',NULL),
(920008,920003,19,'01구8649','CONTAINER_EXIT_HOLD','container_id=5 반출 보류 상태',CURRENT_TIMESTAMP-INTERVAL '25 min','PENDING',NULL,NULL),
-- CSV에 존재하는 한글 레거시 유형도 화면 호환 검증
(920009,920014,6,'01고6421','번호판 훼손','번호판 일부 훼손으로 인식 실패',CURRENT_TIMESTAMP-INTERVAL '50 sec','처리완료','육안 확인 및 수동 입력',CURRENT_TIMESTAMP-INTERVAL '20 sec'),
(920010,920014,6,'01고6421','게이트 통신 오류','게이트 장비 연결 실패',CURRENT_TIMESTAMP-INTERVAL '40 sec','처리중','장비 재연결 중',NULL);

COMMIT;

-- 8. 검증 조회
SELECT work_status,COUNT(*) FROM work_order WHERE work_order_id BETWEEN 920000 AND 920999 GROUP BY work_status ORDER BY work_status;
SELECT in_out_type,process_result,COUNT(*) FROM gate_log WHERE gate_log_id BETWEEN 920000 AND 920999 GROUP BY in_out_type,process_result ORDER BY in_out_type,process_result;
SELECT plate_type,is_success,COUNT(*) FROM plate_recognition WHERE plate_recognition_id BETWEEN 920000 AND 920999 GROUP BY plate_type,is_success ORDER BY plate_type,is_success;
SELECT exception_type,process_status,COUNT(*) FROM exception_log WHERE exception_log_id BETWEEN 920000 AND 920999 GROUP BY exception_type,process_status ORDER BY exception_type,process_status;
SELECT v.vehicle_id,v.plate_number,v.vehicle_type,v.vehicle_status,v.is_registered
FROM vehicle v WHERE v.vehicle_id IN (307,308,311,1,4,9,11,19,21,26,29,6,16,604,2,8,313)
ORDER BY v.vehicle_id;
