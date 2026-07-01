import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { useContainerStore } from '@/stores/adminStore/containerStore'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useTaskStore } from '@/stores/adminStore/taskStore'
import { useYardSectorStore } from '@/stores/adminStore/yardSectorStore'

const driverActions = ['IN', 'LOAD_START', 'LOAD_DONE', 'UNLOAD_START', 'UNLOAD_DONE', 'OUT']

const normalizeCarrier = (carrier) => ({
  ...carrier,
  carrier_id: carrier.carrierId,
  carrier_name: carrier.carrierName,
  carrier_contact: carrier.carrierContact,
  manager_name: carrier.managerName,
  carrier_status: carrier.carrierStatus,
})

const normalizeDriver = (driver) => ({
  ...driver,
  driver_id: driver.driverId,
  driver_name: driver.driverName,
  driver_contact: driver.driverContact,
  is_registered: driver.isRegistered,
  carrier_id: driver.carrierId,
  can_enter: driver.canEnter,
})

const normalizeVehicle = (vehicle) => ({
  ...vehicle,
  vehicle_id: vehicle.vehicleId,
  plate_number: vehicle.plateNumber,
  vehicle_type: vehicle.vehicleType,
  is_registered: vehicle.isRegistered,
  vehicle_status: vehicle.vehicleStatus,
  tractor_no: vehicle.tractorNo,
  chassis_no: vehicle.chassisNo,
  carrier_id: vehicle.carrierId,
})

const normalizeContainer = (container) => ({
  ...container,
  container_id: container.containerId,
  container_number: container.containerNumber,
  container_size: container.containerSize,
  container_location: container.containerLocation,
  current_location: container.containerLocation,
  container_type: container.shippingLine,
  sector_id: container.sectorId,
  row_no: container.rowNo,
  can_exit: container.canExit,
  seal_number: container.sealNumber,
  shipping_line: container.shippingLine,
  on_hold: !container.canExit,
})

const normalizeGateLog = (log) => ({
  ...log,
  gate_log_id: log.gateLogId,
  vehicle_id: log.vehicleId,
  gate_number: log.gateNumber,
  gate_name: log.gateName,
  entry_time: log.entryTime,
  exit_time: log.exitTime,
  in_out_type: log.inOutType,
  process_result: log.processResult,
  manager_check: log.managerCheck,
})

const normalizeWorkOrder = (order, vehicleById) => {
  const vehicle = vehicleById.get(order.vehicleId)
  return {
    ...order,
    work_order_id: order.workOrderId,
    work_type: order.workType,
    vehicle_id: order.vehicleId,
    driver_id: order.driverId,
    carrier_id: vehicle?.carrierId,
    container_id: order.containerId,
    reserved_time: order.reservedTime,
    work_status: order.workStatus,
    is_approved: order.isApproved,
  }
}

const normalizeSector = (sector) => ({
  ...sector,
  sector_id: sector.sectorId,
  sector_name: sector.sectorName,
  block_name: sector.blockName,
  sector_status: sector.sectorStatus,
  waiting_vehicle_count: sector.waitingVehicleCount,
  guide_message: sector.guideMessage,
  alt_waiting_area: sector.altWaitingArea,
})

export const useLogisticsData = () => {
  const carrierStore = useCarrierStore()
  const driverStore = useDriverStore()
  const vehicleStore = useVehicleStore()
  const containerStore = useContainerStore()
  const gateLogStore = useGateLogStore()
  const taskStore = useTaskStore()
  const yardSectorStore = useYardSectorStore()

  const { carriers: rawCarriers } = storeToRefs(carrierStore)
  const { drivers: rawDrivers } = storeToRefs(driverStore)
  const { vehicles: rawVehicles } = storeToRefs(vehicleStore)
  const { containers: rawContainers } = storeToRefs(containerStore)
  const { gateLogs: rawGateLogs } = storeToRefs(gateLogStore)
  const { tasks: rawTasks } = storeToRefs(taskStore)
  const { yardSectors: rawYardSectors } = storeToRefs(yardSectorStore)

  const loadAll = () => {
    carrierStore.loadCarriers()
    driverStore.loadDrivers()
    vehicleStore.loadVehicles()
    containerStore.loadContainers()
    gateLogStore.loadGateLogs()
    taskStore.loadTasks()
    yardSectorStore.loadYardSectors()
  }

  onMounted(loadAll)

  const carriers = computed(() => rawCarriers.value.map(normalizeCarrier))
  const drivers = computed(() => rawDrivers.value.map(normalizeDriver))
  const vehicles = computed(() => rawVehicles.value.map(normalizeVehicle))
  const containers = computed(() => rawContainers.value.map(normalizeContainer))
  const gateLogs = computed(() => rawGateLogs.value.map(normalizeGateLog))
  const yardSectors = computed(() => rawYardSectors.value.map(normalizeSector))
  const vehicleById = computed(() => new Map(rawVehicles.value.map((vehicle) => [vehicle.vehicleId, vehicle])))
  const workOrders = computed(() => rawTasks.value.map((order) => normalizeWorkOrder(order, vehicleById.value)))

  const getCarrierName = (carrierId) => carriers.value.find((carrier) => carrier.carrier_id === carrierId)?.carrier_name || '-'
  const getDriverName = (driverId) => drivers.value.find((driver) => driver.driver_id === driverId)?.driver_name || '-'
  const getPlateNumber = (vehicleId) => vehicles.value.find((vehicle) => vehicle.vehicle_id === vehicleId)?.plate_number || '-'
  const getContainer = (containerId) => containers.value.find((container) => container.container_id === containerId) || null
  const getContainerNumber = (containerId) => containers.value.find((container) => container.container_id === containerId)?.container_number || '-'
  const getSectorName = (sectorId) => yardSectors.value.find((sector) => sector.sector_id === sectorId)?.sector_name || '-'
  const getSectorByContainerId = (containerId) => {
    const container = containers.value.find((item) => item.container_id === containerId)
    return yardSectors.value.find((sector) => sector.sector_id === container?.sector_id) || null
  }

  const availableDrivers = computed(() => drivers.value.filter((driver) => driver.is_registered && driver.can_enter))
  const operationStats = computed(() => [
    { label: '작업', value: workOrders.value.length, hint: '전체 작업' },
    { label: '차량', value: vehicles.value.length, hint: '등록 차량' },
    { label: '컨테이너', value: containers.value.length, hint: '관리 대상' },
    { label: '게이트', value: gateLogs.value.length, hint: '출입 기록' },
  ])

  return {
    availableDrivers,
    carriers,
    containers,
    driverActions,
    drivers,
    gateLogs,
    getCarrierName,
    getContainer,
    getContainerNumber,
    getDriverName,
    getPlateNumber,
    getSectorByContainerId,
    getSectorName,
    loadAll,
    operationStats,
    vehicles,
    workOrders,
    yardSectors,
  }
}
