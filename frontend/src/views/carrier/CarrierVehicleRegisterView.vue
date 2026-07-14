<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { createVehicle, fetchVehiclesByCarrier } from '@/api/vehicleApi'
import { fetchTrailerWorkInfo, fetchWorkOrders } from '@/api/adminApi/workOrderApi'
import { vehicleTypeLabel } from '@/config/vehicleType'

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

  if (trailerInfo?.workOrderId) {
    return trailerInfo
  }

  return workOrders.value.find(
    (workOrder) =>
      workOrder.trailerVehicleId === vehicleId || workOrder.vehicleId === vehicleId,
  ) || null
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
    await loadAssignedWorkOrders()
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

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
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

    <section class="panel">
      <div class="section-title">
        <h2>기사·트레일러 종합 작업정보</h2>
        <span class="status-pill green">{{ assignedTrailerSummaries.length }}건</span>
      </div>

      <div v-if="workOrderLoading" class="empty-box">
        배정된 차량과 작업지시를 확인하는 중입니다.
      </div>

      <div v-else-if="workOrderError" class="empty-box warning-box">
        {{ workOrderError }}
      </div>

      <div v-else-if="assignedTrailerSummaries.length === 0" class="empty-box">
        기사에게 배정된 트레일러가 없습니다.
      </div>

      <div v-else class="assignment-summary-list">
        <article
          v-for="summary in assignedTrailerSummaries"
          :key="summary.vehicle.vehicleId"
          class="assignment-summary-card"
        >
          <div class="assignment-summary-head">
            <div>
              <strong>{{ summary.driver?.driverName || '기사 정보 없음' }}</strong>
              <span>{{ summary.vehicle.plateNumber || '-' }} · {{ vehicleTypeLabel(summary.vehicle.vehicleType) }}</span>
            </div>
            <span class="status-pill" :class="workStatusClass(summary.workOrder)">
              {{ workStatusText(summary.workOrder) }}
            </span>
          </div>

          <div v-if="summary.workOrder" class="work-order-grid">
            <div><span>WorkOrder</span><strong>#{{ summary.workOrder.workOrderId }}</strong></div>
            <div><span>작업 유형</span><strong>{{ summary.workOrder.workType || '-' }}</strong></div>
            <div><span>승인 여부</span><strong>{{ summary.workOrder.isApproved ? '승인' : '대기' }}</strong></div>
            <div><span>예약 시간</span><strong>{{ summary.workOrder.reservedTime || '-' }}</strong></div>
            <div><span>트랙터 ID</span><strong>{{ summary.workOrder.tractorVehicleId || '-' }}</strong></div>
            <div><span>트레일러 ID</span><strong>{{ summary.workOrder.trailerVehicleId || summary.vehicle.vehicleId || '-' }}</strong></div>
            <div><span>컨테이너</span><strong>{{ summary.workOrder.containerNumber || summary.workOrder.containerId || '-' }}</strong></div>
            <div><span>야드 섹터</span><strong>{{ summary.workOrder.sectorName || summary.workOrder.sectorId || '-' }}</strong></div>
            <div><span>작업 위치</span><strong>{{ summary.workOrder.yardLocation || summary.workOrder.containerLocation || '-' }}</strong></div>
            <div><span>안내</span><strong>{{ summary.workOrder.guideMessage || summary.workOrder.workGuideMessage || '-' }}</strong></div>
          </div>

          <div v-else class="assignment-summary-empty">
            차량 배정은 완료되었지만 연결된 WorkOrder가 없습니다. 작업지시가 생성되면 이 영역에 표시됩니다.
          </div>
        </article>
      </div>
    </section>

    <section class="grid-2">
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

.assignment-summary-list {
  display: grid;
  gap: 10px;
}

.assignment-summary-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #f7f9fb;
  border: 1px solid var(--line);
}

.assignment-summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assignment-summary-head strong,
.assignment-summary-head span {
  display: block;
}

.assignment-summary-head strong {
  font-size: 16px;
}

.assignment-summary-head div span {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.work-order-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.work-order-grid div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.work-order-grid span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.work-order-grid strong {
  overflow-wrap: anywhere;
  font-size: 13px;
}

.assignment-summary-empty {
  padding: 12px;
  color: var(--ink-500);
  background: #ffffff;
  border: 1px dashed var(--line);
  font-weight: 700;
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

  .assignment-summary-head {
    align-items: stretch;
    flex-direction: column;
  }

  .work-order-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
