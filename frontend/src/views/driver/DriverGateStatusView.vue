<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { completeWorkOrder, startWorkOrder } from '@/api/adminApi/workOrderApi'
import { fetchMyWorkStatusHistory } from '@/api/driverApi'
import { useDriverStore } from '@/stores/driverStore'
import { displayTone, workStatusLabel } from '@/config/displayLabels'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)
let refreshTimer = null
const processingId = ref(null)
const actionMessage = ref('')
const actionError = ref('')
const selectedWorkStatus = ref('')
const workHistory = ref([])
const historyLoading = ref(false)
const historyLoaded = ref(false)
const historyError = ref('')
const historyQuery = ref('')
const historyPage = ref(1)
const historyPageSize = 10

const workStatusOptions = [
  'DISPATCH_WAITING',
  'APPROVED',
  'GATE_IN',
  'IN_PROGRESS',
  'CANCELED',
]

const filteredWorkOrders = computed(() => {
  if (!selectedWorkStatus.value) return myWorkOrders.value
  return myWorkOrders.value.filter((order) => order.workStatus === selectedWorkStatus.value)
})

const activeWorkOrders = computed(() =>
  filteredWorkOrders.value.filter((order) => !['COMPLETED', 'GATE_OUT'].includes(order.workStatus)),
)

const completedHistory = computed(() => {
  const completedByWorkOrder = new Map()

  workHistory.value
    .filter((history) => ['COMPLETED', 'GATE_OUT'].includes(history.newStatus))
    .forEach((history) => {
      const current = completedByWorkOrder.get(history.workOrderId)
      const currentTime = current?.changedTime ? new Date(current.changedTime).getTime() : 0
      const historyTime = history.changedTime ? new Date(history.changedTime).getTime() : 0

      if (!current || historyTime >= currentTime) {
        completedByWorkOrder.set(history.workOrderId, history)
      }
    })

  return Array.from(completedByWorkOrder.values())
})

const getHistorySearchText = (history) => [
  history.changedTime,
  history.workOrderId,
  history.workType,
  history.plateNumber,
  history.containerNumber,
  history.newStatus,
  workStatusLabel(history.newStatus),
].join(' ').toLowerCase()

const filteredCompletedHistory = computed(() => {
  const keyword = historyQuery.value.trim().toLowerCase()
  if (!keyword) return completedHistory.value
  return completedHistory.value.filter((history) => getHistorySearchText(history).includes(keyword))
})

const historyPageCount = computed(() =>
  Math.max(1, Math.ceil(filteredCompletedHistory.value.length / historyPageSize)),
)

const historyPageNumbers = computed(() =>
  Array.from({ length: historyPageCount.value }, (_, index) => index + 1),
)

const pagedCompletedHistory = computed(() => {
  const start = (historyPage.value - 1) * historyPageSize
  return filteredCompletedHistory.value.slice(start, start + historyPageSize)
})

const getHistoryPageStart = () => {
  if (filteredCompletedHistory.value.length === 0) return 0
  return (historyPage.value - 1) * historyPageSize + 1
}

const getHistoryPageEnd = () => Math.min(
  filteredCompletedHistory.value.length,
  historyPage.value * historyPageSize,
)

const formatHistoryTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

const loadWorkHistory = async () => {
  if (!loginUser.value?.userId) return
  if (!historyLoaded.value) historyLoading.value = true
  historyError.value = ''

  try {
    workHistory.value = (await fetchMyWorkStatusHistory(loginUser.value.userId)) || []
    historyLoaded.value = true
  } catch (error) {
    historyError.value = error.message || '완료 현황을 불러오지 못했습니다.'
  } finally {
    historyLoading.value = false
  }
}

watch(historyQuery, () => {
  historyPage.value = 1
})

watch(historyPageCount, (pageCount) => {
  if (historyPage.value > pageCount) historyPage.value = pageCount
})

const statusText = (order) => {
  if (order.workStatus === 'GATE_OUT') return '출차 완료'
  if (order.workStatus === 'COMPLETED') return order.canExit ? '출차 가능' : '출차 대기'
  if (order.workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (order.workStatus === 'GATE_IN') return '입차 완료'
  if (order.workStatus === 'CANCELED') return workStatusLabel(order.workStatus)
  if (!order.isApproved) return '작업 승인 대기'
  return workStatusLabel(order.workStatus)
}

const statusClass = (order) => {
  if (order.workStatus === 'COMPLETED' && order.canExit) return 'green'
  return displayTone('work', order.workStatus)
}

const guideText = (order) => {
  if (order.workStatus === 'GATE_IN') {
    return '입차 되었습니다. 해당 야드 섹터로 이동하여 작업을 실시하세요.'
  }

  return order.guideMessage || '게이트 입차 후 번호판 인식 결과와 작업 승인 상태를 확인하세요.'
}

const processWork = async (order, action) => {
  processingId.value = order.workOrderId
  actionMessage.value = ''
  actionError.value = ''

  try {
    const result = action === 'start'
      ? await startWorkOrder(order.workOrderId)
      : await completeWorkOrder(order.workOrderId)

    if (result?.success === false) {
      throw new Error(result.message || '작업 상태를 변경하지 못했습니다.')
    }

    actionMessage.value = action === 'start'
      ? '작업을 시작했습니다.'
      : '작업을 완료했습니다.'

    await Promise.all([
      driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId),
      loadWorkHistory(),
    ])
  } catch (error) {
    actionError.value = error.message || '작업 처리에 실패했습니다.'
  } finally {
    processingId.value = null
  }
}

onMounted(() => {
  if (loginUser.value?.userId) {
    driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId)
    loadWorkHistory()

    refreshTimer = setInterval(() => {
      if (!driverStore.loading) {
        driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId).catch(() => {})
      }
      if (!historyLoading.value) {
        loadWorkHistory().catch(() => {})
      }
    }, 5000)
  }
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title work-status-title">
        <h2>작업 현황</h2>
        <div class="work-status-tools">
          <label for="driver-status-filter">작업 상태</label>
          <select id="driver-status-filter" v-model="selectedWorkStatus">
            <option value="">전체</option>
            <option v-for="status in workStatusOptions" :key="status" :value="status">
              {{ workStatusLabel(status) }}
            </option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="empty-box">
        작업 현황을 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div v-else-if="activeWorkOrders.length === 0" class="empty-box">
        {{ myWorkOrders.length === 0 ? '현재 배정된 작업이 없습니다.' : '현재 진행 중인 작업이 없습니다.' }}
      </div>

      <div v-else class="work-list">
        <div v-if="actionMessage" class="action-feedback success" role="status">
          {{ actionMessage }}
        </div>
        <div v-if="actionError" class="action-feedback warning" role="alert">
          {{ actionError }}
        </div>

        <article
          v-for="order in activeWorkOrders"
          :key="order.workOrderId"
          class="work-card"
        >
          <div class="work-card-head">
            <div>
              <strong>{{ order.workType || '작업 유형 미정' }}</strong>
              <span>작업 ID {{ order.workOrderId }}</span>
            </div>

            <span class="status-pill" :class="statusClass(order)">
              {{ statusText(order) }}
            </span>
          </div>

          <div class="work-info-grid">
            <div>
              <span>차량 번호</span>
              <b>{{ order.plateNumber || '-' }}</b>
            </div>
            <div>
              <span>트레일러 번호</span>
              <b>{{ order.trailerPlateNumber || '-' }}</b>
            </div>
            <div>
              <span>컨테이너</span>
              <b>{{ order.containerNumber || '-' }}</b>
            </div>
            <div>
              <span>야드 섹터</span>
              <b>{{ order.sectorName || '-' }}</b>
            </div>
            <div>
              <span>예약 시간</span>
              <b>{{ order.reservedTime || '-' }}</b>
            </div>
          </div>

          <div class="guide-line">
            {{ guideText(order) }}
          </div>

          <div
            v-if="order.workStatus === 'GATE_IN' || order.workStatus === 'IN_PROGRESS'"
            class="driver-work-actions"
          >
            <button
              v-if="order.workStatus === 'GATE_IN'"
              class="primary-button"
              type="button"
              :disabled="processingId === order.workOrderId"
              @click="processWork(order, 'start')"
            >
              {{ processingId === order.workOrderId ? '작업 시작 처리 중...' : '작업 시작' }}
            </button>
            <button
              v-else
              class="primary-button complete"
              type="button"
              :disabled="processingId === order.workOrderId"
              @click="processWork(order, 'complete')"
            >
              {{ processingId === order.workOrderId ? '작업 완료 처리 중...' : '작업 완료' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <section class="panel completed-work-panel">
      <div class="section-title">
        <h2>완료 현황</h2>
        <span class="status-pill green">총 {{ completedHistory.length }}건</span>
      </div>

      <div class="completed-work-tools">
        <label for="completed-work-search">완료 작업 검색</label>
        <input
          id="completed-work-search"
          v-model="historyQuery"
          type="search"
          placeholder="작업 ID·작업 유형·컨테이너 검색"
        />
      </div>

      <div v-if="historyLoading" class="empty-box">
        완료 현황을 불러오는 중입니다.
      </div>

      <div v-else-if="historyError" class="empty-box warning">
        {{ historyError }}
      </div>

      <div v-else-if="filteredCompletedHistory.length === 0" class="empty-box">
        완료된 작업 이력이 없습니다.
      </div>

      <div v-else class="table-wrap completed-work-table">
        <table class="data-table">
          <thead>
            <tr>
              <th>완료 시각</th>
              <th>작업 ID</th>
              <th>작업 유형</th>
              <th>트랙터</th>
              <th>컨테이너</th>
              <th>처리 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="history in pagedCompletedHistory" :key="history.historyId">
              <td>{{ formatHistoryTime(history.changedTime) }}</td>
              <td>{{ history.workOrderId }}</td>
              <td>{{ history.workType || '-' }}</td>
              <td>{{ history.plateNumber || '-' }}</td>
              <td>{{ history.containerNumber || '-' }}</td>
              <td>
                <span class="status-pill green">{{ workStatusLabel(history.newStatus) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="!historyLoading && !historyError && filteredCompletedHistory.length > 0"
        class="pagination-bar"
      >
        <span>
          {{ getHistoryPageStart() }} - {{ getHistoryPageEnd() }} /
          {{ filteredCompletedHistory.length }}건
        </span>
        <div class="pagination-controls">
          <button class="ghost-button" type="button" :disabled="historyPage === 1" @click="historyPage -= 1">이전</button>
          <button
            v-for="page in historyPageNumbers"
            :key="page"
            class="page-button"
            :class="{ active: historyPage === page }"
            type="button"
            @click="historyPage = page"
          >
            {{ page }}
          </button>
          <button class="ghost-button" type="button" :disabled="historyPage === historyPageCount" @click="historyPage += 1">다음</button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.work-list {
  display: grid;
  gap: 10px;
}

.work-status-title {
  align-items: center;
  gap: 12px;
}

.work-status-tools {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: auto;
}

.work-status-tools label {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.work-status-tools select {
  width: min(100%, 150px);
  min-width: 0;
  min-height: 32px;
  padding: 4px 8px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
  font: inherit;
}

.completed-work-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.completed-work-tools label {
  flex: 0 0 auto;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.completed-work-tools input,
.completed-work-tools select {
  min-height: 32px;
  padding: 4px 8px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
  font: inherit;
}

.completed-work-tools input {
  flex: 1 1 240px;
  width: auto;
  min-width: 0;
}

.completed-work-tools select {
  min-width: 80px;
}

.completed-work-table {
  min-width: 0;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.pagination-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
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

.work-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.work-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.work-card-head strong,
.work-card-head span {
  display: block;
}

.work-card-head strong {
  font-size: 17px;
}

.work-card-head div span {
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.work-info-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.work-info-grid div,
.guide-line,
.empty-box {
  padding: 10px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.work-info-grid span {
  display: block;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.work-info-grid b {
  display: block;
  margin-top: 3px;
}

.guide-line {
  color: #244766;
  font-weight: 800;
}

.driver-work-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.driver-work-actions .primary-button {
  min-width: 180px;
  min-height: 38px;
}

.driver-work-actions .primary-button.complete {
  background: #23734f;
  border-color: #23734f;
}

.action-feedback {
  padding: 11px 12px;
  border: 1px solid var(--line);
  font-weight: 800;
}

.action-feedback.success {
  color: #12643a;
  background: #eaf7ef;
  border-color: #78c69a;
}

.action-feedback.warning {
  color: #a42626;
  background: #fff1f1;
  border-color: #e19a9a;
}

.empty-box {
  color: var(--ink-500);
  font-weight: 800;
}

.empty-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

@media (max-width: 900px) {
  .work-status-title {
    align-items: stretch;
    flex-direction: column;
  }

  .work-status-tools {
    justify-content: flex-start;
    margin-left: 0;
  }

  .completed-work-tools {
    align-items: stretch;
    flex-direction: column;
  }

  .completed-work-tools label {
    align-self: flex-start;
  }

  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination-controls {
    justify-content: flex-start;
  }

  .work-card-head {
    align-items: stretch;
    flex-direction: column;
  }

  .work-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
