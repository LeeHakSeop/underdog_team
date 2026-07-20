<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useDashboardStore } from '@/stores/adminStore/dashboardStore'
import { displayTone, workStatusLabel } from '@/config/displayLabels'

const dashboardStore = useDashboardStore()
const { dashboard, loading, error } = storeToRefs(dashboardStore)

const summary = computed(() => dashboard.value?.summary || {})
const workStatusList = computed(() => dashboard.value?.workStatusList || [])
const recentWorkOrders = computed(() => dashboard.value?.recentWorkOrders || [])
const sectorList = computed(() => dashboard.value?.sectorList || [])
let refreshTimer = null

const recognitionRate = computed(() => {
  const total = summary.value.recognitionTotal || 0
  const success = summary.value.recognitionSuccess || 0
  return total === 0 ? 0 : Math.round((success / total) * 100)
})

const getStatusText = (workStatus) => workStatusLabel(workStatus)

const getStatusClass = (workStatus) => displayTone('work', workStatus)

const metricCards = computed(() => [
  {
    label: '가입 승인 대기',
    value: summary.value.pendingUsers || 0,
    hint: `운송사 ${summary.value.pendingCarriers || 0} / 기사 ${summary.value.pendingDrivers || 0}`,
  },
  {
    label: '오늘 게이트 처리',
    value: (summary.value.todayGateIn || 0) + (summary.value.todayGateOut || 0),
    hint: `입차 ${summary.value.todayGateIn || 0}건 / 출차 ${summary.value.todayGateOut || 0}건`,
  },
  {
    label: '번호판 인식 성공률',
    value: `${recognitionRate.value}%`,
    hint: `성공 ${summary.value.recognitionSuccess || 0}건 / 실패 ${summary.value.recognitionFail || 0}건`,
  },
  {
    label: '미처리 예외',
    value: summary.value.exceptionOpen || 0,
    hint: '미처리 예외 로그 기준',
  },
])

const workCards = computed(() => [
  { label: '전체 작업', value: summary.value.workTotal || 0 },
  { label: '대기 작업', value: summary.value.workReady || 0 },
  { label: '진행 작업', value: summary.value.workInProgress || 0 },
  { label: '완료 작업', value: summary.value.workDone || 0 },
])

const getWorkCount = (workStatus) => {
  const status = workStatusList.value.find((item) => item.workStatus === workStatus)
  return status ? status.workCount : 0
}

const workFlowCards = computed(() => [
  { label: '작업 요청', status: 'DISPATCH_WAITING', count: getWorkCount('DISPATCH_WAITING') },
  { label: '승인 완료', status: 'APPROVED', count: getWorkCount('APPROVED') },
  { label: '입차 완료', status: 'GATE_IN', count: getWorkCount('GATE_IN') },
  { label: '작업 진행', status: 'IN_PROGRESS', count: getWorkCount('IN_PROGRESS') },
  { label: '작업 완료', status: 'COMPLETED', count: getWorkCount('COMPLETED') },
  { label: '출차 완료', status: 'GATE_OUT', count: getWorkCount('GATE_OUT') },
])

const priorityCards = computed(() => [
  {
    label: '승인 처리',
    value: summary.value.pendingUsers || 0,
    text: '운송사/기사 가입 승인 대기',
    tone: (summary.value.pendingUsers || 0) > 0 ? 'amber' : 'green',
  },
  {
    label: '예외 확인',
    value: summary.value.exceptionOpen || 0,
    text: '번호판 인식 실패 및 출입 예외',
    tone: (summary.value.exceptionOpen || 0) > 0 ? 'red' : 'green',
  },
  {
    label: '작업 진행',
    value: summary.value.workInProgress || 0,
    text: '현재 상차/하차 진행 중',
    tone: (summary.value.workInProgress || 0) > 0 ? 'blue' : 'amber',
  },
  {
    label: '야드 확인',
    value: sectorList.value.length,
    text: '섹터 상태 및 대기 차량 확인',
    tone: 'blue',
  },
])

onMounted(() => {
  dashboardStore.loadDashboard()

  refreshTimer = setInterval(() => {
    if (!dashboardStore.loading) {
      dashboardStore.loadDashboard().catch(() => {})
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
        <h2>운영 통계 요약</h2>
        <button class="ghost-button" type="button" @click="dashboardStore.loadDashboard">
          통계 새로고침
        </button>
      </div>

      <div v-if="loading" class="empty-box">
        통계 데이터를 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div v-else class="grid-4">
        <article v-for="card in metricCards" :key="card.label" class="metric-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </article>
      </div>
    </section>

    <section v-if="!loading && !error" class="panel">
      <div class="section-title">
        <h2>작업 흐름 통계</h2>
        <span class="status-pill">5초 갱신</span>
      </div>

      <div class="work-flow-grid">
        <template v-for="(card, index) in workFlowCards" :key="card.status">
          <article class="flow-card">
            <span>{{ card.label }}</span>
            <strong>{{ card.count }}건</strong>
            <small>{{ getStatusText(card.status) }}</small>
          </article>
          <span v-if="index < workFlowCards.length - 1" class="flow-arrow">→</span>
        </template>
      </div>
    </section>

    <section v-if="!loading && !error" class="grid-4 priority-grid">
      <article v-for="card in priorityCards" :key="card.label" class="priority-card" :class="card.tone">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.text }}</p>
      </article>
    </section>

    <section class="grid-2 dashboard-grid">
      <article class="panel">
        <div class="section-title">
          <h2>작업 상태별 집계</h2>
          <span class="status-pill">{{ summary.workTotal || 0 }}건</span>
        </div>

        <div class="work-card-grid">
          <div v-for="card in workCards" :key="card.label" class="work-card">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </div>
        </div>

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>작업 상태</th>
                <th>건수</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="status in workStatusList" :key="status.workStatus">
                <td>
                  <span class="status-pill" :class="getStatusClass(status.workStatus)">
                    {{ getStatusText(status.workStatus) }}
                  </span>
                </td>
                <td>{{ status.workCount }}</td>
              </tr>
              <tr v-if="workStatusList.length === 0">
                <td colspan="2">작업 상태 데이터가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>야드 섹터 요약</h2>
          <span class="status-pill">{{ sectorList.length }}개</span>
        </div>

        <div v-if="sectorList.length" class="sector-list">
          <article v-for="sector in sectorList" :key="sector.sectorId" class="sector-card">
            <div>
              <strong>{{ sector.sectorName || '-' }}</strong>
              <span>{{ sector.blockName || '-' }}</span>
            </div>
            <p>{{ sector.guideMessage || '안내 메시지가 없습니다.' }}</p>
            <dl>
              <div><dt>상태</dt><dd>{{ sector.sectorStatus || '-' }}</dd></div>
              <div><dt>대기 차량</dt><dd>{{ sector.waitingVehicleCount || 0 }}대</dd></div>
              <div><dt>대체 장소</dt><dd>{{ sector.altWaitingArea || '-' }}</dd></div>
            </dl>
          </article>
        </div>

        <div v-else class="empty-box">
          야드 섹터 정보가 없습니다.
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="section-title">
          <h2>최근 작업 요약</h2>
        <RouterLink class="ghost-button" to="/admin/work-orders">상세 보기</RouterLink>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>운송사</th>
              <th>컨테이너</th>
              <th>야드 섹터</th>
              <th>작업 유형</th>
              <th>상태</th>
              <th>예약 시간</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in recentWorkOrders" :key="order.workOrderId">
              <td>{{ order.workOrderId }}</td>
              <td>{{ order.plateNumber || '-' }}</td>
              <td>{{ order.driverName || '-' }}</td>
              <td>{{ order.carrierName || '-' }}</td>
              <td>{{ order.containerNumber || '-' }}</td>
              <td>{{ order.sectorName || '-' }}</td>
              <td>{{ order.workType || '-' }}</td>
              <td>
                <span class="status-pill" :class="getStatusClass(order.workStatus)">
                  {{ getStatusText(order.workStatus) }}
                </span>
              </td>
              <td>{{ order.reservedTime || '-' }}</td>
            </tr>
            <tr v-if="recentWorkOrders.length === 0">
              <td colspan="9">최근 작업 정보가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-grid {
  grid-template-columns: minmax(0, 0.85fr) minmax(360px, 1.15fr);
}

.work-flow-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr) minmax(18px, 28px)) minmax(0, 1fr);
  gap: 6px;
  align-items: center;
}

.flow-card {
  display: grid;
  gap: 4px;
  min-height: 82px;
  padding: 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.flow-card span,
.flow-card small {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 800;
}

.flow-card strong {
  color: var(--ink-900);
  font-size: 20px;
  font-weight: 900;
}

.flow-arrow {
  color: var(--blue-700);
  font-size: 20px;
  font-weight: 900;
  text-align: center;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
  text-align: center;
}

.empty-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.work-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.work-card {
  display: grid;
  gap: 4px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.work-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.work-card strong {
  color: var(--ink-900);
  font-size: 22px;
  font-weight: 900;
}

.sector-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sector-card {
  display: grid;
  gap: 8px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.sector-card div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.sector-card strong {
  color: var(--ink-900);
  font-weight: 900;
}

.sector-card span,
.sector-card dt {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.sector-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.sector-card dl {
  display: grid;
  gap: 5px;
  margin: 0;
}

.sector-card dl div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
}

.sector-card dd {
  margin: 0;
  color: var(--ink-900);
  font-weight: 800;
}

.priority-card {
  display: grid;
  gap: 6px;
  min-height: 96px;
  padding: 12px;
  background: #f7f9fb;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue-700);
  border-radius: 2px;
}

.priority-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.priority-card strong {
  color: var(--ink-900);
  font-size: 26px;
  font-weight: 900;
}

.priority-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
}

.priority-card.green {
  border-left-color: var(--green-600);
}

.priority-card.amber {
  border-left-color: var(--amber-500);
}

.priority-card.red {
  border-left-color: var(--red-500);
}

@media (max-width: 1100px) {
  .dashboard-grid,
  .sector-list {
    grid-template-columns: 1fr;
  }

  .work-flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-arrow {
    display: none;
  }
}

@media (max-width: 760px) {
  .work-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .work-flow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
