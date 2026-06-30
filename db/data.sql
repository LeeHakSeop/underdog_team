SET search_path TO port_gate;

INSERT INTO roles (role_code, role_name, description)
VALUES
    ('ADMIN', 'Administrator', 'System administrator'),
    ('OPERATOR', 'Operator', 'Gate and yard operator'),
    ('CARRIER', 'Carrier', 'Carrier company user')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO users (username, password, name, email, phone, role_code)
VALUES
    ('admin', '1234', 'Admin User', 'admin@port.local', '010-0000-0001', 'ADMIN'),
    ('operator01', '1234', 'Gate Operator', 'operator01@port.local', '010-0000-0002', 'OPERATOR')
ON CONFLICT (username) DO NOTHING;

INSERT INTO carriers (carrier_name, business_no, phone, manager_name, address, approval_status)
VALUES
    ('Busan Logistics', '101-81-00001', '051-111-2222', 'Park Manager', 'Busan Port Road 10', 'APPROVED'),
    ('Korea Freight', '101-81-00002', '051-333-4444', 'Choi Manager', 'Busan Port Road 20', 'APPROVED')
ON CONFLICT (business_no) DO NOTHING;

INSERT INTO vehicles (
    vehicle_no,
    vehicle_type,
    tonnage,
    is_registered,
    vehicle_status,
    tractor_no,
    chassis_no,
    carrier_id
)
VALUES
    (
        '12GA3456',
        'TRACTOR',
        25.00,
        'Y',
        'NORMAL',
        'TR-001',
        'CH-001',
        (SELECT carrier_id FROM carriers WHERE business_no = '101-81-00001')
    ),
    (
        '34NA7890',
        'TRACTOR',
        25.00,
        'Y',
        'NORMAL',
        'TR-002',
        'CH-002',
        (SELECT carrier_id FROM carriers WHERE business_no = '101-81-00002')
    )
ON CONFLICT (vehicle_no) DO NOTHING;

INSERT INTO drivers (driver_name, phone, is_registered, carrier_id, can_enter)
VALUES
    (
        'Kim Driver',
        '010-1111-2222',
        'Y',
        (SELECT carrier_id FROM carriers WHERE business_no = '101-81-00001'),
        'Y'
    ),
    (
        'Lee Driver',
        '010-3333-4444',
        'Y',
        (SELECT carrier_id FROM carriers WHERE business_no = '101-81-00002'),
        'Y'
    )
ON CONFLICT (driver_name, phone, carrier_id) DO NOTHING;

INSERT INTO yard_sectors (
    sector_code,
    sector_name,
    block_name,
    sector_status,
    waiting_vehicle_count,
    guide_message,
    alternative_waiting_area
)
VALUES
    ('A-01', 'Yard A Sector 01', 'A', 'AVAILABLE', 1, 'Move straight after gate-in and wait at A-01.', 'WAIT-A'),
    ('B-07', 'Yard B Sector 07', 'B', 'AVAILABLE', 2, 'Use inner road 2 and wait at B-07.', 'WAIT-B'),
    ('C-03', 'Yard C Sector 03', 'C', 'CONGESTED', 5, 'Reefer loading sector is busy. Follow operator guidance.', 'WAIT-C')
ON CONFLICT (sector_code) DO NOTHING;

INSERT INTO containers (
    container_no,
    size_type,
    container_type,
    current_location,
    sector_id,
    block_name,
    bay,
    row_no,
    tier,
    can_release,
    is_hold,
    seal_no,
    shipping_company
)
VALUES
    (
        'KDTU1234567',
        '40FT',
        'DRY',
        'Block B / Bay 07 / Row 03 / Tier 02',
        (SELECT sector_id FROM yard_sectors WHERE sector_code = 'B-07'),
        'B',
        '07',
        '03',
        '02',
        'Y',
        'N',
        'SEAL-001',
        'KDT Shipping'
    ),
    (
        'KDTU7654321',
        '20FT',
        'REEFER',
        'Block C / Bay 03 / Row 01 / Tier 01',
        (SELECT sector_id FROM yard_sectors WHERE sector_code = 'C-03'),
        'C',
        '03',
        '01',
        '01',
        'Y',
        'N',
        'SEAL-002',
        'KDT Shipping'
    )
ON CONFLICT (container_no) DO NOTHING;

INSERT INTO gates (gate_code, gate_name, gate_type)
VALUES
    ('IN-01', 'Gate In 01', 'IN'),
    ('OUT-01', 'Gate Out 01', 'OUT')
ON CONFLICT (gate_code) DO NOTHING;

INSERT INTO work_orders (
    order_no,
    work_type,
    vehicle_id,
    driver_id,
    carrier_id,
    container_id,
    sector_id,
    gate_in_id,
    gate_out_id,
    reservation_time,
    work_status,
    is_approved,
    approved_by,
    approved_at,
    created_by
)
VALUES
    (
        'WO-20260630-001',
        'LOAD',
        (SELECT vehicle_id FROM vehicles WHERE vehicle_no = '12GA3456'),
        (SELECT driver_id FROM drivers WHERE driver_name = 'Kim Driver' ORDER BY driver_id LIMIT 1),
        (SELECT carrier_id FROM carriers WHERE business_no = '101-81-00001'),
        (SELECT container_id FROM containers WHERE container_no = 'KDTU1234567'),
        (SELECT sector_id FROM yard_sectors WHERE sector_code = 'B-07'),
        (SELECT gate_id FROM gates WHERE gate_code = 'IN-01'),
        (SELECT gate_id FROM gates WHERE gate_code = 'OUT-01'),
        TIMESTAMP '2026-06-30 13:00:00',
        'GATE_IN',
        'Y',
        (SELECT user_id FROM users WHERE username = 'admin'),
        TIMESTAMP '2026-06-30 12:30:00',
        (SELECT user_id FROM users WHERE username = 'admin')
    ),
    (
        'WO-20260630-002',
        'LOAD',
        (SELECT vehicle_id FROM vehicles WHERE vehicle_no = '34NA7890'),
        (SELECT driver_id FROM drivers WHERE driver_name = 'Lee Driver' ORDER BY driver_id LIMIT 1),
        (SELECT carrier_id FROM carriers WHERE business_no = '101-81-00002'),
        (SELECT container_id FROM containers WHERE container_no = 'KDTU7654321'),
        (SELECT sector_id FROM yard_sectors WHERE sector_code = 'C-03'),
        (SELECT gate_id FROM gates WHERE gate_code = 'IN-01'),
        (SELECT gate_id FROM gates WHERE gate_code = 'OUT-01'),
        TIMESTAMP '2026-06-30 14:00:00',
        'WAITING',
        'N',
        NULL,
        NULL,
        (SELECT user_id FROM users WHERE username = 'operator01')
    )
ON CONFLICT (order_no) DO NOTHING;

INSERT INTO gate_logs (
    order_id,
    gate_id,
    vehicle_id,
    in_out_type,
    gate_time,
    process_result,
    need_admin_check
)
SELECT
    wo.order_id,
    g.gate_id,
    v.vehicle_id,
    'IN',
    TIMESTAMP '2026-06-30 13:02:00',
    'APPROVED',
    'N'
FROM work_orders wo
JOIN gates g ON g.gate_code = 'IN-01'
JOIN vehicles v ON v.vehicle_no = '12GA3456'
WHERE wo.order_no = 'WO-20260630-001'
  AND NOT EXISTS (
      SELECT 1
      FROM gate_logs gl
      WHERE gl.order_id = wo.order_id
        AND gl.gate_id = g.gate_id
        AND gl.in_out_type = 'IN'
  );

INSERT INTO plate_recognitions (
    gate_log_id,
    gate_id,
    vehicle_image_path,
    plate_image_path,
    recognized_vehicle_no,
    is_success,
    confidence,
    corrected_vehicle_no,
    error_message,
    recognized_at
)
SELECT
    gl.gate_log_id,
    gl.gate_id,
    '/images/vehicles/12GA3456.jpg',
    '/images/plates/12GA3456.jpg',
    '12GA3456',
    'Y',
    98.50,
    NULL,
    NULL,
    TIMESTAMP '2026-06-30 13:01:50'
FROM gate_logs gl
JOIN work_orders wo ON wo.order_id = gl.order_id
WHERE wo.order_no = 'WO-20260630-001'
  AND gl.in_out_type = 'IN'
  AND NOT EXISTS (
      SELECT 1
      FROM plate_recognitions pr
      WHERE pr.gate_log_id = gl.gate_log_id
  );

INSERT INTO work_status_histories (
    order_id,
    previous_status,
    changed_status,
    changed_at,
    changed_by,
    change_reason,
    memo
)
SELECT
    wo.order_id,
    'WAITING',
    'GATE_IN',
    TIMESTAMP '2026-06-30 13:02:00',
    (SELECT user_id FROM users WHERE username = 'operator01'),
    'Vehicle entered through gate',
    'Plate recognition matched the approved work order.'
FROM work_orders wo
WHERE wo.order_no = 'WO-20260630-001'
  AND NOT EXISTS (
      SELECT 1
      FROM work_status_histories wsh
      WHERE wsh.order_id = wo.order_id
        AND wsh.changed_status = 'GATE_IN'
  );

INSERT INTO exception_logs (
    exception_type,
    exception_message,
    vehicle_no,
    order_id,
    occurred_at,
    process_status,
    admin_action,
    processed_by,
    processed_at
)
SELECT
    'SECTOR_CONGESTED',
    'Assigned sector C-03 is congested. Alternative waiting area guidance is required.',
    '34NA7890',
    wo.order_id,
    TIMESTAMP '2026-06-30 13:50:00',
    'PROCESSING',
    'Guide driver to WAIT-C until sector is available.',
    (SELECT user_id FROM users WHERE username = 'operator01'),
    TIMESTAMP '2026-06-30 13:55:00'
FROM work_orders wo
WHERE wo.order_no = 'WO-20260630-002'
  AND NOT EXISTS (
      SELECT 1
      FROM exception_logs el
      WHERE el.order_id = wo.order_id
        AND el.exception_type = 'SECTOR_CONGESTED'
  );
