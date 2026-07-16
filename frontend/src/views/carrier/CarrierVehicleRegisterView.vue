<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { createVehicle, fetchVehiclesByCarrier, updateVehicle } from '@/api/vehicleApi'
import { fetchTrailerWorkInfo, fetchWorkOrders } from '@/api/adminApi/workOrderApi'
import { vehicleTypeLabel } from '@/config/vehicleType'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
  showSummary: {
    type: Boolean,
    default: true,
  },
  showInput: {
    type: Boolean,
    default: true,
  },
})

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')

const carriers = ref([])
const drivers = ref([])
const assignedVehicles = ref([])
const workOrders = ref([])
const trailerWorkInfos = ref({})
const workOrderLoading = ref(false)
const workOrderError = ref('')
const driverSearch = ref('')
const currentPage = ref(1)
const editingVehicleId = ref(null)
const editPlateNumber = ref('')
const pageSize = 10
let refreshTimer = null

const currentUser = readCurrentUser()

const vehicleTypes = ['트레일러']
const tonnageOptions = ['1톤', '2.5톤', '5톤', '8.5톤', '11톤', '18톤', '25톤']

const form = ref({
  driverId: null,
  plateNumber: '',
  vehicleType: '트레일러',
  tonnage: '25톤',
  chassisNo: '',
})

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const myDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) => driver.carrierId === myCarrier.value.carrierId,
  )
})

const assignableDrivers = computed(() =>
  myDrivers.value.filter(
    (driver) =>
      driver.userStatus !== 'WITHDRAWN' &&
      driver.isRegistered === true &&
      // 트랙터가 이미 최종 승인된 기사도 트레일러를 추가 배정할 수 있어야 한다.
      // canEnter는 출입 승인 상태이므로 트레일러 배정 가능 여부로 제한하지 않는다.
      driver.driverId != null,
  ),
)

const selectedDriver = computed(() =>
  assignableDrivers.value.find((driver) => driver.driverId === form.value.driverId),
)

const isTrailerVehicle = (vehicleType) => {
  const normalizedType = String(vehicleType || '').trim().toUpperCase()
  return normalizedType === 'TRAILER' || vehicleType === '트레일러'
}

const getWorkOrderForVehicle = (vehicleId) => {
  const trailerInfo = trailerWorkInfos.value[vehicleId]
  const workOrder = workOrders.value.find(
    (order) =>
      order.trailerVehicleId === vehicleId || order.vehicleId === vehicleId,
  ) || null

  if (trailerInfo?.workOrderId) {
    // 상세 작업정보와 목록 조회 정보가 서로 다른 DTO이므로
    // 예약 시간·승인 상태 같은 목록 필드를 함께 유지한다.
    return { ...workOrder, ...trailerInfo }
  }

  return workOrder
}

const assignedTrailerSummaries = computed(() => {
  return assignedVehicles.value
    .map((vehicle) => {
      const workOrder = getWorkOrderForVehicle(vehicle.vehicleId)
      const driverId = vehicle.driverId || workOrder?.driverId
      const driver = drivers.value.find((item) => item.driverId === driverId)

      return {
        vehicle,
        driver,
        workOrder,
      }
    })
    .sort((left, right) => (right.workOrder?.workOrderId || 0) - (left.workOrder?.workOrderId || 0))
})

const filteredAssignedTrailerSummaries = computed(() => {
  const keyword = driverSearch.value.trim().toLowerCase()

  if (!keyword) return assignedTrailerSummaries.value

  return assignedTrailerSummaries.value.filter((summary) =>
    String(summary.driver?.driverName || '').toLowerCase().includes(keyword),
  )
})

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredAssignedTrailerSummaries.value.length / pageSize)),
)

const paginatedAssignedTrailerSummaries = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAssignedTrailerSummaries.value.slice(start, start + pageSize)
})

const pageNumbers = computed(() =>
  Array.from({ length: totalPages.value }, (_, index) => index + 1),
)

watch(driverSearch, () => {
  currentPage.value = 1
})

watch(totalPages, (pageCount) => {
  if (currentPage.value > pageCount) currentPage.value = pageCount
})

const workStatusText = (workOrder) => {
  if (!workOrder) return '작업지시 없음'

  const statusMap = {
    DISPATCH_WAITING: '배차 대기',
    APPROVED: '승인 완료',
    GATE_IN: '입차 완료',
    IN_PROGRESS: '작업 진행 중',
    COMPLETED: '작업 완료',
    GATE_OUT: '출차 완료',
    CANCELED: '취소',
  }

  return statusMap[workOrder.workStatus] || workOrder.workStatus || '상태 미정'
}

const workStatusClass = (workOrder) => {
  if (!workOrder) return 'amber'
  if (['COMPLETED', 'GATE_OUT'].includes(workOrder.workStatus)) return 'green'
  if (workOrder.workStatus === 'CANCELED') return 'red'
  return 'amber'
}

const loadAssignedWorkOrders = async () => {
  workOrderLoading.value = true
  workOrderError.value = ''

  try {
    if (!myCarrier.value?.carrierId) {
      assignedVehicles.value = []
      workOrders.value = []
      trailerWorkInfos.value = {}
      return
    }

    const [vehicleData, workOrderData] = await Promise.all([
      fetchVehiclesByCarrier(myCarrier.value.carrierId),
      fetchWorkOrders(),
    ])

    assignedVehicles.value = (vehicleData || []).filter((vehicle) => isTrailerVehicle(vehicle.vehicleType))
    workOrders.value = workOrderData || []

    const trailerInfoEntries = await Promise.all(
      assignedVehicles.value.map(async (vehicle) => {
        try {
          return [vehicle.vehicleId, await fetchTrailerWorkInfo(vehicle.vehicleId)]
        } catch {
          return [vehicle.vehicleId, null]
        }
      }),
    )

    trailerWorkInfos.value = Object.fromEntries(trailerInfoEntries)
  } catch (error) {
    workOrderError.value = error.message || '작업지시 정보를 불러오지 못했습니다.'
    assignedVehicles.value = []
    workOrders.value = []
    trailerWorkInfos.value = {}
  } finally {
    workOrderLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [carrierData, driverData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
    if (props.showSummary) {
      await loadAssignedWorkOrders()
    } else {
      assignedVehicles.value = []
      workOrders.value = []
      trailerWorkInfos.value = {}
    }
  } catch (error) {
    errorMessage.value = error.message || '데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const generateCode = (prefix) => {
  const number = Math.floor(Math.random() * 900000 + 100000)
  return `${prefix}-${number}`
}

const resetForm = () => {
  form.value = {
    driverId: null,
    plateNumber: '',
    vehicleType: '트레일러',
    tonnage: '25톤',
    chassisNo: '',
  }
}

const validate = () => {
  form.value.vehicleType = '트레일러'

  if (!myCarrier.value) {
    throw new Error('로그인한 운송사 정보를 찾을 수 없습니다.')
  }

  if (!form.value.driverId) {
    throw new Error('트레일러를 배정할 기사를 선택하세요.')
  }

  if (!selectedDriver.value) {
    throw new Error('운송사 가입 승인이 완료된 소속 기사만 배정할 수 있습니다.')
  }

  if (!form.value.plateNumber.trim()) {
    throw new Error('트레일러 차량번호를 입력하세요.')
  }

  if (!form.value.tonnage) {
    throw new Error('톤수를 선택하세요.')
  }
}

const submitVehicle = async () => {
  message.value = ''
  errorMessage.value = ''

  try {
    validate()
    saving.value = true

    await createVehicle({
      driverId: form.value.driverId,
      carrierId: myCarrier.value.carrierId,
      userId: selectedDriver.value?.userId,
      plateNumber: form.value.plateNumber,
      vehicleType: '트레일러',
      tonnage: form.value.tonnage,
      tractorNo: null,
      chassisNo: form.value.chassisNo,
    })

    message.value = '트레일러 배정 내용이 관리자에게 전달되었습니다. 최종 승인 후 출입 가능합니다.'
    resetForm()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '트레일러 배정에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

const startTrailerEdit = (vehicle) => {
  editingVehicleId.value = vehicle.vehicleId
  editPlateNumber.value = vehicle.plateNumber || ''
  message.value = ''
  errorMessage.value = ''
}

const cancelTrailerEdit = () => {
  editingVehicleId.value = null
  editPlateNumber.value = ''
}

const saveTrailerEdit = async (vehicle) => {
  const plateNumber = editPlateNumber.value.trim()

  if (!plateNumber) {
    errorMessage.value = '트레일러 차량번호를 입력하세요.'
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    await updateVehicle(vehicle.vehicleId, {
      ...vehicle,
      plateNumber,
      vehicleType: vehicle.vehicleType || '트레일러',
      tonnage: vehicle.tonnage || '25톤',
      chassisNo: vehicle.chassisNo || null,
      tractorNo: null,
    })
    message.value = '트레일러 차량번호가 수정되었습니다. 연결된 작업정보에도 같은 차량이 반영됩니다.'
    cancelTrailerEdit()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '트레일러 차량번호 수정에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
  if (props.showSummary) {
    refreshTimer = setInterval(() => {
      if (!loading.value && !saving.value && !workOrderLoading.value && !editingVehicleId.value) {
        loadData().catch(() => {})
      }
    }, 5000)
  }
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div id="assignment-summary" class="page-stack">
    <section v-if="!props.embedded" class="panel">
      <div class="section-title">
        <h2>트레일러 배정</h2>
        <span class="status-pill">
          {{ myCarrier?.carrierName || '운송사 조회 중' }}
        </span>
      </div>

      <div v-if="message" class="form-message success">
        {{ message }}
      </div>

      <div v-if="errorMessage" class="form-message error">
        {{ errorMessage }}
      </div>
    </section>

    <section v-else-if="message || errorMessage" class="panel">
      <div v-if="message" class="form-message success">
        {{ message }}
      </div>
      <div v-if="errorMessage" class="form-message error">
        {{ errorMessage }}
      </div>
    </section>

    <section v-if="props.showSummary" class="panel">
      <div class="section-title">
        <h2>기사·트레일러 배정 및 작업정보</h2>
        <div class="section-title-actions">
          <span class="status-pill green">{{ filteredAssignedTrailerSummaries.length }}건</span>
          <a class="ghost-button" href="#work-order-management">
            작업지시 입력으로 이동
          </a>
        </div>
      </div>

      <div class="summary-toolbar">
        <label for="driverSearch">기사 이름 검색</label>
        <input
          id="driverSearch"
          v-model.trim="driverSearch"
          type="search"
          placeholder="기사 이름을 입력하세요"
        />
      </div>

      <div v-if="workOrderLoading" class="empty-box">
        배정된 차량과 작업지시를 확인하는 중입니다.
      </div>

      <div v-else-if="workOrderError" class="empty-box warning-box">
        {{ workOrderError }}
      </div>

      <div v-else-if="filteredAssignedTrailerSummaries.length === 0" class="empty-box">
        {{ assignedTrailerSummaries.length === 0 ? '기사에게 배정된 트레일러가 없습니다.' : '검색 조건에 맞는 배정 내역이 없습니다.' }}
      </div>

      <div v-else class="table-wrap assignment-summary-table-wrap">
        <table class="data-table assignment-summary-table">
          <thead>
            <tr>
              <th>기사</th>
              <th>트레일러</th>
              <th>차량 상태</th>
              <th>WorkOrder</th>
              <th>작업 유형</th>
              <th>작업 상태</th>
              <th>컨테이너·위치</th>
              <th>예약 시간</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="summary in paginatedAssignedTrailerSummaries" :key="summary.vehicle.vehicleId">
              <td>{{ summary.driver?.driverName || '기사 정보 없음' }}</td>
              <td>
                <div v-if="editingVehicleId === summary.vehicle.vehicleId" class="trailer-edit-cell">
                  <input
                    v-model.trim="editPlateNumber"
                    class="trailer-edit-input"
                    type="text"
                    aria-label="트레일러 차량번호"
                    @keyup.enter="saveTrailerEdit(summary.vehicle)"
                  />
                  <div class="trailer-edit-actions">
                    <button
                      class="table-action-button primary"
                      type="button"
                      :disabled="saving"
                      @click="saveTrailerEdit(summary.vehicle)"
                    >
                      저장
                    </button>
                    <button
                      class="table-action-button"
                      type="button"
                      :disabled="saving"
                      @click="cancelTrailerEdit"
                    >
                      취소
                    </button>
                  </div>
                </div>
                <template v-else>
                  <strong>{{ summary.vehicle.plateNumber || '-' }}</strong>
                  <small>{{ vehicleTypeLabel(summary.vehicle.vehicleType) }}</small>
                  <button
                    class="table-action-button"
                    type="button"
                    :disabled="saving"
                    @click="startTrailerEdit(summary.vehicle)"
                  >
                    수정
                  </button>
                </template>
              </td>
              <td>{{ summary.vehicle.vehicleStatus || (summary.vehicle.isRegistered ? '승인' : '승인 대기') }}</td>
              <td>{{ summary.workOrder?.workOrderId ? `#${summary.workOrder.workOrderId}` : '-' }}</td>
              <td>{{ summary.workOrder?.workType || '-' }}</td>
              <td>
                <span class="status-pill" :class="workStatusClass(summary.workOrder)">
                  {{ workStatusText(summary.workOrder) }}
                </span>
              </td>
              <td>
                {{ summary.workOrder?.containerNumber || summary.workOrder?.containerId || '-' }}
                <small v-if="summary.workOrder?.yardLocation || summary.workOrder?.containerLocation">
                  {{ summary.workOrder.yardLocation || summary.workOrder.containerLocation }}
                </small>
              </td>
              <td>{{ summary.workOrder?.reservedTime || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="filteredAssignedTrailerSummaries.length > 0" class="pagination" aria-label="기사·트레일러 배정 현황 페이지">
        <button class="ghost-button" type="button" :disabled="currentPage === 1" @click="currentPage -= 1">
          이전
        </button>
        <button
          v-for="page in pageNumbers"
          :key="page"
          class="page-button"
          :class="{ active: currentPage === page }"
          type="button"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
        <button class="ghost-button" type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">
          다음
        </button>
        <span>{{ currentPage }} / {{ totalPages }} 페이지</span>
      </div>
    </section>

    <section v-if="props.showInput" class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>배정 정보 입력</h2>
          <span class="status-pill amber">관리자 최종 승인 요청</span>
        </div>

        <div v-if="loading" class="empty-box">
          불러오는 중...
        </div>

        <form v-else class="form-grid" @submit.prevent="submitVehicle">
          <div class="field full">
            <label for="driverId">배정 기사</label>
            <select id="driverId" v-model.number="form.driverId">
              <option disabled :value="null">운송사 가입 승인 완료 기사를 선택하세요</option>
              <option
                v-for="driver in assignableDrivers"
                :key="driver.driverId"
                :value="driver.driverId"
              >
                {{ driver.driverName }} / {{ driver.driverContact || '-' }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="plateNumber">트레일러 차량번호</label>
            <input
              id="plateNumber"
              v-model.trim="form.plateNumber"
              placeholder="예: 부산80바9999"
            />
          </div>

          <div class="field">
            <label for="vehicleType">차량종류</label>
            <select id="vehicleType" v-model="form.vehicleType">
              <option v-for="type in vehicleTypes" :key="type" :value="type">
                {{ type }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="tonnage">톤수</label>
            <select id="tonnage" v-model="form.tonnage">
              <option v-for="ton in tonnageOptions" :key="ton" :value="ton">
                {{ ton }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="chassisNo">샤시 번호</label>
            <div class="inline-field">
              <input id="chassisNo" v-model.trim="form.chassisNo" placeholder="예: CH-999999" />
              <button type="button" @click="form.chassisNo = generateCode('CH')">자동</button>
            </div>
          </div>

          <button class="primary-button full submit-button" type="submit" :disabled="saving">
            {{ saving ? '전달 중...' : '관리자에게 최종 승인 요청' }}
          </button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>배정 가능 기사</h2>
          <span class="status-pill green">{{ assignableDrivers.length }}명</span>
        </div>

        <div v-if="myDrivers.length === 0" class="empty-box">
          소속 기사가 없습니다.
        </div>

        <div v-else-if="assignableDrivers.length === 0" class="empty-box">
          배정 가능한 기사가 없습니다. 먼저 기사 가입 승인 화면에서 기사를 승인하세요.
        </div>

        <div v-else class="driver-list">
          <div
            v-for="driver in assignableDrivers"
            :key="driver.driverId"
            class="driver-row"
            :class="{ selected: driver.driverId === form.driverId }"
          >
            <div>
              <b>{{ driver.driverName }}</b>
              <span>연락처 {{ driver.driverContact || '-' }}</span>
            </div>

            <div class="driver-actions">
              <span class="status-pill amber">
                트레일러 배정 가능
              </span>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.full {
  grid-column: 1 / -1;
}

.form-message {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

.form-message.success {
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

.warning-box {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.inline-field {
  display: flex;
  gap: 8px;
}

.inline-field input {
  flex: 1;
  min-width: 0;
}

.inline-field button {
  width: 60px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 4px;
  font-weight: 700;
}

.submit-button {
  min-height: 44px;
}

.driver-list {
  display: grid;
  gap: 10px;
}

.driver-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.driver-row.selected {
  border-color: var(--blue-700);
  box-shadow: inset 0 0 0 1px var(--blue-700);
}

.driver-row b,
.driver-row span {
  display: block;
}

.driver-row b {
  margin-bottom: 4px;
}

.driver-row div span {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.driver-actions {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.section-title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.summary-toolbar label {
  color: var(--ink-700);
  font-weight: 700;
  white-space: nowrap;
}

.summary-toolbar input {
  width: min(320px, 100%);
  min-height: 34px;
  padding: 0 9px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.assignment-summary-table-wrap {
  border: 1px solid var(--line);
}

.assignment-summary-table {
  min-width: 1020px;
  border: 0;
}

.assignment-summary-table td small {
  display: block;
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 11px;
}

.trailer-edit-cell {
  display: grid;
  gap: 5px;
}

.trailer-edit-input {
  width: min(150px, 100%);
  min-height: 30px;
  padding: 0 7px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 3px;
}

.trailer-edit-actions {
  display: flex;
  gap: 4px;
}

.table-action-button {
  min-height: 26px;
  padding: 0 7px;
  color: var(--ink-700);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 3px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.table-action-button.primary {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.table-action-button:disabled {
  cursor: wait;
  opacity: 0.55;
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

@media (max-width: 760px) {
  .inline-field {
    flex-direction: column;
  }

  .inline-field button {
    width: 100%;
    min-height: 36px;
  }

  .driver-row {
    align-items: stretch;
    flex-direction: column;
  }

  .driver-actions {
    justify-items: start;
  }

  .section-title-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-toolbar input {
    width: 100%;
  }
}
</style>
