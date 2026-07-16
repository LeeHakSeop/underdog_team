BEGIN;

UPDATE plate_recognition p
SET recognized_plate = v.plate_number
FROM gate_log g
JOIN vehicle v
  ON v.vehicle_id = COALESCE(g.tractor_vehicle_id, g.vehicle_id)
WHERE p.gate_log_id = g.gate_log_id
  AND UPPER(TRIM(v.vehicle_type)) IN ('트랙터', 'TRACTOR')
  AND p.recognized_plate IS DISTINCT FROM v.plate_number;

COMMIT;