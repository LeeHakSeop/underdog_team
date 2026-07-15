<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { fetchVehiclesByCarrier } from '@/api/vehicleApi'
import { fetchContainers } from '@/api/adminApi/containerApi'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'
import { vehicleTypeLabel } from '@/config/vehicleType'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
  showInput: {
    type: Boolean,
    default: true,
  },
  showList: {
    type: Boolean,
    default: true,
  },
})

const workOrderStore = useWorkOrderStore()
const currentUser = readCurrentUser()

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')
const editingWorkOrderId = ref(null)

const carriers = ref([])
const drivers = ref([])
const vehicles = ref([])
const containers = ref([])
const workOrderPage = ref(1)
const workOrderPageSize = 10
let refreshTimer = null

const form = ref({
  driverId: null,
  trailerVehicleId: null,
  containerId: null,
  workType: '반출 상차',
  reservedTime: '',
})

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const myDrivers = computed(() =>
  drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value?.carrierId &&
      driver.isRegistered === true,
  ),
)

const activeWorkStatuses = ['DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED']
const isActiveWorkOrder = (order) => activeWorkStatuses.includes(order?.workStatus)

const assignedDriverIds = computed(() => new Set(
  workOrderStore.workOrders
    .filter(isActiveWorkOrder)
    .map((order) => order.driverId)
    .filter(Boolean),
))

const assignedTrailerVehicleIds = computed(() => new Set(
  workOrderStore.workOrders
    .filter(isActiveWorkOrder)
    .map((order) => order.trailerVehicleId)
    .filter(Boolean),
))

const availableDrivers = computed(() => myDrivers.value.filter((driver) => (
  !assignedDriverIds.value.has(driver.driverId) || driver.driverId === form.value.driverId
)))

const isVehicleType = (vehicleType, expectedType) => {
  const normalizedType = String(vehicleType || '').trim().toUpperCase()
  return normalizedType === expectedType || vehicleType === (expectedType === 'TRACTOR' ? '트랙터' : '트레일러')
}

const carrierVehicles = computed(() =>
  vehicles.value.filter(
    (vehicle) => vehicle.carrierId === myCarrier.value?.carrierId && vehicle.isRegistered === true,
  ),
)

const tractorVehicles = computed(() =>
  carrierVehicles.value.filter((vehicle) => isVehicleType(vehicle.vehicleType, 'TRACTOR')),
)

const trailerVehicles = computed(() =>
  carrierVehicles.value.filter((vehicle) => isVehicleType(vehicle.vehicleType, 'TRAILER')),
)

const availableTrailerVehicles = computed(() => trailerVehicles.value.filter((vehicle) => (
  !assignedTrailerVehicleIds.value.has(vehicle.vehicleId)
  || vehicle.vehicleId === form.value.trailerVehicleId
)))

const selectedDriver = computed(() =>
  myDrivers.value.find((driver) => driver.driverId === form.value.driverId),
)

const selectedTractor = computed(() =>
  tractorVehicles.value.find((vehicle) => vehicle.driverId === form.value.driverId),
)

const selectedTrailer = computed(() =>
  trailerVehicles.value.find((vehicle) => vehicle.vehicleId === form.value.trailerVehicleId),
)

const getOrderValue = (order, key) => order?.[key] ?? null

const myWorkOrders = computed(() => {
  const driverIds = new Set(myDrivers.value.map((driver) => driver.driverId))
  const vehicleIds = new Set(carrierVehicles.value.map((vehicle) => vehicle.vehicleId))

  return workOrderStore.workOrders.filter((order) => {
    const driverId = getOrderValue(order, 'driverId')
    const tractorVehicleId = getOrderValue(order, 'tractorVehicleId')
    const trailerVehicleId = getOrderValue(order, 'trailerVehicleId')
    const vehicleId = getOrderValue(order, 'vehicleId')

    return (
      driverIds.has(driverId) ||
      vehicleIds.has(tractorVehicleId) ||
      vehicleIds.has(trailerVehicleId) ||
      vehicleIds.has(vehicleId)
    )
  })
})

const workOrderTotalPages = computed(() =>
  Math.max(1, Math.ceil(myWorkOrders.value.length / workOrderPageSize)),
)

const paginatedWorkOrders = computed(() => {
  const start = (workOrderPage.value - 1) * workOrderPageSize
  return myWorkOrders.value.slice(start, start + workOrderPageSize)
})

const workOrderPageNumbers = computed(() =>
  Array.from({ length: workOrderTotalPages.value }, (_, index) => index + 1),
)

watch(myWorkOrders, () => {
  if (workOrderPage.value > workOrderTotalPages.value) {
    workOrderPage.value = workOrderTotalPages.value
  }
})

const getDriverName = (driverId) =>
  myDrivers.value.find((driver) => driver.driverId === driverId)?.driverName || '-'

const getVehiclePlate = (vehicleId) =>
  carrierVehicles.value.find((vehicle) => vehicle.vehicleId === vehicleId)?.plateNumber || '-'

const getContainerNumber = (containerId) =>
  containers.value.find((container) => container.containerId === containerId)?.containerNumber || '-'

const getStatusText = (status) => {
  const statusMap = {
    DISPATCH_WAITING: '승인 대기',
    APPROVED: '입차 대기',
    GATE_IN: '입차 완료',
    IN_PROGRESS: '작업 진행 중',
    COMPLETED: '출차 대기',
    GATE_OUT: '출차 완료',
    CANCELED: '취소',
  }

  return statusMap[status] || status || '-'
}

const getStatusClass = (status) => {
  if (status === 'DISPATCH_WAITING') return 'amber'
  if (status === 'COMPLETED' || status === 'GATE_OUT') return 'green'
  if (status === 'CANCELED') return 'red'
  return ''
}

const resetForm = () => {
  editingWorkOrderId.value = null
  form.value = {
    driverId: null,
    trailerVehicleId: null,
    containerId: null,
    workType: '반출 상차',
    reservedTime: '',
  }
}

const startEdit = (order) => {
  if (order.workStatus !== 'DISPATCH_WAITING') {
    errorMessage.value = '승인 대기 중인 작업 지시만 수정할 수 있습니다.'
    return
  }

  editingWorkOrderId.value = order.workOrderId
  form.value = {
    driverId: order.driverId,
    trailerVehicleId: order.trailerVehicleId,
    containerId: order.containerId,
    workType: order.workType || '반출 상차',
    reservedTime: order.reservedTime ? String(order.reservedTime).slice(0, 16) : '',
  }
  message.value = ''
  errorMessage.value = ''
}

const removeWorkOrder = async (order) => {
  if (order.workStatus !== 'DISPATCH_WAITING') {
    errorMessage.value = '승인 대기 중인 작업 지시만 삭제할 수 있습니다.'
    return
  }

  if (!window.confirm(`WorkOrder #${order.workOrderId}를 삭제하시겠습니까?`)) return

  try {
    saving.value = true
    await workOrderStore.remove(order.workOrderId)
    if (editingWorkOrderId.value === order.workOrderId) resetForm()
    message.value = `WorkOrder #${order.workOrderId}가 취소되었습니다.`
  } catch (error) {
    errorMessage.value = error.message || '작업 지시 삭제에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [carrierData, driverData, containerData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
      fetchContainers(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
    containers.value = containerData || []

    await loadCarrierVehicles()

    await workOrderStore.loadWorkOrders()
  } catch (error) {
    errorMessage.value = error.message || '작업지시 입력에 필요한 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const loadCarrierVehicles = async () => {
  if (myCarrier.value?.carrierId) {
    vehicles.value = (await fetchVehiclesByCarrier(myCarrier.value.carrierId)) || []
  } else {
    vehicles.value = []
  }
}

const validate = () => {
  if (!myCarrier.value) throw new Error('로그인한 운송사 정보를 찾을 수 없습니다.')
  if (!selectedDriver.value) throw new Error('작업을 배정할 승인 완료 기사를 선택하세요.')
  if (!selectedTractor.value) throw new Error('선택한 기사에게 등록된 트랙터가 없습니다.')
  if (!selectedTrailer.value) throw new Error('트레일러를 선택하세요.')
  if (!form.value.containerId) throw new Error('작업 컨테이너를 선택하세요.')
  if (!form.value.workType) throw new Error('작업 유형을 선택하세요.')
  if (!form.value.reservedTime) throw new Error('작업 예약 시간을 입력하세요.')
}

const submitWorkOrder = async () => {
  message.value = ''
  errorMessage.value = ''

  try {
    validate()
    saving.value = true

    const workOrder = {
      workType: form.value.workType,
      vehicleId: selectedTractor.value.vehicleId,
      tractorVehicleId: selectedTractor.value.vehicleId,
      trailerVehicleId: form.value.trailerVehicleId,
      driverId: form.value.driverId,
      containerId: form.value.containerId,
      reservedTime: form.value.reservedTime,
      workStatus: 'DISPATCH_WAITING',
      isApproved: false,
    }

    if (editingWorkOrderId.value) {
      await workOrderStore.edit(editingWorkOrderId.value, workOrder)
      message.value = '작업 지시가 수정되었습니다. 관리자 승인 대기 상태입니다.'
    } else {
      await workOrderStore.addWorkOrder(workOrder)
      message.value = '기사 작업 지시가 등록되었습니다. 관리자 승인 대기 상태입니다.'
    }

    resetForm()
  } catch (error) {
    errorMessage.value = error.message || '작업지시 등록에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
  refreshTimer = setInterval(() => {
    if (!loading.value && !saving.value && !workOrderStore.loading) {
      loadCarrierVehicles().catch(() => {})
      workOrderStore.loadWorkOrders().catch(() => {})
    }
  }, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div id="work-order-management" class="page-stack">
    <section v-if="!props.embedded" class="panel">
      <div class="section-title">
        <h2>기사 작업지시 관리</h2>
        <div class="section-title-actions">
          <span class="status-pill">{{ myCarrier?.carrierName || '운송사 조회 중' }}</span>
          <a class="ghost-button" href="#assignment-summary">
            배정 정보로 이동
          </a>
        </div>
      </div>

      <div v-if="message" class="form-message success">{{ message }}</div>
      <div v-if="errorMessage" class="form-message error">{{ errorMessage }}</div>
    </section>

    <section v-else-if="message || errorMessage" class="panel">
      <div v-if="message" class="form-message success">{{ message }}</div>
      <div v-if="errorMessage" class="form-message error">{{ errorMessage }}</div>
    </section>

    <section v-if="props.showInput" class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>작업지시 입력</h2>
          <span class="status-pill amber">관리자 승인 요청</span>
        </div>

        <div v-if="loading" class="empty-box">입력 정보를 불러오는 중입니다.</div>

        <form v-else class="form-grid" @submit.prevent="submitWorkOrder">
          <div class="field full">
            <label for="workDriver">배정 기사</label>
            <select id="workDriver" v-model.number="form.driverId">
              <option disabled :value="null">승인 완료 기사를 선택하세요</option>
              <option v-for="driver in availableDrivers" :key="driver.driverId" :value="driver.driverId">
                {{ driver.driverName }} / {{ driver.driverContact || '-' }}
              </option>
            </select>
          </div>

          <div class="field full auto-vehicle-field">
            <label>기사 소유 트랙터</label>
            <div class="auto-vehicle-value">
              <strong>{{ selectedTractor?.plateNumber || '기사 선택 후 자동 연결' }}</strong>
              <span>{{ selectedTractor?.tractorNo || '등록된 트랙터 번호 없음' }}</span>
            </div>
            <small>트랙터는 기사 소유 차량으로 자동 연결됩니다. 차량 변경은 기사 페이지에서 수정합니다.</small>
          </div>

          <div class="field">
            <label for="trailerVehicle">트레일러</label>
            <select id="trailerVehicle" v-model.number="form.trailerVehicleId">
              <option disabled :value="null">트레일러를 선택하세요</option>
              <option v-for="vehicle in availableTrailerVehicles" :key="vehicle.vehicleId" :value="vehicle.vehicleId">
                {{ vehicle.plateNumber }} / {{ vehicleTypeLabel(vehicle.vehicleType) }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="containerId">컨테이너</label>
            <select id="containerId" v-model.number="form.containerId">
              <option disabled :value="null">컨테이너를 선택하세요</option>
              <option v-for="container in containers" :key="container.containerId" :value="container.containerId">
                {{ container.containerNumber }} / {{ container.containerLocation || '-' }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="workType">작업 유형</label>
            <select id="workType" v-model="form.workType">
              <option value="반출 상차">반출 상차</option>
              <option value="반입 하차">반입 하차</option>
            </select>
          </div>

          <div class="field full">
            <label for="reservedTime">예약 시간</label>
            <input id="reservedTime" v-model="form.reservedTime" type="datetime-local" />
          </div>

          <button class="primary-button full" type="submit" :disabled="saving">
            {{ saving ? '처리 중...' : (editingWorkOrderId ? '작업 지시 수정' : '기사에게 작업 지시 등록') }}
          </button>
          <button v-if="editingWorkOrderId" class="ghost-button full" type="button" @click="resetForm">
            수정 취소
          </button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>선택 정보</h2>
          <span class="status-pill green">{{ myWorkOrders.length }}건</span>
        </div>

        <div class="selection-summary">
          <div><span>기사</span><strong>{{ selectedDriver?.driverName || '-' }}</strong></div>
          <div><span>트랙터</span><strong>{{ selectedTractor?.plateNumber || '-' }}</strong></div>
          <div><span>트레일러</span><strong>{{ selectedTrailer?.plateNumber || '-' }}</strong></div>
          <div><span>컨테이너</span><strong>{{ getContainerNumber(form.containerId) }}</strong></div>
          <div><span>작업 유형</span><strong>{{ form.workType || '-' }}</strong></div>
          <div><span>등록 상태</span><strong>관리자 승인 대기</strong></div>
        </div>

        <div class="notice-box">
          작업지시 등록 후 상태는 배차 대기입니다. 관리자가 승인하면 기사의 작업 현황에 표시되고 입차 처리를 진행할 수 있습니다.
        </div>
      </article>
    </section>

    <section v-if="props.showList" class="panel">
      <div class="section-title">
        <h2>작업지시 목록</h2>
        <span class="status-pill">배정 기록 · 실시간 조회</span>
      </div>

      <div v-if="myWorkOrders.length === 0" class="empty-box">
        등록된 작업지시가 없습니다.
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>WorkOrder</th>
              <th>기사</th>
              <th>트랙터</th>
              <th>트레일러</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>예약 시간</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in paginatedWorkOrders" :key="order.workOrderId">
              <td>#{{ order.workOrderId }}</td>
              <td>{{ getDriverName(order.driverId) }}</td>
              <td>{{ getVehiclePlate(order.tractorVehicleId || order.vehicleId) }}</td>
              <td>{{ getVehiclePlate(order.trailerVehicleId) }}</td>
              <td>{{ getContainerNumber(order.containerId) }}</td>
              <td>{{ order.workType || '-' }}</td>
              <td>{{ order.reservedTime || '-' }}</td>
              <td>
                <span class="status-pill" :class="getStatusClass(order.workStatus)">
                  {{ getStatusText(order.workStatus) }}
                </span>
              </td>
              <td class="row-actions">
                <template v-if="order.workStatus === 'DISPATCH_WAITING'">
                  <button class="ghost-button" type="button" @click="startEdit(order)">수정</button>
                  <button class="ghost-button danger-action" type="button" :disabled="saving" @click="removeWorkOrder(order)">삭제</button>
                </template>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="myWorkOrders.length > 0" class="pagination" aria-label="작업지시 목록 페이지">
        <button class="ghost-button" type="button" :disabled="workOrderPage === 1" @click="workOrderPage -= 1">
          이전
        </button>
        <button
          v-for="page in workOrderPageNumbers"
          :key="page"
          class="page-button"
          :class="{ active: workOrderPage === page }"
          type="button"
          @click="workOrderPage = page"
        >
          {{ page }}
        </button>
        <button class="ghost-button" type="button" :disabled="workOrderPage === workOrderTotalPages" @click="workOrderPage += 1">
          다음
        </button>
        <span>{{ workOrderPage }} / {{ workOrderTotalPages }} 페이지</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.form-message {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  font-weight: 700;
}

.form-message.success {
  color: #155e3b;
  background: #ecfdf3;
  border-color: #b7ebc9;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border-color: #fecaca;
}

.section-title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 10px;
}

.pagination > span {
  margin-left: 5px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.page-button {
  min-width: 30px;
  min-height: 30px;
  color: var(--ink-700);
  background: #f1f5f9;
  border: 1px solid var(--line);
  border-radius: 2px;
  font-weight: 700;
}

.page-button.active {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.full {
  grid-column: 1 / -1;
}

.empty-box {
  padding: 22px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 700;
}

.selection-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.selection-summary div {
  display: grid;
  gap: 4px;
  padding: 11px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.selection-summary span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.selection-summary strong {
  overflow-wrap: anywhere;
  font-size: 14px;
}

.auto-vehicle-field {
  padding: 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.auto-vehicle-value {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-height: 36px;
  padding: 8px 10px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.auto-vehicle-value span,
.auto-vehicle-field small {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.auto-vehicle-field small {
  display: block;
  margin-top: 6px;
}

.notice-box {
  margin-top: 12px;
  padding: 12px;
  color: var(--ink-700);
  background: #fff8e7;
  border: 1px solid #ffe0a6;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.row-actions {
  display: flex;
  gap: 5px;
  white-space: nowrap;
}

.danger-action {
  color: #a23a35;
  border-color: #f4bdb9;
}

@media (max-width: 760px) {
  .section-title-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .selection-summary {
    grid-template-columns: 1fr;
  }
}
</style>
