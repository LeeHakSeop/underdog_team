BEGIN;

UPDATE exception_log e
SET plate_number = v.plate_number
FROM gate_log g
JOIN vehicle v
  ON v.vehicle_id = COALESCE(g.tractor_vehicle_id, g.vehicle_id)
WHERE e.gate_log_id = g.gate_log_id
  AND UPPER(TRIM(v.vehicle_type)) IN ('트랙터', 'TRACTOR')
  AND e.plate_number IS DISTINCT FROM v.plate_number;

COMMIT;