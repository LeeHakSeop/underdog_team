<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useDashboardStore } from '@/stores/adminStore/dashboardStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useWeatherStore } from '@/stores/weatherStore'
import WeatherCard from '@/components/WeatherCard.vue'

const dashboardStore = useDashboardStore()
const notificationStore = useNotificationStore()
const weatherStore = useWeatherStore()
const { dashboard, loading, error, lastUpdatedAt } = storeToRefs(dashboardStore)
const { notifications } = storeToRefs(notificationStore)
const { weatherInfo, loading: weatherLoading, errMsg: weatherError } = storeToRefs(weatherStore)

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

const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const formatCount = (value) => `${value || 0}건`
const formatVehicleCount = (value) => `${value || 0}대`
const formatPercent = (value) => `${Number(value || 0).toFixed(1)}%`

const workStatusMap = {
  DISPATCH_WAITING: { label: '배차 대기', tone: 'amber' },
  APPROVED: { label: '입차 대기', tone: 'blue' },
  GATE_IN: { label: '입차 완료', tone: 'green' },
  IN_PROGRESS: { label: '작업 중', tone: 'blue' },
  COMPLETED: { label: '출차 대기', tone: 'amber' },
  GATE_OUT: { label: '출차 완료', tone: 'green' },
  CANCELED: { label: '반려', tone: 'red' },
  CANCELLED: { label: '반려', tone: 'red' },
  UNKNOWN: { label: '미지정', tone: 'gray' },
}

const sectorStatusMap = {
  NORMAL: { label: '정상', tone: 'green' },
  WARNING: { label: '주의', tone: 'amber' },
  DANGER: { label: '위험', tone: 'red' },
}

const getWorkStatus = (status) => workStatusMap[status] || { label: status || '-', tone: 'gray' }
const getSectorStatus = (status) => sectorStatusMap[status] || { label: status || '-', tone: 'gray' }

const metricCards = computed(() => [
  {
    label: '오늘 입출차',
    value: formatCount((summary.value.todayGateIn || 0) + (summary.value.todayGateOut || 0)),
    hint: `입차 ${summary.value.todayGateIn || 0}건 / 출차 ${summary.value.todayGateOut || 0}건`,
    to: '/admin/yard-map',
    tone: 'blue',
  },
  {
    label: '진행 중 작업',
    value: formatCount(summary.value.workInProgress),
    hint: `대기 ${summary.value.workReady || 0}건 / 완료 ${summary.value.workDone || 0}건`,
    to: '/admin/work-orders',
    tone: (summary.value.workInProgress || 0) > 0 ? 'blue' : 'green',
  },
  {
    label: '대기 차량',
    value: formatVehicleCount(summary.value.waitingVehicles),
    hint: `위험 섹터 ${summary.value.congestedSectors || 0}개 / 주의 ${summary.value.warningSectors || 0}개`,
    to: '/admin/yard-map',
    tone: (summary.value.congestedSectors || 0) > 0 ? 'red' : (summary.value.warningSectors || 0) > 0 ? 'amber' : 'green',
  },
  {
    label: '미처리 예외',
    value: formatCount(summary.value.exceptionOpen),
    hint: `번호판 실패 ${summary.value.recognitionFail || 0}건 / 출차 보류 ${summary.value.exitHoldContainers || 0}건`,
    to: '/admin/events',
    tone: (summary.value.exceptionOpen || 0) > 0 ? 'red' : 'green',
  },
])

const operationCards = computed(() => [
  {
    label: '승인 대기',
    value: formatCount(summary.value.pendingUsers),
    hint: `운송사 ${summary.value.pendingCarriers || 0}건 / 기사 ${summary.value.pendingDrivers || 0}건`,
    to: '/admin/members',
    tone: (summary.value.pendingUsers || 0) > 0 ? 'amber' : 'green',
  },
  {
    label: '번호판 인식률',
    value: `${recognitionRate.value}%`,
    hint: `성공 ${summary.value.recognitionSuccess || 0}건 / 실패 ${summary.value.recognitionFail || 0}건`,
    to: '/admin/plate-recognition',
    tone: recognitionRate.value >= 90 || (summary.value.recognitionTotal || 0) === 0 ? 'green' : 'amber',
  },
  {
    label: '정비 차량',
    value: formatVehicleCount(summary.value.maintenanceVehicles),
    hint: `전체 등록 차량 ${summary.value.totalVehicles || 0}대 기준`,
    to: '/admin/work-orders',
    tone: (summary.value.maintenanceVehicles || 0) > 0 ? 'amber' : 'green',
  },
  {
    label: '출차 보류',
    value: formatCount(summary.value.exitHoldContainers),
    hint: '컨테이너 출차 가능 여부 기준',
    to: '/admin/containers',
    tone: (summary.value.exitHoldContainers || 0) > 0 ? 'red' : 'green',
  },
])

const weatherReasons = computed(() => {
  if (weatherError.value || !weatherInfo.value?.available) return []
  const reasons = []
  if (Number(weatherInfo.value.windSpeed || 0) >= 10) reasons.push('강풍')
  if (Number(weatherInfo.value.rainfall || 0) >= 5) reasons.push('강수')
  if (Number(weatherInfo.value.visibility || 0) <= 5000) reasons.push('저시정')
  return reasons
})

const adminWeatherSummary = computed(() => {
  if (weatherLoading.value) {
    return { label: '기상 확인 중', tone: 'gray', reason: '운영 판단용 날씨 정보를 불러오는 중입니다.' }
  }

  if (weatherError.value || !weatherInfo.value?.available) {
    return { label: '확인 필요', tone: 'amber', reason: '기상 데이터가 없어 운영 맵과 현장 공지를 함께 확인해야 합니다.' }
  }

  if (weatherInfo.value.riskLevel === 'DANGER') {
    return {
      label: '운영 위험',
      tone: 'red',
      reason: weatherReasons.value.length ? `${weatherReasons.value.join(' / ')} 영향으로 현장 통제 여부를 확인하세요.` : '기상 위험 상태입니다.',
    }
  }

  if (weatherInfo.value.riskLevel === 'CAUTION') {
    return {
      label: '주의 운영',
      tone: 'amber',
      reason: weatherReasons.value.length ? `${weatherReasons.value.join(' / ')} 영향으로 운영 여유 시간을 두고 확인하세요.` : '기상 주의 상태입니다.',
    }
  }

  return {
    label: '정상 운영',
    tone: 'green',
    reason: '현재 부산항 날씨 기준으로 운영 제한 요인은 크지 않습니다.',
  }
})

const weatherGuideList = computed(() => {
  if (weatherLoading.value) {
    return ['부산항 기상 데이터를 확인 중입니다.']
  }

  if (weatherError.value || !weatherInfo.value?.available) {
    return [
      '기상 데이터가 없으므로 운영 맵과 현장 연락을 우선 기준으로 확인하세요.',
      '야드 혼잡도와 게이트 처리 상태를 먼저 점검하세요.',
    ]
  }

  if (weatherInfo.value.riskLevel === 'DANGER') {
    return [
      '강풍, 강수, 저시정 여부에 따라 입차 지연 가능성을 확인하세요.',
      '야드 작업 지속 여부와 기사 대기 지시를 함께 판단하세요.',
    ]
  }

  if (weatherInfo.value.riskLevel === 'CAUTION') {
    return [
      '기상 주의 상태이므로 작업 간격과 대기 시간을 여유 있게 관리하세요.',
      '운영 맵의 혼잡 섹터와 함께 확인하면 더 정확합니다.',
    ]
  }

  return [
    '현재 기상 기준으로 운영 제한 요인은 크지 않습니다.',
    '야드 혼잡과 게이트 예외를 우선 확인하세요.',
  ]
})

const getNotificationValue = (item, ...keys) => {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

const exceptionMetaMap = {
  DRIVER_ALREADY_ASSIGNED: { label: '기사 중복 배차', tone: 'red', priority: 3, guide: '기존 진행 작업과 신규 배차를 함께 확인하세요.' },
  VEHICLE_DUPLICATE_ASSIGNMENT: { label: '차량 중복 배차', tone: 'red', priority: 3, guide: '트랙터·트레일러의 현재 배차를 먼저 정리하세요.' },
  YARD_SECTOR_CAPACITY_EXCEEDED: { label: '야드 수용량 초과', tone: 'red', priority: 3, guide: '대상 섹터의 대기 차량과 작업 배치를 우선 조정하세요.' },
  DRIVER_CANNOT_ENTER: { label: '기사 출입 제한', tone: 'red', priority: 3, guide: '기사 출입 상태와 승인 여부를 우선 확인하세요.' },
  AI_SERVER_ERROR: { label: '번호판 인식 장애', tone: 'red', priority: 3, guide: '인식 장비와 수동 확인 대기 건을 함께 점검하세요.' },
  CARRIER_INACTIVE: { label: '운송사 이용 제한', tone: 'amber', priority: 2, guide: '운송사 계정 상태와 최근 승인 이력을 확인하세요.' },
  DRIVER_UNAVAILABLE: { label: '기사 배차 불가', tone: 'amber', priority: 2, guide: '기사 승인·근무 상태를 확인한 뒤 재배차하세요.' },
  VEHICLE_UNAVAILABLE: { label: '차량 배차 불가', tone: 'amber', priority: 2, guide: '차량 운행 상태와 정비 여부를 확인하세요.' },
  YARD_SECTOR_UNAVAILABLE: { label: '야드 배정 불가', tone: 'amber', priority: 2, guide: '섹터 운영 상태를 확인하고 대체 구역을 검토하세요.' },
  WORK_ORDER_NOT_APPROVED: { label: '작업 승인 대기', tone: 'amber', priority: 2, guide: '배차 승인 대기 작업 누적 여부를 먼저 점검하세요.' },
  WORK_ORDER_NOT_FOUND: { label: '작업지시 불일치', tone: 'amber', priority: 2, guide: '작업지시 누락 여부와 최근 배차 변경 이력을 확인하세요.' },
  VEHICLE_NOT_REGISTERED: { label: '미등록 차량', tone: 'amber', priority: 2, guide: '차량 등록 상태와 번호판 매칭 결과를 재확인하세요.' },
  VEHICLE_TYPE_MISMATCH: { label: '차량 유형 불일치', tone: 'amber', priority: 2, guide: '트랙터와 트레일러 연결 관계를 다시 확인하세요.' },
  PLATE_NOT_DETECTED: { label: '번호판 인식 실패', tone: 'amber', priority: 2, guide: '번호판 인식 장비 상태와 촬영 환경을 점검하세요.' },
  CONTAINER_NOT_FOUND: { label: '컨테이너 정보 없음', tone: 'amber', priority: 2, guide: '컨테이너 정보와 작업지시 연결 상태를 다시 확인하세요.' },
}

const getExceptionMeta = (type) => exceptionMetaMap[type] || {
  label: '기타 운영 예외',
  tone: 'amber',
  priority: 1,
  guide: '예외 처리 내역에서 상세 원인과 조치 이력을 확인하세요.',
}

const openExceptions = computed(() => notifications.value.filter((item) => {
  const status = getNotificationValue(item, 'processStatus', 'process_status') || 'UNPROCESSED'
  return status !== 'PROCESSED'
}))

const topExceptionGroups = computed(() => {
  const grouped = new Map()

  openExceptions.value.forEach((item) => {
    const type = getNotificationValue(item, 'exceptionType', 'exception_type') || 'EXCEPTION'
    const meta = getExceptionMeta(type)
    const current = grouped.get(type) || {
      type,
      label: meta.label,
      tone: meta.tone,
      priority: meta.priority,
      guide: meta.guide,
      count: 0,
      message: getNotificationValue(item, 'exceptionMessage', 'exception_message') || '예외 상세를 확인하세요.',
      occurredTime: getNotificationValue(item, 'occurredTime', 'occurred_time'),
    }
    current.count += 1
    grouped.set(type, current)
  })

  return Array.from(grouped.values())
    .sort((left, right) => {
      if (right.priority !== left.priority) return right.priority - left.priority
      if (right.count !== left.count) return right.count - left.count
      return new Date(right.occurredTime || 0) - new Date(left.occurredTime || 0)
    })
    .slice(0, 3)
})

const getWorkCount = (workStatus) => {
  const status = workStatusList.value.find((item) => item.workStatus === workStatus)
  return status ? status.workCount : 0
}

const workFlowCards = computed(() => [
  { label: '배차 대기', status: 'DISPATCH_WAITING', count: getWorkCount('DISPATCH_WAITING') },
  { label: '승인 완료', status: 'APPROVED', count: getWorkCount('APPROVED') },
  { label: '입차 완료', status: 'GATE_IN', count: getWorkCount('GATE_IN') },
  { label: '작업 진행', status: 'IN_PROGRESS', count: getWorkCount('IN_PROGRESS') },
  { label: '작업 완료', status: 'COMPLETED', count: getWorkCount('COMPLETED') },
  { label: '출차 완료', status: 'GATE_OUT', count: getWorkCount('GATE_OUT') },
])

const workCards = computed(() => [
  { label: '전체 작업', value: summary.value.workTotal || 0 },
  { label: '대기 작업', value: summary.value.workReady || 0 },
  { label: '진행 작업', value: summary.value.workInProgress || 0 },
  { label: '완료 작업', value: summary.value.workDone || 0 },
])

onMounted(() => {
  dashboardStore.loadDashboard()
  notificationStore.loadNotifications().catch(() => {})
  weatherStore.fetchWeather().catch(() => {})

  refreshTimer = setInterval(() => {
    if (!dashboardStore.loading) {
      dashboardStore.loadDashboard().catch(() => {})
    }
    if (!notificationStore.loading) {
      notificationStore.loadNotifications().catch(() => {})
    }
    if (!weatherStore.loading) {
      weatherStore.fetchWeather().catch(() => {})
    }
  }, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="page-stack admin-dashboard-page">
    <section class="panel">
      <div class="section-title dashboard-title">
        <div>
          <h2>운영 현황 요약</h2>
          <small>최근 갱신 {{ formatDateTime(lastUpdatedAt) }}</small>
        </div>
        <button class="ghost-button" type="button" @click="dashboardStore.loadDashboard">
          새로고침
        </button>
      </div>

      <div v-if="loading" class="empty-box">
        운영 현황 데이터를 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div v-else class="metric-grid">
        <RouterLink
          v-for="card in metricCards"
          :key="card.label"
          class="metric-card"
          :class="card.tone"
          :to="card.to"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </RouterLink>
      </div>
    </section>

    <section v-if="!loading && !error" class="grid-2 weather-dashboard-grid">
      <article class="panel weather-summary-panel" :class="adminWeatherSummary.tone">
        <div class="section-title">
          <h2>부산항 기상 판단</h2>
          <span class="status-pill" :class="adminWeatherSummary.tone">{{ adminWeatherSummary.label }}</span>
        </div>
        <p class="weather-brief">{{ adminWeatherSummary.reason }}</p>
        <div class="weather-guide-list">
          <p v-for="item in weatherGuideList" :key="item">{{ item }}</p>
        </div>
      </article>

      <WeatherCard
        :weather="weatherInfo"
        :loading="weatherLoading"
        :error="weatherError"
        title="부산항 날씨"
      />
    </section>

    <section v-if="!loading && !error" class="metric-grid">
      <RouterLink
        v-for="card in operationCards"
        :key="card.label"
        class="metric-card compact"
        :class="card.tone"
        :to="card.to"
      >
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.hint }}</small>
      </RouterLink>
    </section>

    <section v-if="!loading && !error" class="panel">
      <div class="section-title">
        <h2>미처리 예외 요약</h2>
        <RouterLink class="status-pill red" to="/admin/events">
          {{ formatCount(openExceptions.length) }}
        </RouterLink>
      </div>

      <div v-if="topExceptionGroups.length === 0" class="empty-box">
        현재 바로 확인이 필요한 예외는 없습니다.
      </div>

      <div v-else class="exception-summary-grid">
        <RouterLink
          v-for="item in topExceptionGroups"
          :key="item.type"
          class="exception-summary-card"
          :class="item.tone"
          to="/admin/events"
        >
          <span>{{ item.label }}</span>
          <strong>{{ formatCount(item.count) }}</strong>
          <p>{{ item.message }}</p>
          <small>{{ item.guide }}</small>
        </RouterLink>
      </div>
    </section>

    <section v-if="!loading && !error" class="panel">
      <div class="section-title">
        <h2>작업 흐름 현황</h2>
        <span class="status-pill">5초 갱신</span>
      </div>

      <div class="work-flow-grid">
        <template v-for="(card, index) in workFlowCards" :key="card.status">
          <article class="flow-card" :class="getWorkStatus(card.status).tone">
            <span>{{ card.label }}</span>
            <strong>{{ formatCount(card.count) }}</strong>
            <small>{{ getWorkStatus(card.status).label }}</small>
          </article>
          <span v-if="index < workFlowCards.length - 1" class="flow-arrow">></span>
        </template>
      </div>
    </section>

    <section class="grid-2 dashboard-grid">
      <article class="panel">
        <div class="section-title">
          <h2>작업 상태별 집계</h2>
          <span class="status-pill">{{ formatCount(summary.workTotal) }}</span>
        </div>

        <div class="work-card-grid">
          <div v-for="card in workCards" :key="card.label" class="work-card">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </div>
        </div>

        <div class="table-wrap">
          <table class="data-table work-status-table">
            <thead>
              <tr>
                <th>작업 상태</th>
                <th>건수</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="status in workStatusList" :key="status.workStatus">
                <td>
                  <span class="status-pill" :class="getWorkStatus(status.workStatus).tone">
                    {{ getWorkStatus(status.workStatus).label }}
                  </span>
                </td>
                <td>{{ formatCount(status.workCount) }}</td>
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
          <RouterLink class="ghost-button" to="/admin/yard-map">운영 맵</RouterLink>
        </div>

        <div v-if="sectorList.length" class="sector-list">
          <article v-for="sector in sectorList" :key="sector.sectorId" class="sector-card" :class="getSectorStatus(sector.statusLevel).tone">
            <div class="sector-head">
              <strong>{{ sector.sectorName || '-' }}</strong>
              <span class="status-pill" :class="getSectorStatus(sector.statusLevel).tone">
                {{ getSectorStatus(sector.statusLevel).label }}
              </span>
            </div>
            <p>{{ sector.guideMessage || '안내 메시지가 없습니다.' }}</p>
            <div class="usage-line">
              <i :style="{ width: `${Math.min(Number(sector.usageRate || 0), 100)}%` }"></i>
            </div>
            <dl>
              <div><dt>블록</dt><dd>{{ sector.blockName || '-' }}</dd></div>
              <div><dt>사용률</dt><dd>{{ formatPercent(sector.usageRate) }}</dd></div>
              <div><dt>대기 차량</dt><dd>{{ formatVehicleCount(sector.waitingVehicleCount) }}</dd></div>
              <div><dt>진행 작업</dt><dd>{{ formatCount(sector.workOrderCount) }}</dd></div>
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
                <span class="status-pill" :class="getWorkStatus(order.workStatus).tone">
                  {{ getWorkStatus(order.workStatus).label }}
                </span>
              </td>
              <td>{{ formatDateTime(order.reservedTime) }}</td>
            </tr>
            <tr v-if="recentWorkOrders.length === 0">
              <td colspan="9">최근 작업 데이터가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-dashboard-page {
  width: 100%;
  min-width: 0;
}

.dashboard-title {
  align-items: center;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  display: grid;
  gap: 6px;
  min-height: 116px;
  padding: 14px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue-700);
}

.metric-card.compact {
  min-height: 98px;
}

.metric-card span,
.metric-card small {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 800;
}

.metric-card strong {
  font-size: 28px;
  font-weight: 900;
}

.metric-card.green { border-left-color: var(--green-600); }
.metric-card.amber { border-left-color: var(--amber-500); }
.metric-card.red { border-left-color: var(--red-500); }

.weather-dashboard-grid {
  grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr);
}

.weather-summary-panel {
  border-left: 5px solid var(--green-600);
}

.weather-summary-panel.amber {
  border-left-color: var(--amber-500);
}

.weather-summary-panel.red {
  border-left-color: var(--red-500);
}

.weather-brief {
  margin: 0 0 10px;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.weather-guide-list {
  display: grid;
  gap: 8px;
}

.weather-guide-list p {
  margin: 0;
  padding: 10px 12px;
  color: var(--ink-700);
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.exception-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.exception-summary-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  color: inherit;
  text-decoration: none;
  background: #fff7f7;
  border: 1px solid #e4b2b2;
  border-left: 4px solid #b63a3a;
}

.exception-summary-card.amber {
  background: #fffaf0;
  border-color: #e4c277;
  border-left-color: var(--amber-500);
}

.exception-summary-card span {
  color: #8a2f2f;
  font-size: 12px;
  font-weight: 800;
}

.exception-summary-card.amber span,
.exception-summary-card.amber strong {
  color: #8a5a08;
}

.exception-summary-card strong {
  color: #7d2020;
  font-size: 20px;
  font-weight: 900;
}

.exception-summary-card p,
.exception-summary-card small {
  margin: 0;
  line-height: 1.5;
}

.exception-summary-card p {
  color: var(--ink-900);
  font-size: 13px;
  font-weight: 800;
}

.exception-summary-card small {
  color: var(--ink-600);
  font-size: 12px;
  font-weight: 700;
}

.dashboard-grid {
  grid-template-columns: minmax(0, 0.85fr) minmax(420px, 1.15fr);
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
  min-height: 92px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-top: 4px solid var(--blue-700);
}

.flow-card.green { border-top-color: var(--green-600); }
.flow-card.amber { border-top-color: var(--amber-500); }
.flow-card.red { border-top-color: var(--red-500); }

.flow-card span,
.flow-card small {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 800;
}

.flow-card strong {
  color: var(--ink-900);
  font-size: 22px;
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
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.work-card span {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 900;
}

.work-card strong {
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 900;
}

.work-status-table {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  table-layout: fixed;
}

.work-status-table th,
.work-status-table td {
  width: 50%;
}

.sector-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sector-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-left: 4px solid var(--green-600);
}

.sector-card.amber { border-left-color: var(--amber-500); }
.sector-card.red { border-left-color: var(--red-500); }

.sector-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.sector-card strong {
  color: var(--ink-900);
  font-weight: 900;
}

.sector-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.usage-line {
  height: 7px;
  overflow: hidden;
  background: #dfe6ee;
  border: 1px solid #c5cfda;
}

.usage-line i {
  display: block;
  height: 100%;
  background: #23639c;
}

.sector-card.amber .usage-line i {
  background: #c7891b;
}

.sector-card.red .usage-line i {
  background: #b8403a;
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

.sector-card dt {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.sector-card dd {
  margin: 0;
  color: var(--ink-900);
  font-weight: 800;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid,
  .sector-list,
  .weather-dashboard-grid,
  .exception-summary-grid {
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
  .metric-grid,
  .work-card-grid,
  .work-flow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
