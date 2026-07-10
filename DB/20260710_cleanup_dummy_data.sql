BEGIN;

DELETE FROM plate_recognition;
DELETE FROM exception_log;
DELETE FROM work_status_history;
DELETE FROM gate_log;
DELETE FROM work_order;
DELETE FROM container;
DELETE FROM yard_sector;

DELETE FROM vehicle
WHERE vehicle_id <> 301;

DELETE FROM driver
WHERE driver_id <> 301;

DELETE FROM carrier
WHERE carrier_id <> 5;

DELETE FROM users
WHERE user_id NOT IN (1, 3, 4);

SELECT setval('vehicle_vehicle_id_seq', COALESCE((SELECT MAX(vehicle_id) FROM vehicle), 1), true);
SELECT setval('driver_driver_id_seq', COALESCE((SELECT MAX(driver_id) FROM driver), 1), true);
SELECT setval('carrier_carrier_id_seq', COALESCE((SELECT MAX(carrier_id) FROM carrier), 1), true);
SELECT setval('users_user_id_seq', COALESCE((SELECT MAX(user_id) FROM users), 1), true);
SELECT setval('work_order_work_order_id_seq', COALESCE((SELECT MAX(work_order_id) FROM work_order), 1), true);
SELECT setval('gate_log_gate_log_id_seq', COALESCE((SELECT MAX(gate_log_id) FROM gate_log), 1), true);
SELECT setval('container_container_id_seq', COALESCE((SELECT MAX(container_id) FROM container), 1), true);
SELECT setval('yard_sector_sector_id_seq', COALESCE((SELECT MAX(sector_id) FROM yard_sector), 1), true);
SELECT setval('plate_recognition_plate_recognition_id_seq', COALESCE((SELECT MAX(plate_recognition_id) FROM plate_recognition), 1), true);
SELECT setval('work_status_history_history_id_seq', COALESCE((SELECT MAX(history_id) FROM work_status_history), 1), true);

COMMIT;
