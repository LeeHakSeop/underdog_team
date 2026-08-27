<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { readCurrentUser } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'
import { useWeatherStore } from '@/stores/weatherStore'
import WeatherCard from '@/components/WeatherCard.vue'

const currentUser = readCurrentUser()
const notificationStore = useNotificationStore()
const carrierStore = useCarrierStore()
const driverStore = useDriverStore()
const weatherStore = useWeatherStore()
const { carriers } = storeToRefs(carrierStore)
const { drivers } = storeToRefs(driverStore)
const { notifications } = storeToRefs(notificationStore)
const { weatherInfo, loading: weatherLoading, errMsg: weatherError } = storeToRefs(weatherStore)

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]

const myCarrier = computed(() => carriers.value.find(
  (carrier) => String(getId(carrier, 'userId')) === String(currentUser?.userId),
))

const carrierName = computed(() => {
  if (carrierStore.loading) return '운송사 정보를 불러오는 중입니다.'
  return myCarrier.value?.carrierName || myCarrier.value?.carrier_name || '운송사 정보 미등록'
})

const carrierManagerName = computed(() =>
  myCarrier.value?.managerName || myCarrier.value?.manager_name || currentUser?.displayName || '-',
)

const myDrivers = computed(() => {
  if (!myCarrier.value) return []
  const carrierId = getId(myCarrier.value, 'carrierId')
  return drivers.value.filter((driver) => getId(driver, 'carrierId') === carrierId)
})

const availableMyDrivers = computed(() =>
  myDrivers.value.filter((driver) => driver.canEnter === true || driver.can_enter === true),
)

const waitingDrivers = computed(() =>
  myDrivers.value.filter((driver) => !(driver.canEnter === true || driver.can_enter === true)),
)

const getNotificationValue = (item, ...keys) => {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

const myDriverPlates = computed(() => new Set(
  myDrivers.value
    .map((driver) => driver.tractorPlateNumber || driver.tractor_plate_number)
    .filter(Boolean)
    .map((plate) => String(plate).trim()),
))

const carrierExceptions = computed(() => notifications.value.filter((item) => {
  const status = getNotificationValue(item, 'processStatus', 'process_status') || 'UNPROCESSED'
  const plateNumber = String(getNotificationValue(item, 'plateNumber', 'plate_number') || '').trim()
  return status !== 'PROCESSED' && plateNumber && myDriverPlates.value.has(plateNumber)
}))

const carrierExceptionSummary = computed(() => {
  if (carrierExceptions.value.length === 0) {
    return {
      label: '예외 없음',
      tone: 'green',
      reason: '현재 소속 기사 기준으로 확인이 필요한 예외가 없습니다.',
    }
  }

  const typeCount = new Map()
  carrierExceptions.value.forEach((item) => {
    const type = getNotificationValue(item, 'exceptionType', 'exception_type') || 'EXCEPTION'
    typeCount.set(type, (typeCount.get(type) || 0) + 1)
  })

  const [topType] = Array.from(typeCount.entries()).sort((left, right) => right[1] - left[1])[0] || ['EXCEPTION', 0]
  const reasonMap = {
    DRIVER_CANNOT_ENTER: '기사 출입 제한이 있어 배차 지연 가능성이 있습니다.',
    WORK_ORDER_NOT_APPROVED: '작업 승인 대기 건이 있어 출발 지시 전 확인이 필요합니다.',
    VEHICLE_NOT_REGISTERED: '차량 등록 문제로 게이트 통과가 지연될 수 있습니다.',
    PLATE_NOT_DETECTED: '번호판 인식 실패가 반복되면 게이트 처리 시간이 늘어납니다.',
  }

  return {
    label: `예외 ${carrierExceptions.value.length}건`,
    tone: 'red',
    reason: reasonMap[topType] || '소속 기사/차량 예외가 있어 배차 전 상태 확인이 필요합니다.',
  }
})

const exceptionActionMap = {
  DRIVER_CANNOT_ENTER: '기사 출입 승인 상태와 게이트 통과 이력을 먼저 확인하세요.',
  WORK_ORDER_NOT_APPROVED: '작업 승인 상태를 확인한 뒤 출발 순서를 다시 조정하세요.',
  VEHICLE_NOT_REGISTERED: '차량 등록 상태와 번호판 정보를 우선 확인하세요.',
  PLATE_NOT_DETECTED: '번호판 촬영 상태와 차량 이미지 품질을 다시 확인하세요.',
  DRIVER_ALREADY_ASSIGNED: '기사 중복 배정',
  DRIVER_UNAVAILABLE: '기사 배정 불가',
  CARRIER_INACTIVE: '운송사 계정 상태를 관리자에게 즉시 확인 요청하세요.',
}

const carrierExceptionCards = computed(() => {
  const grouped = new Map()

  carrierExceptions.value.forEach((item) => {
    const type = getNotificationValue(item, 'exceptionType', 'exception_type') || 'EXCEPTION'
    const current = grouped.get(type) || {
      type,
      count: 0,
      sample: getNotificationValue(item, 'exceptionMessage', 'exception_message') || '상세 예외 확인 필요',
    }
    current.count += 1
    grouped.set(type, current)
  })

  const labelMap = {
    DRIVER_CANNOT_ENTER: '기사 출입 제한',
    WORK_ORDER_NOT_APPROVED: '작업 승인 대기',
    VEHICLE_NOT_REGISTERED: '차량 미등록',
    PLATE_NOT_DETECTED: '번호판 인식 실패',
    CARRIER_INACTIVE: '운송사 비활성',
  }

  return Array.from(grouped.values())
    .sort((left, right) => right.count - left.count)
    .slice(0, 3)
    .map((item) => ({
      key: item.type,
      label: labelMap[item.type] || item.type,
      count: item.count,
      message: item.sample,
      action: exceptionActionMap[item.type] || '차량, 기사, 작업 승인 상태를 함께 확인하세요.',
      tone: ['DRIVER_CANNOT_ENTER', 'VEHICLE_NOT_REGISTERED', 'CARRIER_INACTIVE'].includes(item.type) ? 'red' : 'amber',
    }))
})

const weatherReasons = computed(() => {
  const weather = weatherInfo.value
  if (!weather?.available) return []

  const reasons = []
  if (Number(weather.windSpeed || 0) >= 10) reasons.push('강풍')
  if (Number(weather.rainfall || 0) >= 5) reasons.push('강수')
  if (Number(weather.visibility || 0) <= 5000) reasons.push('저시정')
  return reasons
})

const weatherImpact = computed(() => {
  const weather = weatherInfo.value

  if (weatherLoading.value) {
    return { label: '기상 확인 중', tone: 'gray', reason: '배차 영향 정보를 불러오는 중입니다.' }
  }

  if (weatherError.value || !weather?.available) {
    return { label: '확인 필요', tone: 'gray', reason: '기상 정보를 확인할 수 없어 현장 공지를 우선 확인하세요.' }
  }

  if (weather.riskLevel === 'DANGER') {
    return {
      label: '배차 지연 위험',
      tone: 'red',
      reason: weatherReasons.value.length
        ? `${weatherReasons.value.join(' / ')} 영향으로 관리자 확인이 필요합니다.`
        : '기상 위험으로 관리자 확인이 필요합니다.',
    }
  }

  if (weather.riskLevel === 'CAUTION') {
    return {
      label: '일부 지연 가능',
      tone: 'amber',
      reason: weatherReasons.value.length
        ? `${weatherReasons.value.join(' / ')} 영향으로 배차 여유 시간을 확보하세요.`
        : '기상 조건 확인이 필요합니다.',
    }
  }

  return { label: '배차 정상', tone: 'green', reason: '현재 기상 기준으로 배차 진행 가능합니다.' }
})

const dispatchChecklist = computed(() => {
  if (weatherLoading.value) {
    return [
      '부산항 날씨를 확인하는 중입니다.',
      '출발 예정 기사 안내 전 갱신 결과를 먼저 확인하세요.',
    ]
  }

  if (weatherError.value || !weatherInfo.value?.available) {
    return [
      '기상 정보를 확인할 수 없습니다.',
      '현장 공지와 기사 연락 상태를 먼저 확인하세요.',
    ]
  }

  if (weatherInfo.value.riskLevel === 'DANGER') {
    return [
      '출발 예정 기사와 도착 예정 시간을 다시 확인하세요.',
      '입차 승인 상태와 야드 대기 여부를 관리자에게 확인하세요.',
      '지연 가능 기사에게 선제 안내를 보내세요.',
    ]
  }

  if (weatherInfo.value.riskLevel === 'CAUTION') {
    return [
      '강풍·강수·저시정 여부를 확인하세요.',
      '배차 간격을 조금 넓히고 도착 여유 시간을 확보하세요.',
      '입차 대기 기사 우선순위를 다시 점검하세요.',
    ]
  }

  return [
    '현재 배차는 정상 진행 가능합니다.',
    '승인 대기 기사와 차량 상태를 함께 확인하세요.',
  ]
})

const dispatchStatusCards = computed(() => [
  {
    label: '배차 판단',
    value: weatherImpact.value.label,
    hint: weatherImpact.value.reason,
    tone: weatherImpact.value.tone,
  },
  {
    label: '기상 원인',
    value: weatherInfo.value?.available ? (weatherReasons.value.join(' / ') || '영향 없음') : '확인 필요',
    hint: '배차 지연 사유 요약',
    tone: weatherImpact.value.tone,
  },
  {
    label: '데이터 상태',
    value: weatherInfo.value?.stale ? '마지막 정상 데이터' : '실시간',
    hint: weatherInfo.value?.updatedAt
      ? `업데이트 ${String(weatherInfo.value.updatedAt).replace('T', ' ').slice(0, 16)}`
      : '업데이트 정보 없음',
    tone: weatherInfo.value?.stale ? 'amber' : 'green',
  },
  {
    label: '배차 예외',
    value: carrierExceptionSummary.value.label,
    hint: carrierExceptionSummary.value.reason,
    tone: carrierExceptionSummary.value.tone,
  },
])

const actionCards = computed(() => [
  {
    title: '승인/회원 관리',
    count: waitingDrivers.value.length,
    text: '기사 출입 승인과 소속 차량 승인 현황을 함께 확인합니다.',
    path: '/carrier/driver-approval',
  },
  {
    title: '배정/작업 조회',
    count: myDrivers.value.length,
    text: '배정 현황과 작업지시 목록, 완료 이력을 확인합니다.',
    path: '/carrier/inquiry',
  },
  {
    title: '배정/작업 입력',
    count: myDrivers.value.length,
    text: '트레일러 배정과 승인 전 작업지시를 입력하고 수정합니다.',
    path: '/carrier/input',
  },
])

const loadData = () => {
  carrierStore.loadCarriers().catch(() => {})
  driverStore.loadDrivers().catch(() => {})
  notificationStore.loadNotifications().catch(() => {})
  weatherStore.fetchWeather().catch(() => {})
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="dispatch-weather-banner" :class="weatherImpact.tone">
      <div>
        <span>기상 배차 영향</span>
        <strong>{{ weatherImpact.label }}</strong>
        <p>{{ weatherImpact.reason }}</p>
      </div>
      <WeatherCard
        :weather="weatherInfo"
        :loading="weatherLoading"
        :error="weatherError"
        title="부산항 배차 날씨"
        mode="carrier"
      />
    </section>

    <section class="grid-3 carrier-dispatch-grid">
      <article
        v-for="card in dispatchStatusCards"
        :key="card.label"
        class="dispatch-status-card"
        :class="card.tone"
      >
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.hint }}</small>
      </article>
    </section>

    <section class="panel dispatch-guide-panel">
      <div class="section-title">
        <h2>배차 판단 가이드</h2>
        <span class="status-pill" :class="weatherImpact.tone">{{ weatherImpact.label }}</span>
      </div>
      <div class="dispatch-guide-list">
        <p v-for="item in dispatchChecklist" :key="item">{{ item }}</p>
      </div>
    </section>

    <section class="panel exception-panel">
      <div class="section-title">
        <h2>배차 예외 확인</h2>
        <span class="status-pill" :class="carrierExceptionSummary.tone">{{ carrierExceptionSummary.label }}</span>
      </div>

      <div v-if="carrierExceptions.length === 0" class="empty-box">
        현재 소속 기사 기준으로 확인이 필요한 미처리 예외가 없습니다.
      </div>

      <div v-else class="exception-card-list">
        <article
          v-for="card in carrierExceptionCards"
          :key="card.key"
          class="exception-card"
          :class="card.tone"
        >
          <div class="exception-card-head">
            <strong>{{ card.label }}</strong>
            <span>{{ card.count }}건</span>
          </div>
          <p>{{ card.message }}</p>
          <small>{{ card.action }}</small>
        </article>
      </div>
    </section>

    <section v-if="carrierStore.error || driverStore.error" class="panel error-panel">
      {{ carrierStore.error || driverStore.error }}
    </section>

    <section class="carrier-identity" aria-label="현재 로그인 운송사">
      <div class="identity-item">
        <span>운송사명</span>
        <strong>{{ carrierName }}</strong>
      </div>
      <div class="identity-item manager-item">
        <span>담당자명</span>
        <strong>{{ carrierManagerName }}</strong>
      </div>
    </section>

    <section class="grid-4">
      <article class="metric-card">
        <span>소속 기사</span>
        <strong>{{ myDrivers.length }}</strong>
        <small>DB 기준</small>
      </article>

      <article class="metric-card">
        <span>출입 가능 기사</span>
        <strong>{{ availableMyDrivers.length }}</strong>
        <small>승인 완료</small>
      </article>

      <article class="metric-card">
        <span>승인 대기</span>
        <strong>{{ waitingDrivers.length }}</strong>
        <small>확인 필요</small>
      </article>

      <article class="metric-card">
        <span>운송사</span>
        <strong>{{ myCarrier ? 1 : 0 }}</strong>
        <small>로그인 계정 기준</small>
      </article>
    </section>

    <section class="grid-3 carrier-action-grid">
      <RouterLink v-for="card in actionCards" :key="card.title" class="carrier-action-card" :to="card.path">
        <span>{{ card.title }}</span>
        <strong>{{ card.count }}</strong>
        <p>{{ card.text }}</p>
      </RouterLink>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>소속 기사</h2>
        <span class="status-pill green">DB 조회</span>
      </div>

      <div v-if="driverStore.loading" class="empty-box">
        불러오는 중입니다.
      </div>

      <div v-else-if="myDrivers.length === 0" class="empty-box">
        소속 기사가 없습니다.
      </div>

      <div v-else class="driver-list">
        <div v-for="driver in myDrivers" :key="driver.driverId || driver.driver_id" class="driver-row">
          <div>
            <b>{{ driver.driverName || driver.driver_name }}</b>
            <span>{{ driver.driverContact || driver.driver_contact || '-' }}</span>
          </div>
          <span class="status-pill" :class="driver.canEnter || driver.can_enter ? 'green' : 'red'">
            {{ driver.canEnter || driver.can_enter ? '출입 가능' : '승인 대기' }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.carrier-identity {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(220px, 1fr);
  gap: 0;
  overflow: hidden;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #8fa8bf;
  border-left: 5px solid var(--blue-700);
  border-radius: 2px;
}

.dispatch-weather-banner {
  display: grid;
  grid-template-columns: minmax(220px, 0.7fr) minmax(0, 1.3fr);
  gap: 10px;
  align-items: stretch;
  padding: 12px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-left: 5px solid var(--green-600);
}

.dispatch-weather-banner.amber { border-left-color: var(--amber-500); }
.dispatch-weather-banner.red { border-left-color: var(--red-500); }
.dispatch-weather-banner.gray { border-left-color: #7b8794; }

.dispatch-weather-banner > div {
  display: grid;
  gap: 5px;
  align-content: center;
  min-width: 0;
}

.dispatch-weather-banner span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.dispatch-weather-banner strong {
  color: #173b60;
  font-size: 24px;
  font-weight: 900;
}

.dispatch-weather-banner p {
  margin: 0;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
}

.carrier-dispatch-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dispatch-status-card {
  display: grid;
  gap: 6px;
  min-height: 102px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  border-left: 4px solid var(--blue-700);
}

.dispatch-status-card.green { border-left-color: var(--green-600); }
.dispatch-status-card.amber { border-left-color: var(--amber-500); }
.dispatch-status-card.red { border-left-color: var(--red-500); }
.dispatch-status-card.gray { border-left-color: #7b8794; }

.dispatch-status-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.dispatch-status-card strong {
  color: var(--ink-900);
  font-size: 22px;
  font-weight: 900;
}

.dispatch-status-card small {
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.dispatch-guide-panel {
  display: grid;
  gap: 10px;
}

.dispatch-guide-list {
  display: grid;
  gap: 8px;
}

.dispatch-guide-list p {
  margin: 0;
  padding: 10px 12px;
  color: var(--ink-700);
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.exception-panel {
  display: grid;
  gap: 10px;
}

.exception-card-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.exception-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  border-left: 4px solid var(--blue-700);
}

.exception-card.amber {
  background: #fff8ea;
  border-left-color: var(--amber-500);
}

.exception-card.red {
  background: #fff4f4;
  border-left-color: var(--red-500);
}

.exception-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.exception-card-head strong {
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 900;
}

.exception-card-head span,
.exception-card p,
.exception-card small {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.identity-item {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px 16px;
}

.identity-item + .identity-item {
  border-left: 1px solid #c4d0dc;
}

.carrier-identity span {
  display: block;
  color: #40566d;
  font-size: 12px;
  font-weight: 800;
}

.carrier-identity strong {
  display: block;
  overflow: hidden;
  color: #163d64;
  font-size: 19px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.manager-item {
  background: #eef4f9;
}

.manager-item strong {
  color: #1f2933;
  font-size: 17px;
}

.carrier-action-card {
  display: grid;
  gap: 6px;
  min-height: 104px;
  padding: 13px;
  background: #f7f9fb;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue-700);
  border-radius: 2px;
}

.carrier-action-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.carrier-action-card strong {
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 900;
}

.carrier-action-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
}

.driver-list {
  display: grid;
  gap: 10px;
}

.driver-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
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
  overflow-wrap: anywhere;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.error-panel {
  color: #991b1b;
  background: #fff1f1;
  border-color: #fecaca;
}

@media (max-width: 900px) {
  .carrier-identity,
  .dispatch-weather-banner,
  .carrier-dispatch-grid,
  .exception-card-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .identity-item + .identity-item {
    border-top: 1px solid #c4d0dc;
    border-left: 0;
  }

  .driver-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
