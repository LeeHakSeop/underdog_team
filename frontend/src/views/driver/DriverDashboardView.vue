<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { readCurrentUser } from '@/stores/authStore'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { useWeatherStore } from '@/stores/weatherStore'
import WeatherCard from '@/components/WeatherCard.vue'
import { vehicleTypeLabel } from '@/config/vehicleType'
import { booleanLabel, displayTone, workStatusLabel } from '@/config/displayLabels'

const currentUser = readCurrentUser()
const carrierStore = useCarrierStore()
const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()
const weatherStore = useWeatherStore()

const { carriers } = storeToRefs(carrierStore)
const {
  drivers,
  myWorkOrders,
  workHistory,
  loading,
  error,
  actionMessage,
  actionResult,
} = storeToRefs(driverStore)
const { myVehicle } = storeToRefs(vehicleStore)
const { weatherInfo, loading: weatherLoading, errMsg: weatherError } = storeToRefs(weatherStore)

const selectedWorkStatus = ref('')
const historyQuery = ref('')
const historyPage = ref(1)
const historyPageSize = 5
const processingId = ref(null)
let refreshTimer = null

const loginUser = computed(() => JSON.parse(localStorage.getItem('portGateUser') || 'null'))

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

const workStatusOptions = [
  'DISPATCH_WAITING',
  'APPROVED',
  'GATE_IN',
  'IN_PROGRESS',
  'COMPLETED',
  'GATE_OUT',
  'CANCELED',
]

const filteredWorkOrders = computed(() => {
  if (!selectedWorkStatus.value) return myWorkOrders.value
  return myWorkOrders.value.filter((order) => order.workStatus === selectedWorkStatus.value)
})

const completedWorkOrderCount = computed(() =>
  myWorkOrders.value.filter((order) => ['COMPLETED', 'GATE_OUT'].includes(order.workStatus)).length,
)

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
  if (order.workStatus === 'GATE_IN') return '입차 후 지정 섹터로 이동해 작업을 시작하세요.'
  if (!order.isApproved) return '관리자 승인 후 게이트 입차가 가능합니다.'
  if (!order.canEnter) return '기사 출입 가능 상태를 운송사 또는 관리자에게 확인하세요.'
  return `${order.destinationSectorName || order.sectorName || '지정 섹터'}로 이동 후 안내 메시지를 확인하세요.`
})

const workflowSteps = computed(() => {
  const status = currentWorkOrder.value?.workStatus
  const completeIndex = {
    DISPATCH_WAITING: 0,
    APPROVED: 1,
    GATE_IN: 2,
    IN_PROGRESS: 3,
    COMPLETED: 4,
    GATE_OUT: 4,
  }[status] ?? -1

  return [
    { label: '배차 확인', done: completeIndex >= 0 },
    { label: '입차 승인', done: completeIndex >= 1 },
    { label: '입차 완료', done: completeIndex >= 2 },
    { label: '작업 진행', done: completeIndex >= 3 },
    { label: '작업 완료', done: completeIndex >= 4 },
  ]
})

const startReady = computed(() => currentWorkOrder.value?.workStatus === 'GATE_IN')
const completeReady = computed(() => currentWorkOrder.value?.workStatus === 'IN_PROGRESS')

const currentActionLabel = computed(() => {
  if (startReady.value) return '작업 시작'
  if (completeReady.value) return '작업 완료'
  return ''
})


const workflowStatusSummary = computed(() => {
  const order = currentWorkOrder.value

  if (!order) {
    return {
      tone: 'info',
      step: '배차 대기',
      message: '새 작업이 배정되면 이 화면에서 바로 확인할 수 있습니다.',
    }
  }

  if (!order.isApproved) {
    return {
      tone: 'warning',
      step: '관리자 승인 대기',
      message: '배차 승인 전에는 입차와 작업 시작이 제한됩니다.',
    }
  }

  if (!order.canEnter) {
    return {
      tone: 'danger',
      step: '출입 상태 확인 필요',
      message: '기사 또는 차량 출입 가능 상태를 먼저 확인해야 합니다.',
    }
  }

  if (order.workStatus === 'APPROVED') {
    return {
      tone: 'blue',
      step: '게이트 입차 대기',
      message: '게이트 입차 완료 후 작업 시작 버튼이 활성화됩니다.',
    }
  }

  if (order.workStatus === 'GATE_IN') {
    return {
      tone: 'blue',
      step: '작업 시작 가능',
      message: '현재 작업 시작 처리가 가능한 상태입니다.',
    }
  }

  if (order.workStatus === 'IN_PROGRESS') {
    return {
      tone: 'blue',
      step: '작업 완료 처리 대기',
      message: '작업이 끝나면 완료 처리 후 출차 가능 여부를 확인하세요.',
    }
  }

  if (order.workStatus === 'COMPLETED') {
    return {
      tone: order.canExit ? 'green' : 'warning',
      step: order.canExit ? '출차 가능' : '관리자 확인 대기',
      message: order.canExit
        ? '출차 게이트로 이동해 최종 출차 처리하면 됩니다.'
        : '작업은 끝났지만 출차 전 관리자 확인이 남아 있습니다.',
    }
  }

  return {
    tone: 'green',
    step: '작업 종료',
    message: '현재 배정 작업이 마무리된 상태입니다.',
  }
})

const actionExceptionGuideMap = {
  WORK_ORDER_NOT_FOUND: {
    label: '작업 없음',
    tone: 'warning',
    guide: '배정 작업이 사라졌습니다. 관리자에게 현재 배차 상태를 확인하세요.',
  },
  WORK_ORDER_NOT_APPROVED: {
    label: '승인 필요',
    tone: 'warning',
    guide: '관리자 승인 전에는 진행할 수 없습니다. 승인 상태를 먼저 확인하세요.',
  },
  WORK_ORDER_ALREADY_STARTED: {
    label: '이미 시작됨',
    tone: 'info',
    guide: '같은 작업이 이미 진행 중입니다. 현재 작업 상태를 새로고침 후 확인하세요.',
  },
  WORK_ORDER_ALREADY_COMPLETED: {
    label: '이미 완료됨',
    tone: 'info',
    guide: '작업 완료 처리된 건입니다. 출차 가능 여부만 확인하면 됩니다.',
  },
  WORK_ORDER_ALREADY_CLOSED: {
    label: '출차 완료',
    tone: 'info',
    guide: '이미 출차까지 끝난 작업입니다. 다른 배정 작업을 확인하세요.',
  },
  WORK_ORDER_NOT_GATE_IN: {
    label: '입차 확인 필요',
    tone: 'warning',
    guide: '게이트 입차 완료 후에만 작업 시작이 가능합니다.',
  },
  WORK_ORDER_NOT_IN_PROGRESS: {
    label: '진행 상태 아님',
    tone: 'warning',
    guide: '작업 완료는 작업 진행 상태에서만 가능합니다. 현재 상태를 다시 확인하세요.',
  },
  CONTAINER_NOT_FOUND: {
    label: '컨테이너 확인 필요',
    tone: 'danger',
    guide: '컨테이너 정보가 없어 완료 처리할 수 없습니다. 관리자에게 즉시 확인 요청이 필요합니다.',
  },
  CONTAINER_UPDATE_FAILED: {
    label: '컨테이너 갱신 실패',
    tone: 'danger',
    guide: '컨테이너 출차 가능 상태 반영에 실패했습니다. 운영자 확인 후 다시 시도하세요.',
  },
  WORK_ORDER_UPDATE_FAILED: {
    label: '상태 반영 실패',
    tone: 'danger',
    guide: '상태 저장이 실패했습니다. 잠시 후 다시 시도하거나 관리자에게 알려주세요.',
  },
  WORK_ORDER_REQUEST_FAILED: {
    label: '통신 실패',
    tone: 'danger',
    guide: '서버 통신 중 문제가 발생했습니다. 네트워크 상태와 관리자 공지를 확인하세요.',
  },
}

const actionExceptionInfo = computed(() => {
  if (!actionResult.value || actionResult.value.success !== false) return null
  return actionExceptionGuideMap[actionResult.value.exceptionType] || {
    label: '처리 확인 필요',
    tone: 'warning',
    guide: '작업 상태를 다시 확인한 뒤 필요하면 관리자에게 문의하세요.',
  }
})

const actionDisabledReason = computed(() => {
  const order = currentWorkOrder.value
  if (!order) return '진행할 작업이 없습니다.'
  if (order.workStatus === 'DISPATCH_WAITING') return '배차 승인 후 진행할 수 있습니다.'
  if (order.workStatus === 'APPROVED') return '게이트 입차 완료 후 시작할 수 있습니다.'
  if (order.workStatus === 'COMPLETED') return order.canExit ? '출차 게이트로 이동하세요.' : '관리자 출차 확인을 기다리세요.'
  if (order.workStatus === 'GATE_OUT') return '이미 출차가 완료된 작업입니다.'
  return ''
})

const operationAlerts = computed(() => {
  const alerts = []
  const order = currentWorkOrder.value

  if (!order) {
    alerts.push({
      tone: 'info',
      title: '현재 배정 작업이 없습니다.',
      message: '새 배차가 들어오면 이 화면에서 바로 확인할 수 있습니다.',
    })
    return alerts
  }

  if (!order.isApproved) {
    alerts.push({
      tone: 'warning',
      title: '배차 승인 대기',
      message: '관리자 승인 전에는 게이트 입차와 작업 진행이 제한됩니다.',
    })
  }

  if (!order.canEnter) {
    alerts.push({
      tone: 'danger',
      title: '출입 가능 상태 확인 필요',
      message: '기사 또는 차량 출입 상태가 제한되어 있을 수 있습니다. 운송사 또는 관리자에게 확인하세요.',
    })
  }

  if (weatherError.value || !weatherInfo.value?.available) {
    alerts.push({
      tone: 'warning',
      title: '부산항 날씨 확인 필요',
      message: weatherInfo.value?.errorMessage || weatherError.value || '실시간 날씨 정보를 확인할 수 없어 현장 공지와 관리자 안내를 먼저 확인하세요.',
    })
  } else if (weatherInfo.value?.fallbackUsed || weatherInfo.value?.stale) {
    alerts.push({
      tone: 'info',
      title: '마지막 정상 날씨 기준 안내',
      message: weatherInfo.value.errorMessage || '실시간 호출에 실패해 마지막 정상 데이터를 기준으로 안내하고 있습니다.',
    })
  }

  if (weatherInfo.value?.riskLevel === 'CAUTION') {
    alerts.push({
      tone: 'warning',
      title: '부산항 기상 주의',
      message: weatherInfo.value.guideMessage || '현장 이동 전 기상 변화를 확인하세요.',
    })
  }

  if (weatherInfo.value?.riskLevel === 'DANGER') {
    alerts.push({
      tone: 'danger',
      title: '부산항 기상 위험',
      message: weatherInfo.value.guideMessage || '현장 통제 여부와 관리자 안내를 먼저 확인하세요.',
    })
  }

  if (actionExceptionInfo.value) {
    alerts.unshift({
      tone: actionExceptionInfo.value.tone,
      title: actionExceptionInfo.value.label,
      message: actionExceptionInfo.value.guide,
    })
  }

  return alerts.slice(0, 3)
})

const completedHistory = computed(() => {
  const latestCompleted = new Map()

  workHistory.value
    .filter((history) => ['COMPLETED', 'GATE_OUT'].includes(history.newStatus))
    .forEach((history) => {
      const current = latestCompleted.get(history.workOrderId)
      const currentTime = current?.changedTime ? new Date(current.changedTime).getTime() : 0
      const nextTime = history.changedTime ? new Date(history.changedTime).getTime() : 0
      if (!current || nextTime >= currentTime) {
        latestCompleted.set(history.workOrderId, history)
      }
    })

  return Array.from(latestCompleted.values())
})

const filteredCompletedHistory = computed(() => {
  const keyword = historyQuery.value.trim().toLowerCase()
  if (!keyword) return completedHistory.value
  return completedHistory.value.filter((history) => {
    const searchText = [
      history.workOrderId,
      history.workType,
      history.plateNumber,
      history.containerNumber,
      history.newStatus,
      workStatusLabel(history.newStatus),
      history.changedTime,
    ].join(' ').toLowerCase()
    return searchText.includes(keyword)
  })
})

const historyPageCount = computed(() =>
  Math.max(1, Math.ceil(filteredCompletedHistory.value.length / historyPageSize)),
)

const pagedCompletedHistory = computed(() => {
  const start = (historyPage.value - 1) * historyPageSize
  return filteredCompletedHistory.value.slice(start, start + historyPageSize)
})

watch(historyQuery, () => {
  historyPage.value = 1
})

watch(historyPageCount, (pageCount) => {
  if (historyPage.value > pageCount) historyPage.value = pageCount
})

const getBooleanText = (value) => {
  if (value === true) return '승인'
  if (value === false) return '미승인'
  return '-'
}

const getWorkStatusClass = (status) => displayTone('work', status)
const getWorkStatusText = (status) => workStatusLabel(status)
const getEntryClass = (value) => (value ? 'green' : 'red')

const formatReservedTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const formatHistoryTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const handleWorkAction = async (action) => {
  const order = currentWorkOrder.value
  const userId = loginUser.value?.userId
  if (!order || !userId) return

  processingId.value = order.workOrderId

  try {
    if (action === 'start') {
      await driverStore.startAssignedWork(order.workOrderId, userId)
    } else {
      await driverStore.completeAssignedWork(order.workOrderId, userId)
    }
  } finally {
    processingId.value = null
  }
}

onMounted(async () => {
  weatherStore.fetchWeather().catch(() => {})

  if (loginUser.value?.userId) {
    await Promise.allSettled([
      driverStore.loadDrivers(),
      carrierStore.loadCarriers(),
      driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId),
      driverStore.loadMyWorkHistory(loginUser.value.userId),
    ])

    if (myDriver.value?.driverId) {
      vehicleStore.loadVehicleByDriver(myDriver.value.driverId).catch(() => {})
    }

    refreshTimer = setInterval(() => {
      if (!driverStore.loading) {
        driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId).catch(() => {})
        driverStore.loadMyWorkHistory(loginUser.value.userId).catch(() => {})
      }
      if (!weatherStore.loading) {
        weatherStore.fetchWeather().catch(() => {})
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

    <section class="panel">
      <div class="section-title">
        <h2>현재 배정 작업</h2>
        <span class="work-list-count">완료 건수 <strong>{{ completedWorkOrderCount }}건</strong></span>
      </div>

      <div v-if="loading && myWorkOrders.length === 0" class="empty-panel">
        작업 정보를 불러오는 중입니다.
      </div>

      <div v-else-if="error && myWorkOrders.length === 0" class="empty-panel warning">
        {{ error }}
      </div>

      <template v-else-if="currentWorkOrder">
        <div class="work-summary">
          <div>
            <span>트랙터 번호</span>
            <strong>{{ currentWorkOrder.plateNumber || '-' }}</strong>
          </div>
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
          <div>
            <span>출발 Yard</span>
            <strong>{{ currentWorkOrder.startSectorName || '-' }}</strong>
          </div>
          <div>
            <span>목적 Yard</span>
            <strong>{{ currentWorkOrder.destinationSectorName || '-' }}</strong>
          </div>
        </div>

        <div class="workflow-strip" aria-label="작업 진행 상태">
          <div
            v-for="(step, index) in workflowSteps"
            :key="step.label"
            class="workflow-step"
            :class="{ done: step.done }"
          >
            <b>{{ index + 1 }}</b>
            <span>{{ step.label }}</span>
          </div>
        </div>

        <div class="workflow-status-card" :class="workflowStatusSummary.tone">
          <strong>{{ workflowStatusSummary.step }}</strong>
          <p>{{ workflowStatusSummary.message }}</p>
        </div>

        <div v-if="actionMessage" class="action-feedback success" role="status">
          {{ actionMessage }}
        </div>
        <div v-if="error && myWorkOrders.length > 0" class="action-feedback warning" role="alert">
          {{ error }}
        </div>

        <div class="driver-action-bar">
          <div class="action-guide">
            <strong>{{ passStatus }}</strong>
            <p>{{ nextGuide }}</p>
          </div>
          <div class="action-buttons">
            <button
              v-if="startReady"
              class="primary-button"
              type="button"
              :disabled="processingId === currentWorkOrder.workOrderId"
              @click="handleWorkAction('start')"
            >
              {{ processingId === currentWorkOrder.workOrderId ? '처리 중...' : currentActionLabel }}
            </button>
            <button
              v-else-if="completeReady"
              class="primary-button complete"
              type="button"
              :disabled="processingId === currentWorkOrder.workOrderId"
              @click="handleWorkAction('complete')"
            >
              {{ processingId === currentWorkOrder.workOrderId ? '처리 중...' : currentActionLabel }}
            </button>
            <span v-else class="action-hint">{{ actionDisabledReason }}</span>
          </div>
        </div>
      </template>

      <div v-else class="empty-panel">
        로그인한 기사에게 배정된 작업 정보가 없습니다.
      </div>
    </section>

    <section class="tractor-overview panel">
      <div class="section-title">
        <h2>내 트랙터 / 차량 정보</h2>
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

    <WeatherCard
      :weather="weatherInfo"
      :loading="weatherLoading"
      :error="weatherError"
      title="부산항 날씨"
      mode="driver"
    />

    <section class="panel">
      <div class="section-title">
        <h2>작업 전 확인</h2>
        <span class="status-pill blue">현장 안내</span>
      </div>

      <div class="alert-grid">
        <article
          v-for="alert in operationAlerts"
          :key="`${alert.tone}-${alert.title}`"
          class="alert-card"
          :class="alert.tone"
        >
          <strong>{{ alert.title }}</strong>
          <p>{{ alert.message }}</p>
        </article>
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
        <strong>{{ currentWorkOrder.destinationSectorName || '-' }}</strong>
        <p>{{ currentWorkOrder.guideMessage || '야드 안내 메시지가 없습니다.' }}</p>
      </article>
      <article class="driver-pass-card">
        <span>예약 시간</span>
        <strong>{{ formatReservedTime(currentWorkOrder.reservedTime) }}</strong>
        <p>{{ currentWorkOrder.containerNumber || '-' }} / {{ currentWorkOrder.workType || '-' }}</p>
      </article>
    </section>

    <section v-if="currentWorkOrder" class="grid-2 driver-grid">
      <article class="panel">
        <div class="section-title">
          <h2>컨테이너 / 야드 안내</h2>
          <span class="status-pill green">{{ currentWorkOrder.destinationSectorName || '-' }}</span>
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
            <tr><th>현재 야드 섹터</th><td>{{ currentWorkOrder.sectorName || '-' }}</td></tr>
            <tr><th>출발 Yard</th><td>{{ currentWorkOrder.startSectorName || '-' }}</td></tr>
            <tr><th>목적 Yard</th><td>{{ currentWorkOrder.destinationSectorName || '-' }}</td></tr>
            <tr><th>섹터 상태</th><td>{{ currentWorkOrder.sectorStatus || '-' }}</td></tr>
            <tr><th>대체 대기장소</th><td>{{ currentWorkOrder.altWaitingArea || '-' }}</td></tr>
            <tr><th>중간 상태 안내</th><td>{{ currentWorkOrder.guideMessage || '-' }}</td></tr>
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
      <div class="section-title work-list-title">
        <h2>작업 목록</h2>
        <div class="work-list-tools">
          <label for="driver-work-status">작업 상태</label>
          <select id="driver-work-status" v-model="selectedWorkStatus">
            <option value="">전체</option>
            <option v-for="status in workStatusOptions" :key="status" :value="status">
              {{ getWorkStatusText(status) }}
            </option>
          </select>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>작업 유형</th>
              <th>트랙터</th>
              <th>트레일러</th>
              <th>컨테이너</th>
              <th>출발 Yard</th>
              <th>목적 Yard</th>
              <th>예약 시간</th>
              <th>상태</th>
              <th>승인</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredWorkOrders" :key="order.workOrderId">
              <td>{{ order.workOrderId }}</td>
              <td>{{ order.workType || '-' }}</td>
              <td>{{ order.plateNumber || '-' }}</td>
              <td>{{ order.trailerPlateNumber || '-' }}</td>
              <td>{{ order.containerNumber || '-' }}</td>
              <td>{{ order.startSectorName || '-' }}</td>
              <td>{{ order.destinationSectorName || '-' }}</td>
              <td>{{ formatReservedTime(order.reservedTime) }}</td>
              <td>
                <span class="status-pill" :class="getWorkStatusClass(order.workStatus)">
                  {{ getWorkStatusText(order.workStatus) }}
                </span>
              </td>
              <td>{{ getBooleanText(order.isApproved) }}</td>
            </tr>
            <tr v-if="filteredWorkOrders.length === 0">
              <td colspan="9">
                {{ myWorkOrders.length === 0 ? '조회된 작업 정보가 없습니다.' : '선택한 상태의 작업이 없습니다.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title history-title">
        <h2>작업 이력</h2>
        <span class="status-pill green">총 {{ completedHistory.length }}건</span>
      </div>

      <div class="history-tools">
        <label for="driver-history-search">이력 검색</label>
        <input
          id="driver-history-search"
          v-model="historyQuery"
          type="search"
          placeholder="작업 ID, 작업 유형, 컨테이너 번호"
        />
      </div>

      <div v-if="filteredCompletedHistory.length === 0" class="empty-panel">
        완료된 작업 이력이 없습니다.
      </div>

      <template v-else>
        <div class="table-wrap">
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
                  <span class="status-pill green">{{ getWorkStatusText(history.newStatus) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="history-pagination">
          <button class="ghost-button" type="button" :disabled="historyPage === 1" @click="historyPage -= 1">이전</button>
          <span>{{ historyPage }} / {{ historyPageCount }}</span>
          <button class="ghost-button" type="button" :disabled="historyPage === historyPageCount" @click="historyPage += 1">다음</button>
        </div>
      </template>
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
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 12px;
  min-width: 0;
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

.empty-panel.warning,
.action-feedback.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.workflow-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.workflow-step {
  display: grid;
  gap: 6px;
  justify-items: center;
  padding: 10px 8px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  color: var(--ink-500);
}

.workflow-step b {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  background: #d9e3ee;
  border-radius: 50%;
  color: #36516e;
}

.workflow-step span {
  font-size: 12px;
  font-weight: 800;
  text-align: center;
  overflow-wrap: anywhere;
}

.workflow-step.done {
  background: #eef8f1;
  border-color: #a7cfb2;
  color: #1d5b39;
}

.workflow-step.done b {
  background: #2f8a5d;
  color: #ffffff;
}

.workflow-status-card {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  background: #f8fafc;
}

.workflow-status-card strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 900;
}

.workflow-status-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.workflow-status-card.warning {
  border-color: #f5d38a;
  background: #fff8e7;
}

.workflow-status-card.danger {
  border-color: #fecaca;
  background: #fff1f2;
}

.workflow-status-card.blue {
  border-color: #bfd5ee;
  background: #f2f7fc;
}

.workflow-status-card.green {
  border-color: #b7ebc9;
  background: #ecfdf3;
}

.action-feedback {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #9fd6ba;
  background: #eef8f1;
  color: #1d5b39;
  font-size: 13px;
  font-weight: 800;
}

.driver-action-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, auto);
  gap: 12px;
  align-items: center;
  margin-top: 12px;
  padding: 12px 14px;
  background: #f7f9fb;
  border: 1px solid var(--line);
}

.action-guide {
  display: grid;
  gap: 4px;
}

.action-guide strong {
  color: #173b60;
  font-size: 18px;
}

.action-guide p,
.action-hint {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.action-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.primary-button.complete {
  background: #177245;
}

.driver-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}

.alert-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.alert-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--line);
  background: #f7f9fb;
}

.alert-card strong {
  color: #173b60;
  font-size: 14px;
  font-weight: 900;
}

.alert-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.5;
}

.alert-card.info {
  border-left: 4px solid #2f6fad;
}

.alert-card.warning {
  background: #fff8ea;
  border-color: #ebcf8b;
  border-left: 4px solid #cc8a12;
}

.alert-card.danger {
  background: #fff4f4;
  border-color: #e4a6a6;
  border-left: 4px solid #b63a3a;
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

.work-list-title,
.history-title {
  align-items: center;
  gap: 12px;
}

.work-list-tools,
.history-tools {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: auto;
}

.history-tools {
  margin: 0 0 10px;
}

.work-list-tools label,
.history-tools label {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.work-list-tools select,
.history-tools input {
  min-width: 150px;
  min-height: 32px;
  padding: 4px 8px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
  font: inherit;
}

.history-tools input {
  width: min(100%, 280px);
  min-width: 180px;
}

.work-list-count {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  color: var(--ink-500);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.work-list-count strong {
  color: var(--ink-900);
  font-size: 14px;
}

.history-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

@media (max-width: 900px) {
  .driver-identity,
  .tractor-summary,
  .driver-operation-panel,
  .driver-grid,
  .alert-grid,
  .workflow-strip,
  .work-summary,
  .driver-action-bar {
    grid-template-columns: 1fr;
  }

  .identity-item + .identity-item {
    border-top: 1px solid var(--line);
    border-left: 0;
  }

  .work-list-tools,
  .history-tools {
    flex-wrap: wrap;
    justify-content: flex-start;
    margin-left: 0;
  }

  .history-tools input {
    width: 100%;
    min-width: 0;
  }

  .action-buttons {
    justify-content: flex-start;
  }
}
</style>
