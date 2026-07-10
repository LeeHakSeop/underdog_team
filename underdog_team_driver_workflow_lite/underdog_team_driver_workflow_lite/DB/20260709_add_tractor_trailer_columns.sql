ALTER TABLE work_order
ADD COLUMN IF NOT EXISTS tractor_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
ADD COLUMN IF NOT EXISTS trailer_vehicle_id BIGINT REFERENCES vehicle(vehicle_id);

ALTER TABLE gate_log
ADD COLUMN IF NOT EXISTS tractor_vehicle_id BIGINT REFERENCES vehicle(vehicle_id),
ADD COLUMN IF NOT EXISTS trailer_vehicle_id BIGINT REFERENCES vehicle(vehicle_id);

ALTER TABLE plate_recognition
ADD COLUMN IF NOT EXISTS plate_type VARCHAR(20);
