<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { readCurrentUser } from '@/stores/authStore'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { vehicleTypeLabel } from '@/config/vehicleType'
import { booleanLabel, displayTone, workStatusLabel } from '@/config/displayLabels'

const currentUser = readCurrentUser()
const carrierStore = useCarrierStore()
const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()
const { carriers } = storeToRefs(carrierStore)
const { drivers, myWorkOrders, loading, error } = storeToRefs(driverStore)
const { myVehicle } = storeToRefs(vehicleStore)
let refreshTimer = null

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

const myDriver = computed(() =>
  drivers.value.find(
    (driver) => String(driver.userId) === String(loginUser.value?.userId),
  ),
)

const myCarrier = computed(() =>
  carriers.value.find(
    (carrier) => String(carrier.carrierId) === String(myDriver.value?.carrierId),
  ),
)

const driverName = computed(() =>
  myDriver.value?.driverName ||
  currentUser?.displayName ||
  currentUser?.userName ||
  loginUser.value?.userName ||
  loginUser.value?.loginId ||
  '-',
)

const carrierName = computed(() =>
  myCarrier.value?.carrierName || currentWorkOrder.value?.carrierName || '-',
)

const vehicleApprovalText = computed(() => {
  if (!myVehicle.value) return '차량 정보 없음'
  return myVehicle.value.isRegistered ? '최종 승인 완료' : '최종 승인 대기'
})

const vehicleApprovalClass = computed(() =>
  myVehicle.value?.isRegistered ? 'green' : 'amber',
)

const currentWorkOrder = computed(() => {
  const activeStatuses = ['DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED']
  return myWorkOrders.value.find((order) => activeStatuses.includes(order.workStatus)) || null
})

const passStatus = computed(() => {
  const order = currentWorkOrder.value
  if (!order) return '대기'
  if (order.workStatus === 'GATE_OUT') return '출차 완료'
  if (order.workStatus === 'COMPLETED') return order.canExit ? '출차 가능' : '출차 대기'
  if (order.workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (order.workStatus === 'GATE_IN') return '입차 완료'
  if (order.isApproved && order.canEnter) return '입차 가능'
  return '승인 대기'
})

const nextGuide = computed(() => {
  const order = currentWorkOrder.value
  if (!order) return '배정된 작업이 없습니다.'
  if (order.workStatus === 'GATE_OUT') return '출차 처리가 완료되었습니다.'
  if (order.workStatus === 'COMPLETED') {
    return order.canExit
      ? '작업이 완료되었습니다. 출차 게이트로 이동하세요.'
      : '작업이 완료되었습니다. 관리자 확인 후 출차할 수 있습니다.'
  }
  if (order.workStatus === 'IN_PROGRESS') return order.guideMessage || '작업을 진행하세요.'
  if (order.workStatus === 'GATE_IN') return '입차 되었습니다. 해당 야드 섹터로 이동하여 작업을 실시하세요.'
  if (!order.isApproved) return '관리자 승인 후 게이트 입차가 가능합니다.'
  if (!order.canEnter) return '기사 출입 가능 상태를 운송사 또는 관리자에게 확인하세요.'
  return `${order.sectorName || '지정 섹터'}로 이동 후 안내 메시지를 확인하세요.`
})

const getBooleanText = (value) => {
  if (value === true) return '승인'
  if (value === false) return '미승인'
  return '-'
}

const getWorkStatusClass = (status) => displayTone('work', status)

const getWorkStatusText = (status) => workStatusLabel(status)

const getEntryClass = (value) => (value ? 'green' : 'red')

onMounted(async () => {
  if (loginUser.value?.userId) {
    await Promise.allSettled([
      driverStore.loadDrivers(),
      carrierStore.loadCarriers(),
      driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId),
    ])

    if (myDriver.value?.driverId) {
      vehicleStore.loadVehicleByDriver(myDriver.value.driverId).catch(() => {})
    }

    refreshTimer = setInterval(() => {
      if (!driverStore.loading) {
        driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId).catch(() => {})
      }
    }, 5000)
  }
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="page-stack driver-page">
    <section class="driver-identity" aria-label="현재 로그인 기사">
      <div class="identity-item">
        <span>기사명</span>
        <strong>{{ driverName }}</strong>
      </div>
      <div class="identity-item">
        <span>소속 운송사</span>
        <strong>{{ carrierName }}</strong>
      </div>
    </section>

    <section class="tractor-overview panel">
      <div class="section-title">
        <h2>내 트랙터</h2>
        <span class="status-pill" :class="vehicleApprovalClass">
          {{ vehicleApprovalText }}
        </span>
      </div>

      <div v-if="myVehicle" class="tractor-summary">
        <div>
          <span>차량번호</span>
          <strong>{{ myVehicle.plateNumber || '-' }}</strong>
        </div>
        <div>
          <span>차량 유형</span>
          <strong>{{ vehicleTypeLabel(myVehicle.vehicleType) }}</strong>
        </div>
        <div>
          <span>차량 상태</span>
          <strong>{{ myVehicle.vehicleStatus || '-' }}</strong>
        </div>
      </div>

      <div v-else class="empty-panel">
        등록된 트랙터 정보가 없습니다.
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>내 작업 안내</h2>
        <span class="status-pill">{{ currentWorkOrder ? 1 : 0 }}건</span>
      </div>

      <div v-if="loading" class="empty-panel">
        작업 정보를 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-panel warning">
        {{ error }}
      </div>

      <div v-else-if="currentWorkOrder" class="work-summary">
        <div>
          <span>트레일러 번호</span>
          <strong>{{ currentWorkOrder.trailerPlateNumber || '-' }}</strong>
        </div>
        <div>
          <span>작업 유형</span>
          <strong>{{ currentWorkOrder.workType || '-' }}</strong>
        </div>
        <div>
          <span>작업 상태</span>
          <strong>
            <span class="status-pill" :class="getWorkStatusClass(currentWorkOrder.workStatus)">
              {{ getWorkStatusText(currentWorkOrder.workStatus) }}
            </span>
          </strong>
        </div>
        <div>
          <span>작업 승인</span>
          <strong>{{ getBooleanText(currentWorkOrder.isApproved) }}</strong>
        </div>
      </div>

      <div v-else class="empty-panel">
        로그인한 기사에게 배정된 작업 정보가 없습니다.
      </div>
    </section>

    <section v-if="currentWorkOrder" class="driver-operation-panel">
      <article class="driver-pass-card">
        <span>최종 출입 상태</span>
        <strong>{{ passStatus }}</strong>
        <p>{{ nextGuide }}</p>
      </article>
      <article class="driver-pass-card">
        <span>목적지</span>
        <strong>{{ currentWorkOrder.sectorName || '-' }}</strong>
        <p>{{ currentWorkOrder.guideMessage || '야드 안내 메시지가 없습니다.' }}</p>
      </article>
      <article class="driver-pass-card">
        <span>예약 시간</span>
        <strong>{{ currentWorkOrder.reservedTime || '-' }}</strong>
        <p>{{ currentWorkOrder.containerNumber || '-' }} / {{ currentWorkOrder.workType || '-' }}</p>
      </article>
    </section>

    <section v-if="currentWorkOrder" class="grid-2 driver-grid">
      <article class="panel">
        <div class="section-title">
          <h2>컨테이너 / 야드 안내</h2>
          <span class="status-pill green">{{ currentWorkOrder.sectorName || '-' }}</span>
        </div>

        <table class="data-table">
          <tbody>
            <tr><th>컨테이너 번호</th><td>{{ currentWorkOrder.containerNumber || '-' }}</td></tr>
            <tr><th>컨테이너 크기</th><td>{{ currentWorkOrder.containerSize || '-' }}</td></tr>
            <tr><th>컨테이너 위치</th><td>{{ currentWorkOrder.containerLocation || '-' }}</td></tr>
            <tr>
              <th>블록 / 베이 / 로우</th>
              <td>{{ currentWorkOrder.block || '-' }} / {{ currentWorkOrder.bay || '-' }} / {{ currentWorkOrder.rowNo || '-' }}</td>
            </tr>
            <tr><th>야드 섹터</th><td>{{ currentWorkOrder.sectorName || '-' }}</td></tr>
            <tr><th>섹터 상태</th><td>{{ currentWorkOrder.sectorStatus || '-' }}</td></tr>
            <tr><th>대체 대기장소</th><td>{{ currentWorkOrder.altWaitingArea || '-' }}</td></tr>
            <tr><th>안내 메시지</th><td>{{ currentWorkOrder.guideMessage || '-' }}</td></tr>
          </tbody>
        </table>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>연락처 / 출입 정보</h2>
          <span class="status-pill" :class="getEntryClass(currentWorkOrder.canEnter)">
            {{ booleanLabel(currentWorkOrder.canEnter) }}
          </span>
        </div>

        <table class="data-table">
          <tbody>
            <tr><th>기사 연락처</th><td>{{ currentWorkOrder.driverContact || '-' }}</td></tr>
            <tr>
              <th>기사 출입 가능</th>
              <td>
                <span class="status-pill" :class="getEntryClass(currentWorkOrder.canEnter)">
                  {{ booleanLabel(currentWorkOrder.canEnter) }}
                </span>
              </td>
            </tr>
            <tr><th>운송사 연락처</th><td>{{ currentWorkOrder.carrierContact || '-' }}</td></tr>
          </tbody>
        </table>
      </article>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>작업 목록</h2>
        <span class="status-pill">{{ myWorkOrders.length }}건</span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>작업 유형</th>
              <th>트레일러</th>
              <th>컨테이너</th>
              <th>야드 섹터</th>
              <th>예약 시간</th>
              <th>상태</th>
              <th>승인</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in myWorkOrders" :key="order.workOrderId">
              <td>{{ order.workOrderId }}</td>
              <td>{{ order.workType || '-' }}</td>
              <td>{{ order.trailerPlateNumber || '-' }}</td>
              <td>{{ order.containerNumber || '-' }}</td>
              <td>{{ order.sectorName || '-' }}</td>
              <td>{{ order.reservedTime || '-' }}</td>
              <td>
                <span class="status-pill" :class="getWorkStatusClass(order.workStatus)">
                  {{ getWorkStatusText(order.workStatus) }}
                </span>
              </td>
              <td>{{ getBooleanText(order.isApproved) }}</td>
            </tr>
            <tr v-if="myWorkOrders.length === 0">
              <td colspan="8">조회된 작업 정보가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.driver-page {
  width: 100%;
  max-width: none;
  min-width: 0;
}

.driver-identity {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border: 1px solid var(--line);
  background: #ffffff;
}

.identity-item {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px 14px;
}

.identity-item + .identity-item {
  border-left: 1px solid var(--line);
}

.identity-item span,
.tractor-summary span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.identity-item strong {
  color: #173b60;
  font-size: 18px;
  overflow-wrap: anywhere;
}

.tractor-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.tractor-summary div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px 14px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.tractor-summary strong {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: 17px;
}

.work-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 12px;
  min-width: 0;
  overflow-x: auto;
}

.work-summary div,
.empty-panel {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 14px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.work-summary span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.work-summary strong {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: 17px;
  font-weight: 900;
}

.empty-panel {
  color: var(--ink-500);
  font-weight: 800;
}

.empty-panel.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.driver-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}

.driver-grid > .panel {
  min-width: 0;
  overflow: hidden;
}

.driver-grid .data-table {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

.driver-grid .data-table th,
.driver-grid .data-table td {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.driver-grid .data-table th {
  width: 42%;
}

.driver-operation-panel {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1fr;
  gap: 10px;
  min-width: 0;
}

.driver-pass-card {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 14px;
  background: #f7f9fb;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue-700);
  border-radius: 2px;
}

.driver-pass-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.driver-pass-card strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--ink-900);
  font-size: 22px;
  font-weight: 900;
}

.driver-pass-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

@media (min-width: 1100px) and (max-height: 760px) {
  .work-summary {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
  }

  .work-summary div,
  .empty-panel,
  .driver-pass-card {
    padding: 11px 12px;
  }

  .work-summary strong {
    font-size: 16px;
  }

  .driver-operation-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .driver-pass-card strong {
    font-size: 20px;
  }

  .driver-grid .data-table th,
  .driver-grid .data-table td {
    padding: 7px 8px;
  }
}

@media (max-width: 900px) {
  .driver-identity,
  .tractor-summary,
  .driver-operation-panel,
  .driver-grid {
    grid-template-columns: 1fr;
  }

  .identity-item + .identity-item {
    border-top: 1px solid var(--line);
    border-left: 0;
  }
}
</style>
