-- 동기화 후 프로젝트 핵심 DB 상태 확인

SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

SELECT
    'exception_log' AS table_name,
    COUNT(*) AS row_count,
    COUNT(*) FILTER (WHERE exception_log_id IS NULL) AS missing_id,
    COUNT(*) FILTER (WHERE occurred_time IS NULL) AS missing_occurred_time
FROM exception_log;

SELECT
    (SELECT COUNT(*) FROM pm_equipment) AS equipment_count,
    (SELECT COUNT(*) FROM pm_sensor_data) AS sensor_data_count,
    (SELECT COUNT(*) FROM pm_event) AS event_count;

SELECT
    conrelid::regclass AS table_name,
    conname,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid IN (
      'exception_log'::regclass,
      'pm_equipment'::regclass,
      'pm_sensor_data'::regclass,
      'pm_event'::regclass
  )
ORDER BY conrelid::regclass::TEXT, conname;
