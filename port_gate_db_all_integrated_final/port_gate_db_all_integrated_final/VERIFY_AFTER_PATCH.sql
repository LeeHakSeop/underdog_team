SELECT 'trailer_plate_dataset' AS item, COUNT(*) AS count FROM trailer_plate_dataset
UNION ALL SELECT 'operational trailers',COUNT(*) FROM vehicle WHERE vehicle_type IN('트레일러','TRAILER')
UNION ALL SELECT '2026-07-15 work orders',COUNT(*) FROM work_order WHERE reserved_time::date=DATE '2026-07-15'
UNION ALL SELECT '2026-07-15 gate logs',COUNT(*) FROM gate_log WHERE entry_time::date=DATE '2026-07-15'
UNION ALL SELECT '2026-07-15 OCR logs',COUNT(*) FROM plate_recognition WHERE recognition_time::date=DATE '2026-07-15'
UNION ALL SELECT 'exception logs',COUNT(*) FROM exception_log;

SELECT trigger_name,event_manipulation,event_object_table
FROM information_schema.triggers
WHERE trigger_name='trg_plate_recognition_exception';
