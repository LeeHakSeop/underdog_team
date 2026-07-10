<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useDashboardStore } from '@/stores/adminStore/dashboardStore'

const dashboardStore = useDashboardStore()
const { dashboard, loading, error } = storeToRefs(dashboardStore)

const summary = computed(() => dashboard.value?.summary || {})
const workStatusList = computed(() => dashboard.value?.workStatusList || [])
const recentWorkOrders = computed(() => dashboard.value?.recentWorkOrders || [])
const sectorList = computed(() => dashboard.value?.sectorList || [])

const recognitionRate = computed(() => {
  const total = summary.value.recognitionTotal || 0
  const success = summary.value.recognitionSuccess || 0

  if (total === 0) {
    return 0
  }

  return Math.round((success / total) * 100)
})

const metricCards = computed(() => [
  {
    label: '가입 승인 대기',
    value: summary.value.pendingUsers || 0,
    hint: `운송사 ${summary.value.pendingCarriers || 0} / 기사 ${summary.value.pendingDrivers || 0}`,
  },
  {
    label: '오늘 게이트 처리',
    value: (summary.value.todayGateIn || 0) + (summary.value.todayGateOut || 0),
    hint: `입차 ${summary.value.todayGateIn || 0} / 출차 ${summary.value.todayGateOut || 0}`,
  },
  {
    label: '번호판 인식 성공률',
    value: `${recognitionRate.value}%`,
    hint: `성공 ${summary.value.recognitionSuccess || 0} / 실패 ${summary.value.recognitionFail || 0}`,
  },
  {
    label: '미처리 예외',
    value: summary.value.exceptionOpen || 0,
    hint: 'exception_log 기준',
  },
])

const workCards = computed(() => [
  { label: '전체 작업', value: summary.value.workTotal || 0 },
  { label: '대기', value: summary.value.workReady || 0 },
  { label: '진행', value: summary.value.workInProgress || 0 },
  { label: '완료', value: summary.value.workDone || 0 },
])

onMounted(() => {
  dashboardStore.loadDashboard()
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>관리자 대시보드</h2>
        <button class="ghost-button" type="button" @click="dashboardStore.loadDashboard">
          새로고침
        </button>
      </div>

      <div v-if="loading" class="empty-box">
        대시보드 현황을 불러오는 중입니다.
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

    <section class="grid-2 dashboard-grid">
      <article class="panel">
        <div class="section-title">
          <h2>작업 진행 현황</h2>
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
                <td>{{ status.workStatus }}</td>
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
          <h2>야드 섹터 현황</h2>
          <span class="status-pill">{{ sectorList.length }}개</span>
        </div>

        <div class="sector-list">
          <article v-for="sector in sectorList" :key="sector.sectorId" class="sector-card">
            <div>
              <strong>{{ sector.sectorName || '-' }}</strong>
              <span>{{ sector.blockName || '-' }}</span>
            </div>
            <p>{{ sector.guideMessage || '안내 메시지가 없습니다.' }}</p>
            <dl>
              <div>
                <dt>상태</dt>
                <dd>{{ sector.sectorStatus || '-' }}</dd>
              </div>
              <div>
                <dt>대기 차량</dt>
                <dd>{{ sector.waitingVehicleCount || 0 }}대</dd>
              </div>
              <div>
                <dt>대체 장소</dt>
                <dd>{{ sector.altWaitingArea || '-' }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>최근 작업정보</h2>
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
              <td><span class="status-pill">{{ order.workStatus || '-' }}</span></td>
              <td>{{ order.reservedTime || '-' }}</td>
            </tr>
            <tr v-if="recentWorkOrders.length === 0">
              <td colspan="9">최근 작업정보가 없습니다.</td>
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

@media (max-width: 1100px) {
  .dashboard-grid,
  .sector-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .work-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
