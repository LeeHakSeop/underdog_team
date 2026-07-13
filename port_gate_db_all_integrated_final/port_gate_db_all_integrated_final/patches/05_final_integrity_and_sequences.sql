-- 최종 정합성 및 시퀀스 보정
BEGIN;

-- 불필요 문자 사전 테이블은 최종 구성에서 사용하지 않음.
DROP TABLE IF EXISTS korean_plate_character CASCADE;

-- 완료/출차 작업의 트레일러 배정 초기화
UPDATE vehicle v
SET driver_id=NULL, user_id=NULL
FROM work_order w
WHERE v.vehicle_id=w.trailer_vehicle_id
  AND w.work_status IN ('COMPLETED','GATE_OUT');

-- 시퀀스가 존재하는 테이블만 MAX(PK) 다음 값으로 보정
DO $$
DECLARE r record; seq_name text; max_id bigint;
BEGIN
  FOR r IN SELECT * FROM (VALUES
    ('users','user_id'),('carrier','carrier_id'),('driver','driver_id'),
    ('vehicle','vehicle_id'),('yard_sector','sector_id'),('container','container_id'),
    ('work_order','work_order_id'),('gate_log','gate_log_id'),
    ('plate_recognition','plate_recognition_id'),('work_status_history','history_id'),
    ('exception_log','exception_id'),('trailer_plate_dataset','dataset_id')
  ) AS x(tbl,col)
  LOOP
    IF to_regclass('public.'||r.tbl) IS NOT NULL THEN
      seq_name := pg_get_serial_sequence(r.tbl,r.col);
      IF seq_name IS NOT NULL THEN
        EXECUTE format('SELECT COALESCE(MAX(%I),0) FROM %I',r.col,r.tbl) INTO max_id;
        IF max_id=0 THEN
          PERFORM setval(seq_name,1,false);
        ELSE
          PERFORM setval(seq_name,max_id,true);
        END IF;
      END IF;
    END IF;
  END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS idx_work_order_trailer_status ON work_order(trailer_vehicle_id,work_status);
CREATE INDEX IF NOT EXISTS idx_work_order_driver_status ON work_order(driver_id,work_status);
CREATE INDEX IF NOT EXISTS idx_gate_log_trailer_time ON gate_log(trailer_vehicle_id,entry_time DESC);
CREATE INDEX IF NOT EXISTS idx_plate_recognition_gate ON plate_recognition(gate_log_id);
CREATE INDEX IF NOT EXISTS idx_exception_log_type_time ON exception_log(exception_type,occurred_time DESC);

COMMIT;
