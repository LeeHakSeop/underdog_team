<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useCarrierStore } from '@/stores/carrierStore'
import { useContainerStore } from '@/stores/adminStore/containerStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'

const workOrderStore = useWorkOrderStore()
const carrierStore = useCarrierStore()
const containerStore = useContainerStore()
const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()
const processingId = ref(null)
const processMessage = ref('')
let refreshTimer = null

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]
const getValue = (row, camelKey, snakeKey) => row?.[camelKey] ?? row?.[snakeKey] ?? ''

const getVehicle = (vehicleId) => {
  return vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId) || null
}

const getCarrierName = (order) => {
  const vehicle = getVehicle(getId(order, 'vehicleId')) || getVehicle(getId(order, 'trailerVehicleId'))
  const carrierId = getId(vehicle, 'carrierId')
  const carrier = carrierStore.carriers.find((item) => getId(item, 'carrierId') === carrierId)
  return getValue(carrier, 'carrierName', 'carrier_name') || '-'
}

const getDriverName = (driverId) => {
  const driver = driverStore.drivers.find((item) => getId(item, 'driverId') === driverId)
  return getValue(driver, 'driverName', 'driver_name') || '-'
}

const getPlateNumber = (vehicleId) => {
  const vehicle = getVehicle(vehicleId)
  return getValue(vehicle, 'plateNumber', 'plate_number') || '-'
}

const getContainer = (containerId) => {
  return containerStore.containers.find((item) => getId(item, 'containerId') === containerId) || null
}

const getContainerNumber = (containerId) => {
  const container = getContainer(containerId)
  return getValue(container, 'containerNumber', 'container_number') || '-'
}

const getYardLocation = (containerId) => {
  const container = getContainer(containerId)
  if (!container) return '-'
  return `${container.block || '-'}-${container.bay || '-'}-${container.rowNo || container.row_no || '-'}`
}

const carrierRequests = computed(() => {
  return workOrderStore.workOrders.filter((order) => {
    const status = getValue(order, 'workStatus', 'work_status')
    const isApproved = getValue(order, 'isApproved', 'is_approved')
    return ['DISPATCH_WAITING', 'REQUESTED', 'PENDING'].includes(status) && isApproved !== true
  })
})

const processingTasks = computed(() => {
  return workOrderStore.workOrders.filter((order) => !carrierRequests.value.includes(order))
})

const processWorkOrder = async (order, action) => {
  const workOrderId = getId(order, 'workOrderId')
  processingId.value = workOrderId
  processMessage.value = ''

  try {
    if (action === 'approve') {
      await workOrderStore.approve(workOrderId)
      processMessage.value = '작업 승인이 완료되었습니다.'
    }

    if (action === 'reject') {
      await workOrderStore.reject(workOrderId)
      processMessage.value = '작업 요청을 반려했습니다.'
    }

    if (action === 'start') {
      await workOrderStore.start(workOrderId)
      processMessage.value = '작업 시작 처리가 완료되었습니다.'
    }

    if (action === 'complete') {
      await workOrderStore.complete(workOrderId)
      processMessage.value = '작업 완료 처리가 완료되었습니다.'
    }
  } catch (error) {
    processMessage.value = error.message || '작업 처리에 실패했습니다.'
  } finally {
    processingId.value = null
  }
}

const getStatusText = (workStatus) => {
  if (workStatus === 'DISPATCH_WAITING') return '승인 대기'
  if (workStatus === 'APPROVED') return '입차 대기'
  if (workStatus === 'GATE_IN') return '입차 완료'
  if (workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (workStatus === 'COMPLETED') return '출차 대기'
  if (workStatus === 'GATE_OUT') return '출차 완료'
  if (workStatus === 'REJECTED') return '반려'
  return workStatus || '-'
}

const getStatusClass = (workStatus) => {
  if (workStatus === 'DISPATCH_WAITING') return 'amber'
  if (workStatus === 'COMPLETED' || workStatus === 'GATE_OUT') return 'green'
  if (workStatus === 'IN_PROGRESS') return 'blue'
  return ''
}

const loadData = () => {
  workOrderStore.loadWorkOrders().catch(() => {})
  carrierStore.loadCarriers().catch(() => {})
  containerStore.loadContainers().catch(() => {})
  driverStore.loadDrivers().catch(() => {})
  vehicleStore.loadVehicles().catch(() => {})
}

onMounted(() => {
  loadData()
  refreshTimer = setInterval(() => {
    if (!workOrderStore.loading && processingId.value === null) {
      workOrderStore.loadWorkOrders().catch(() => {})
    }
  }, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>운송사 작업 요청 승인관리</h2>
        <span class="status-pill amber">배차 대기 {{ carrierRequests.length }}건</span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>운송사</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>예약</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in carrierRequests" :key="getId(order, 'workOrderId')">
              <td>{{ getId(order, 'workOrderId') }}</td>
              <td>{{ getCarrierName(order) }}</td>
              <td>{{ getPlateNumber(getId(order, 'vehicleId') || getId(order, 'trailerVehicleId')) }}</td>
              <td>{{ getDriverName(getId(order, 'driverId')) }}</td>
              <td>{{ getContainerNumber(getId(order, 'containerId')) }}</td>
              <td>{{ getValue(order, 'workType', 'work_type') }}</td>
              <td>{{ getValue(order, 'reservedTime', 'reserved_time') }}</td>
              <td>
                <span class="status-pill amber">
                  {{ getStatusText(getValue(order, 'workStatus', 'work_status')) }}
                </span>
              </td>
              <td>
                <button
                  class="ghost-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'approve')"
                >
                  {{ processingId === getId(order, 'workOrderId') ? '처리 중' : '작업 승인' }}
                </button>
                <button
                  class="ghost-button reject-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'reject')"
                >
                  반려
                </button>
              </td>
            </tr>
            <tr v-if="carrierRequests.length === 0">
              <td colspan="9">배차 대기 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <p v-if="processMessage" class="process-message">{{ processMessage }}</p>

    <section class="panel">
      <div class="section-title">
        <h2>기사 할당 작업 관리</h2>
        <span class="status-pill green">작업 처리 {{ processingTasks.length }}건</span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>컨테이너</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>야드 위치</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in processingTasks" :key="getId(order, 'workOrderId')">
              <td>{{ getId(order, 'workOrderId') }}</td>
              <td>{{ getContainerNumber(getId(order, 'containerId')) }}</td>
              <td>{{ getPlateNumber(getId(order, 'vehicleId') || getId(order, 'trailerVehicleId')) }}</td>
              <td>{{ getDriverName(getId(order, 'driverId')) }}</td>
              <td>{{ getYardLocation(getId(order, 'containerId')) }}</td>
              <td>
                <span class="status-pill" :class="getStatusClass(getValue(order, 'workStatus', 'work_status'))">
                  {{ getStatusText(getValue(order, 'workStatus', 'work_status')) }}
                </span>
              </td>
              <td>
                <button
                  v-if="getValue(order, 'workStatus', 'work_status') === 'GATE_IN'"
                  class="primary-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'start')"
                >
                  {{ processingId === getId(order, 'workOrderId') ? '처리 중' : '작업 시작' }}
                </button>
                <button
                  v-else-if="getValue(order, 'workStatus', 'work_status') === 'IN_PROGRESS'"
                  class="primary-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'complete')"
                >
                  {{ processingId === getId(order, 'workOrderId') ? '처리 중' : '작업 완료' }}
                </button>
                <span v-else>{{ getStatusText(getValue(order, 'workStatus', 'work_status')) }}</span>
              </td>
            </tr>
            <tr v-if="processingTasks.length === 0">
              <td colspan="7">처리 중인 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.process-message {
  margin: 0;
  padding: 10px 12px;
  color: var(--ink-700);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
}

.reject-button {
  margin-left: 6px;
  color: #9f1d1d;
  border-color: #e4a6a6;
}
</style>
