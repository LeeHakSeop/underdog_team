-- Run after DB_Create.sql.
-- This migration keeps vehicle as the master table and does not change
-- existing work_order, gate_log, driver, or carrier foreign keys.

BEGIN;

DO $$
DECLARE
    unexpected_types TEXT;
BEGIN
    SELECT string_agg(type_value, ', ' ORDER BY type_value)
    INTO unexpected_types
    FROM (
        SELECT DISTINCT COALESCE(vehicle_type, '<NULL>') AS type_value
        FROM vehicle
        WHERE vehicle_type IS NULL
           OR vehicle_type NOT IN ('TRACTOR', 'TRAILER', U&'\D2B8\B808\C77C\B7EC')
    ) unexpected;

    IF unexpected_types IS NOT NULL THEN
        RAISE EXCEPTION 'Unexpected vehicle_type values: %', unexpected_types;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS tractor (
    vehicle_id BIGINT PRIMARY KEY,
    CONSTRAINT tractor_vehicle_id_fkey
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicle(vehicle_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trailer (
    vehicle_id BIGINT PRIMARY KEY,
    CONSTRAINT trailer_vehicle_id_fkey
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicle(vehicle_id)
        ON DELETE CASCADE
);

UPDATE vehicle
SET vehicle_type = 'TRAILER'
WHERE vehicle_type = U&'\D2B8\B808\C77C\B7EC';

INSERT INTO tractor (vehicle_id)
SELECT vehicle_id
FROM vehicle
WHERE vehicle_type = 'TRACTOR'
ON CONFLICT (vehicle_id) DO NOTHING;

INSERT INTO trailer (vehicle_id)
SELECT vehicle_id
FROM vehicle
WHERE vehicle_type = 'TRAILER'
ON CONFLICT (vehicle_id) DO NOTHING;

COMMIT;
