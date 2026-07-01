export const carriers = [
  {
    carrier_id: 1,
    carrier_name: 'Hanjin',
    carrier_contact: '051-111-1111',
    manager_name: 'Kim Manager',
    carrier_status: 'APPROVED',
  },
  {
    carrier_id: 2,
    carrier_name: 'CJ Logistics',
    carrier_contact: '051-222-2222',
    manager_name: 'Lee Manager',
    carrier_status: 'APPROVED',
  },
  {
    carrier_id: 3,
    carrier_name: 'Dongbang',
    carrier_contact: '051-333-3333',
    manager_name: 'Park Manager',
    carrier_status: 'PENDING',
  },
]

export const drivers = [
  {
    driver_id: 1,
    driver_name: 'Kim Chulsoo',
    driver_contact: '010-1000-0001',
    is_registered: true,
    carrier_id: 1,
    can_enter: true,
  },
  {
    driver_id: 2,
    driver_name: 'Lee Younghee',
    driver_contact: '010-1000-0002',
    is_registered: true,
    carrier_id: 2,
    can_enter: true,
  },
  {
    driver_id: 3,
    driver_name: 'Park Minsu',
    driver_contact: '010-1000-0003',
    is_registered: false,
    carrier_id: 3,
    can_enter: false,
  },
]

export const vehicles = [
  {
    vehicle_id: 1,
    plate_number: 'BUSAN80BA1001',
    vehicle_type: 'TRACTOR',
    tonnage: '25T',
    is_registered: true,
    vehicle_status: 'NORMAL',
    tractor_no: 'TR0001',
    chassis_no: 'CH0001',
    carrier_id: 1,
  },
  {
    vehicle_id: 2,
    plate_number: 'BUSAN80BA1002',
    vehicle_type: 'CARGO',
    tonnage: '11T',
    is_registered: true,
    vehicle_status: 'NORMAL',
    tractor_no: 'TR0002',
    chassis_no: 'CH0002',
    carrier_id: 2,
  },
  {
    vehicle_id: 3,
    plate_number: 'BUSAN80BA1003',
    vehicle_type: 'TRACTOR',
    tonnage: '25T',
    is_registered: false,
    vehicle_status: 'RESTRICTED',
    tractor_no: 'TR0003',
    chassis_no: 'CH0003',
    carrier_id: 3,
  },
]

export const yardSectors = [
  {
    sector_id: 1,
    sector_name: 'A-01',
    block_name: 'A',
    sector_status: 'AVAILABLE',
    waiting_vehicle_count: 1,
    guide_message: 'Proceed to A block',
    alt_waiting_area: 'WAIT-A',
  },
  {
    sector_id: 2,
    sector_name: 'B-07',
    block_name: 'B',
    sector_status: 'WORKING',
    waiting_vehicle_count: 2,
    guide_message: 'Use main lane',
    alt_waiting_area: 'WAIT-B',
  },
  {
    sector_id: 3,
    sector_name: 'C-03',
    block_name: 'C',
    sector_status: 'CONGESTED',
    waiting_vehicle_count: 5,
    guide_message: 'Wait before entry',
    alt_waiting_area: 'WAIT-C',
  },
]

export const containers = [
  {
    container_id: 1,
    container_number: 'KDTU1234567',
    container_size: '40FT',
    container_type: 'DRY',
    current_location: 'Block B / Bay 07 / Row 03 / Tier 02',
    sector_id: 2,
    block: 'B',
    bay: '07',
    row_no: '03',
    tier: '02',
    can_exit: true,
    on_hold: false,
    seal_number: 'SEAL-001',
    shipping_line: 'KDT Line',
  },
  {
    container_id: 2,
    container_number: 'KDTU7654321',
    container_size: '20FT',
    container_type: 'REEFER',
    current_location: 'Block C / Bay 03 / Row 01 / Tier 01',
    sector_id: 3,
    block: 'C',
    bay: '03',
    row_no: '01',
    tier: '01',
    can_exit: true,
    on_hold: false,
    seal_number: 'SEAL-002',
    shipping_line: 'KDT Line',
  },
  {
    container_id: 3,
    container_number: 'KDTU3321904',
    container_size: '40FT',
    container_type: 'TANK',
    current_location: 'Block A / Bay 01 / Row 02 / Tier 01',
    sector_id: 1,
    block: 'A',
    bay: '01',
    row_no: '02',
    tier: '01',
    can_exit: false,
    on_hold: true,
    seal_number: 'SEAL-003',
    shipping_line: 'KDT Line',
  },
]

export const workOrders = [
  {
    work_order_id: 1,
    work_type: 'LOAD_OUT',
    vehicle_id: 1,
    driver_id: 1,
    carrier_id: 1,
    container_id: 1,
    reserved_time: '2026-07-01T13:00:00',
    work_status: 'APPROVED',
    is_approved: true,
  },
  {
    work_order_id: 2,
    work_type: 'LOAD_OUT',
    vehicle_id: 2,
    driver_id: 2,
    carrier_id: 2,
    container_id: 2,
    reserved_time: '2026-07-01T14:00:00',
    work_status: 'DISPATCH_WAITING',
    is_approved: false,
  },
  {
    work_order_id: 3,
    work_type: 'UNLOAD_IN',
    vehicle_id: 3,
    driver_id: 3,
    carrier_id: 3,
    container_id: 3,
    reserved_time: '2026-07-01T14:30:00',
    work_status: 'DRIVER_ACCEPTED',
    is_approved: false,
  },
]

export const gateLogs = [
  {
    gate_log_id: 1,
    vehicle_id: 1,
    gate_number: 'G-01',
    gate_name: 'Gate 01',
    entry_time: '2026-07-01T13:02:00',
    exit_time: null,
    in_out_type: 'IN',
    process_result: 'APPROVED',
    manager_check: false,
  },
  {
    gate_log_id: 2,
    vehicle_id: 2,
    gate_number: 'G-01',
    gate_name: 'Gate 01',
    entry_time: '2026-07-01T13:50:00',
    exit_time: null,
    in_out_type: 'IN',
    process_result: 'WAITING',
    manager_check: true,
  },
  {
    gate_log_id: 3,
    vehicle_id: 1,
    gate_number: 'G-03',
    gate_name: 'Gate 03',
    entry_time: null,
    exit_time: '2026-07-01T15:10:00',
    in_out_type: 'OUT',
    process_result: 'APPROVED',
    manager_check: false,
  },
]

export const plateRecognitions = [
  {
    plate_recognition_id: 1,
    gate_log_id: 1,
    vehicle_image: '/images/gate/G-01-001.jpg',
    plate_image: '/images/plate/BUSAN80BA1001.jpg',
    recognized_plate: 'BUSAN80BA1001',
    is_success: true,
    confidence: 0.98,
    manual_correction: false,
    error_message: '',
    recognition_time: '2026-07-01T13:02:00',
  },
  {
    plate_recognition_id: 2,
    gate_log_id: 2,
    vehicle_image: '/images/gate/G-01-002.jpg',
    plate_image: '/images/plate/BUSAN80BA1002.jpg',
    recognized_plate: 'BUSAN80BA1002',
    is_success: true,
    confidence: 0.94,
    manual_correction: false,
    error_message: '',
    recognition_time: '2026-07-01T13:50:00',
  },
]

export const workStatusHistory = [
  {
    history_id: 1,
    work_order_id: 1,
    prev_status: 'DISPATCH_WAITING',
    new_status: 'APPROVED',
    changed_time: '2026-07-01T12:30:00',
    changed_by: 'admin',
    reason: 'Gate reservation approved',
    remark: '',
  },
  {
    history_id: 2,
    work_order_id: 3,
    prev_status: 'DISPATCH_WAITING',
    new_status: 'DRIVER_ACCEPTED',
    changed_time: '2026-07-01T14:00:00',
    changed_by: 'driver',
    reason: 'Driver accepted work order',
    remark: '',
  },
]

export const exceptionLogs = [
  {
    exception_id: 1,
    exception_type: 'SECTOR_CONGESTION',
    exception_message: 'C-03 waiting vehicles exceeded threshold',
    plate_number: 'BUSAN80BA1002',
    occurred_time: '2026-07-01T13:55:00',
    process_status: 'OPEN',
    manager_action: 'Guide to alternate waiting area',
    processed_time: null,
  },
  {
    exception_id: 2,
    exception_type: 'CONTAINER_HOLD',
    exception_message: 'Container is on hold',
    plate_number: 'BUSAN80BA1003',
    occurred_time: '2026-07-01T14:20:00',
    process_status: 'OPEN',
    manager_action: 'Check hold status before exit',
    processed_time: null,
  },
]

const carrierById = new Map(carriers.map((carrier) => [carrier.carrier_id, carrier]))
const driverById = new Map(drivers.map((driver) => [driver.driver_id, driver]))
const vehicleById = new Map(vehicles.map((vehicle) => [vehicle.vehicle_id, vehicle]))
const containerById = new Map(containers.map((container) => [container.container_id, container]))
const sectorById = new Map(yardSectors.map((sector) => [sector.sector_id, sector]))

export const getCarrierName = (carrierId) => carrierById.get(carrierId)?.carrier_name || '-'
export const getDriverName = (driverId) => driverById.get(driverId)?.driver_name || '-'
export const getPlateNumber = (vehicleId) => vehicleById.get(vehicleId)?.plate_number || '-'
export const getContainerNumber = (containerId) => containerById.get(containerId)?.container_number || '-'
export const getContainer = (containerId) => containerById.get(containerId) || null
export const getSectorName = (sectorId) => sectorById.get(sectorId)?.sector_name || '-'
export const getSectorByContainerId = (containerId) => {
  const container = getContainer(containerId)
  return container ? sectorById.get(container.sector_id) || null : null
}

export const availableDrivers = drivers.filter((driver) => driver.is_registered && driver.can_enter)

export const operationStats = [
  {
    label: 'Registered vehicles',
    value: vehicles.filter((vehicle) => vehicle.is_registered).length,
    hint: 'vehicle.is_registered',
  },
  {
    label: 'Drivers can enter',
    value: drivers.filter((driver) => driver.can_enter).length,
    hint: 'driver.can_enter',
  },
  {
    label: 'Approved carriers',
    value: carriers.filter((carrier) => carrier.carrier_status === 'APPROVED').length,
    hint: 'carrier.carrier_status',
  },
  {
    label: 'Open exceptions',
    value: exceptionLogs.filter((log) => log.process_status === 'OPEN').length,
    hint: 'exception_log.process_status',
  },
]

export const driverActions = ['IN', 'LOAD_START', 'LOAD_DONE', 'UNLOAD_START', 'UNLOAD_DONE', 'OUT']
