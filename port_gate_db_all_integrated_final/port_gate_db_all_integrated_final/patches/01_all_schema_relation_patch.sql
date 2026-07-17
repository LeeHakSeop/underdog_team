-- 기존 DB 구조 보정: 사용자-기사-운송사-차량, 트랙터/트레일러 컬럼
BEGIN;

ALTER TABLE carrier ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE driver ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS driver_id BIGINT;
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE work_order ADD COLUMN IF NOT EXISTS tractor_vehicle_id BIGINT;
ALTER TABLE work_order ADD COLUMN IF NOT EXISTS trailer_vehicle_id BIGINT;
ALTER TABLE gate_log ADD COLUMN IF NOT EXISTS tractor_vehicle_id BIGINT;
ALTER TABLE gate_log ADD COLUMN IF NOT EXISTS trailer_vehicle_id BIGINT;
ALTER TABLE plate_recognition ADD COLUMN IF NOT EXISTS plate_type VARCHAR(20);

DO $$
BEGIN
  IF to_regclass('public.users') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_carrier_user') THEN
    ALTER TABLE carrier ADD CONSTRAINT fk_carrier_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE SET NULL;
  END IF;
  IF to_regclass('public.users') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_driver_user') THEN
    ALTER TABLE driver ADD CONSTRAINT fk_driver_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE SET NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_vehicle_driver') THEN
    ALTER TABLE vehicle ADD CONSTRAINT fk_vehicle_driver FOREIGN KEY(driver_id) REFERENCES driver(driver_id) ON DELETE SET NULL;
  END IF;
  IF to_regclass('public.users') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_vehicle_user') THEN
    ALTER TABLE vehicle ADD CONSTRAINT fk_vehicle_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE SET NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_work_order_tractor_vehicle') THEN
    ALTER TABLE work_order ADD CONSTRAINT fk_work_order_tractor_vehicle FOREIGN KEY(tractor_vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE SET NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_work_order_trailer_vehicle') THEN
    ALTER TABLE work_order ADD CONSTRAINT fk_work_order_trailer_vehicle FOREIGN KEY(trailer_vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE SET NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_gate_log_tractor_vehicle') THEN
    ALTER TABLE gate_log ADD CONSTRAINT fk_gate_log_tractor_vehicle FOREIGN KEY(tractor_vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE SET NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_gate_log_trailer_vehicle') THEN
    ALTER TABLE gate_log ADD CONSTRAINT fk_gate_log_trailer_vehicle FOREIGN KEY(trailer_vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE SET NULL;
  END IF;
END $$;

COMMIT;
