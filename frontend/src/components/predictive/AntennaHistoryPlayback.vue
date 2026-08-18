<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  fetchPredictiveEquipment,
  fetchPredictiveEvents,
  fetchPredictiveMetadata,
  fetchPredictiveSensorData,
} from '@/api/predictiveMaintenanceApi'
import {
  DEMO_EQUIPMENT_ID,
  DEMO_SOURCE_EQUIPMENT_ID,
  predictiveDemoSession,
  requestDemoNotification,
  resetDemoNotifications,
} from '@/config/predictiveDemoSession'

const HOUR_MS = 60 * 60 * 1000
const WINDOW_MS = 14 * 24 * HOUR_MS
const PROGRESSION_THRESHOLD = 0.4564690824

const metrics = {
  successRate: { sensor: 'success_rate', label: '통신 성공률', unit: '%', color: '#23639c', decimals: 2, range: [60, 100] },
  responseTimeMs: { sensor: 'response_time_ms', label: '응답 시간', unit: 'ms', color: '#b47c1c', decimals: 1, range: [30, 230] },
  packetLossRate: { sensor: 'packet_loss_rate', label: '패킷 손실률', unit: '%', color: '#b8403a', decimals: 2, range: [0, 15] },
  trafficLoad: { sensor: 'traffic_load', label: '트래픽 부하', unit: '', color: '#2b8d9c', decimals: 3, range: [0, 1], comparison: 'context' },
  temperatureC: { sensor: 'temperature_c', label: '온도', unit: '℃', color: '#d16a32', decimals: 1, range: [10, 45], comparison: 'context' },
  voltageV: { sensor: 'voltage_v', label: '전압', unit: 'V', color: '#6a7f36', decimals: 2, range: [11, 13] },
  signalStrengthDbm: { sensor: 'signal_strength_dbm', label: '신호 세기', unit: 'dBm', color: '#4277a8', decimals: 1, range: [50, 100] },
  retryCount: { sensor: 'retry_count', label: '재시도 횟수', unit: '회', color: '#8d6b3f', decimals: 0, range: [0, 20] },
  disconnectCount: { sensor: 'disconnect_count', label: '연결 끊김', unit: '회', color: '#9c5362', decimals: 0, range: [0, 10] },
  errorCount: { sensor: 'error_count', label: '오류 횟수', unit: '회', color: '#6c5c91', decimals: 0, range: [0, 25] },
}

const equipmentData = ref({})
const equipmentCodes = ref([])
const loading = ref(true)
const loadError = ref('')
const selectedEquipment = ref(predictiveDemoSession.selectedEquipmentId || DEMO_EQUIPMENT_ID)
const selectedMetric = ref('successRate')
const observationTime = ref(0)
const isPlaying = ref(false)
const playbackHours = ref(6)
const maintenancePlaybackMode = ref(false)
const axisMode = ref('operational')
const datasetVersion = ref('CSV')
const sensorLimits = ref({})
const alertEvents = ref([])
const canvas = ref(null)
const chartWrap = ref(null)
let timerId = null
let resizeObserver = null

const parseCollectedAt = (value) => {
  if (!value) return 0
  return new Date(value.includes('T') ? value : value.replace(' ', 'T')).getTime()
}

const equipmentOptions = computed(() => [DEMO_EQUIPMENT_ID, ...equipmentCodes.value].sort((a, b) => {
  if (a === DEMO_EQUIPMENT_ID) return -1
  if (b === DEMO_EQUIPMENT_ID) return 1
  return a.localeCompare(b)
}))
const equipmentRecords = computed(() => equipmentData.value[selectedEquipment.value] ?? [])
const isDemoEquipment = computed(() => selectedEquipment.value === DEMO_EQUIPMENT_ID)

const minTime = computed(() => equipmentRecords.value[0]?.timestamp ?? 0)
const maxTime = computed(() => equipmentRecords.value.at(-1)?.timestamp ?? 0)

const visibleRecords = computed(() => {
  const start = observationTime.value - WINDOW_MS
  return equipmentRecords.value.filter(
    (item) => item.timestamp >= start && item.timestamp <= observationTime.value,
  )
})

const currentRecord = computed(() => visibleRecords.value.at(-1))
const selectedEquipmentLimits = computed(() => sensorLimits.value[selectedEquipment.value] ?? {})
const selectedMetricLimit = computed(() => {
  const metric = metrics[selectedMetric.value]
  return selectedEquipmentLimits.value[metric.sensor] ?? null
})
const previous24HourRecords = computed(() => {
  if (!currentRecord.value) return []
  const end = currentRecord.value.timestamp
  return equipmentRecords.value.filter(
    (item) => item.timestamp < end && item.timestamp >= end - 24 * HOUR_MS,
  )
})

const metricComparisons = computed(() =>
  Object.fromEntries(
    Object.entries(metrics).map(([key, metric]) => {
      if (!currentRecord.value) {
        return [key, { status: 'normal', text: '비교할 데이터 없음' }]
      }

      if (metric.comparison === 'context') {
        return [key, { status: 'reference', text: '일중 변동 참고 · 경보 판단 제외' }]
      }

      const baselineValues = previous24HourRecords.value.map((item) => item[key])
      if (!baselineValues.length) {
        return [key, { status: 'normal', text: '24시간 기준 데이터 부족' }]
      }

      const baseline = baselineValues.reduce((sum, value) => sum + value, 0) / baselineValues.length
      const value = currentRecord.value[key]
      const delta = value - baseline
      const baselineText = `${baseline.toFixed(metric.decimals)}${metric.unit}`
      const deltaUnit = ['successRate', 'packetLossRate'].includes(key) ? '%p' : metric.unit
      const deltaText = `${delta > 0 ? '+' : ''}${delta.toFixed(Math.max(1, metric.decimals))}${deltaUnit}`

      const limit = selectedEquipmentLimits.value[metric.sensor]
      const isAbnormal = limit?.direction === 'high'
        ? value > limit.limit
        : limit?.direction === 'low'
          ? value < limit.limit
          : false
      const status = isAbnormal ? 'danger' : 'normal'
      const limitText = limit
        ? ` · 한계 ${limit.direction === 'high' ? '≤' : '≥'} ${Number(limit.limit).toFixed(metric.decimals)}${metric.unit}`
        : ''

      const text = status === 'normal'
        ? `24시간 평균 ${baselineText} · 특이 변화 없음${limitText}`
        : `24시간 평균 ${baselineText} · 대비 ${deltaText}${limitText}`

      return [key, { status, text }]
    }),
  ),
)

const firstFailureTime = computed(
  () => equipmentRecords.value.find((item) => item.failureEvent === 1)?.timestamp ?? 0,
)
const firstMaintenanceTime = computed(
  () => equipmentRecords.value.find((item) => item.maintenanceEvent === 1)?.timestamp ?? 0,
)
const hasMaintenanceEpisode = computed(
  () => Boolean(firstFailureTime.value && firstMaintenanceTime.value),
)
const currentAlertEvent = computed(() => {
  const candidates = alertEvents.value.filter((item) => {
    if (item.equipmentId !== selectedEquipment.value) return false
    return !firstFailureTime.value || parseCollectedAt(item.occurredAt) <= firstFailureTime.value
  })
  return candidates.at(-1)
})
const isSuddenFailure = computed(() =>
  hasMaintenanceEpisode.value && !currentAlertEvent.value,
)
const maintenanceStartTime = computed(() =>
  hasMaintenanceEpisode.value ? Math.max(minTime.value, firstFailureTime.value - WINDOW_MS) : minTime.value,
)
const maintenanceEndTime = computed(() =>
  hasMaintenanceEpisode.value
    ? Math.min(maxTime.value, firstMaintenanceTime.value + 24 * HOUR_MS)
    : maxTime.value,
)
const activeMinTime = computed(() =>
  maintenancePlaybackMode.value ? maintenanceStartTime.value : minTime.value,
)
const activeMaxTime = computed(() =>
  maintenancePlaybackMode.value ? maintenanceEndTime.value : maxTime.value,
)

const riskClass = computed(() => {
  const state = currentRecord.value?.operationalState
  if (state === 'failure' || state === 'failure_expected') return 'red'
  if (state === 'suspect' || state === 'risk' || state === 'post_failure_recovery') return 'amber'
  return state ? 'green' : 'blue'
})

const operationalDecision = computed(() => {
  if (!currentRecord.value) return '기록 없음'
  return currentRecord.value.operationalStateKo
})

const sensorWarningConfirmed = computed(() =>
  ['failure_expected', 'failure'].includes(currentRecord.value?.operationalState),
)

const progressionAdvisory = computed(() => {
  const probability = currentRecord.value?.progressionProbability
  if (probability === undefined || probability === null) {
    return {
      label: '보조 모델 미연결',
      detail: '센서 규칙만으로 공식 상태를 판정합니다.',
      className: 'blue',
    }
  }

  const supportsWarning = probability >= PROGRESSION_THRESHOLD
  const percentage = `${(probability * 100).toFixed(1)}%`
  const modelName = currentRecord.value?.progressionModel || 'Random Forest'

  if (sensorWarningConfirmed.value && supportsWarning) {
    return { label: '모델 동의', detail: `${modelName} ${percentage}`, className: 'red' }
  }
  if (sensorWarningConfirmed.value) {
    return {
      label: '모델 이견 · 경보 유지',
      detail: `${modelName} ${percentage} · 센서 경보를 취소하지 않음`,
      className: 'amber',
    }
  }
  if (supportsWarning) {
    return {
      label: '집중 관찰',
      detail: `${modelName} ${percentage} · 공식 고장 예상 전 단계`,
      className: 'amber',
    }
  }
  return { label: '모델 특이사항 없음', detail: `${modelName} ${percentage}`, className: 'green' }
})

const toDateTimeLocal = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const offset = date.getTimezoneOffset() * 60 * 1000
  return new Date(timestamp - offset).toISOString().slice(0, 16)
}

const formatDateTime = (timestamp) =>
  new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(timestamp))

const formatAxisDate = (timestamp) =>
  new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
  })
    .format(new Date(timestamp))
    .replace(/\s/g, '')
    .replace(/\.$/, '')

const formatValue = (value, metricKey = selectedMetric.value) => {
  if (value === undefined || value === null) return '-'
  const metric = metrics[metricKey]
  return `${Number(value).toFixed(metric.decimals)}${metric.unit}`
}

const formatCollectedAt = (timestamp) => {
  const date = new Date(timestamp)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

const loadEquipmentRows = async (equipmentId) => {
  if (!equipmentId || equipmentId === DEMO_EQUIPMENT_ID || equipmentData.value[equipmentId]) return
  const [rows, events] = await Promise.all([
    fetchPredictiveSensorData(equipmentId),
    fetchPredictiveEvents(equipmentId),
  ])
  equipmentData.value = {
    ...equipmentData.value,
    [equipmentId]: rows.map((item) => ({
      ...item,
      timestamp: parseCollectedAt(item.collectedAt),
    })),
  }
  alertEvents.value = [
    ...alertEvents.value.filter((item) => item.equipmentId !== equipmentId),
    ...events.filter((item) => item.eventType === 'FAILURE_EXPECTED'),
  ]
}

const clampTime = (timestamp) => Math.min(activeMaxTime.value, Math.max(activeMinTime.value, timestamp))

const setObservationTime = (timestamp) => {
  observationTime.value = clampTime(timestamp)
}

const onDateTimeChange = (event) => {
  maintenancePlaybackMode.value = false
  const timestamp = new Date(event.target.value).getTime()
  if (!Number.isNaN(timestamp)) setObservationTime(timestamp)
}

const moveTime = (hours) => {
  setObservationTime(observationTime.value + hours * HOUR_MS)
  if (observationTime.value >= activeMaxTime.value) stopPlayback()
}

const startMaintenancePlayback = () => {
  if (!hasMaintenanceEpisode.value) return
  stopPlayback()
  if (isDemoEquipment.value) resetDemoNotifications()
  maintenancePlaybackMode.value = true
  setObservationTime(maintenanceStartTime.value)
  startPlayback()
}

const startPlayback = () => {
  if (isPlaying.value) {
    stopPlayback()
    return
  }
  isPlaying.value = true
  timerId = window.setInterval(() => moveTime(playbackHours.value), 650)
}

function stopPlayback() {
  isPlaying.value = false
  if (timerId) window.clearInterval(timerId)
  timerId = null
}

const drawChart = () => {
  const target = canvas.value
  const wrap = chartWrap.value
  if (!target || !wrap) return

  const width = Math.max(640, wrap.clientWidth)
  const height = 310
  const dpr = window.devicePixelRatio || 1
  target.width = width * dpr
  target.height = height * dpr
  target.style.width = `${width}px`
  target.style.height = `${height}px`

  const context = target.getContext('2d')
  context.scale(dpr, dpr)
  context.clearRect(0, 0, width, height)

  const padding = { top: 24, right: 24, bottom: 42, left: 62 }
  const plotWidth = width - padding.left - padding.right
  const plotHeight = height - padding.top - padding.bottom

  if (visibleRecords.value.length < 2) {
    context.fillStyle = '#fbfcfe'
    context.fillRect(padding.left, padding.top, plotWidth, plotHeight)
    context.fillStyle = '#5d6875'
    context.font = '13px Malgun Gothic'
    context.fillText('선택한 시점에 표시할 데이터가 없습니다.', padding.left + 16, padding.top + 30)
    return
  }

  const metric = metrics[selectedMetric.value]
  const values = visibleRecords.value.map((item) => item[selectedMetric.value])
  let low
  let high

  if (axisMode.value === 'operational') {
    ;[low, high] = metric.range
  } else {
    low = Math.min(...values)
    high = Math.max(...values)
    const valuePadding = Math.max((high - low) * 0.12, selectedMetric.value === 'successRate' ? 0.5 : 1)
    low -= valuePadding
    high += valuePadding
  }

  const rawYForValue = (value) =>
    padding.top + ((high - value) / (high - low)) * plotHeight
  const yForValue = (value) =>
    Math.min(padding.top + plotHeight, Math.max(padding.top, rawYForValue(value)))

  context.fillStyle = '#fbfcfe'
  context.fillRect(padding.left, padding.top, plotWidth, plotHeight)

  const limit = selectedMetricLimit.value
  if (axisMode.value === 'operational' && limit) {
    const fillBand = (from, to, color) => {
      const top = rawYForValue(Math.max(from, to))
      const bottom = rawYForValue(Math.min(from, to))
      context.fillStyle = color
      context.fillRect(padding.left, top, plotWidth, bottom - top)
    }

    if (limit.direction === 'high') {
      fillBand(low, limit.limit, '#f1f8f4')
      fillBand(limit.limit, high, '#fff0ef')
    } else {
      fillBand(limit.limit, high, '#f1f8f4')
      fillBand(low, limit.limit, '#fff0ef')
    }
  }

  context.font = '11px Malgun Gothic'
  context.textAlign = 'right'
  context.textBaseline = 'middle'
  for (let row = 0; row <= 4; row += 1) {
    const y = padding.top + (plotHeight * row) / 4
    const value = high - ((high - low) * row) / 4
    context.strokeStyle = '#dbe3eb'
    context.lineWidth = 1
    context.beginPath()
    context.moveTo(padding.left, y)
    context.lineTo(width - padding.right, y)
    context.stroke()
    context.fillStyle = '#5d6875'
    context.fillText(value.toFixed(metric.decimals), padding.left - 10, y)
  }

  const windowStart = observationTime.value - WINDOW_MS
  const xForTime = (timestamp) =>
    padding.left + ((timestamp - windowStart) / WINDOW_MS) * plotWidth

  context.textAlign = 'center'
  context.textBaseline = 'top'
  for (let day = 0; day <= 14; day += 2) {
    const timestamp = windowStart + day * 24 * HOUR_MS
    const x = xForTime(timestamp)
    context.fillStyle = '#5d6875'
    context.fillText(formatAxisDate(timestamp), x, height - padding.bottom + 12)
  }

  const firstVisiblePrecursor = visibleRecords.value.find(
    (item) => item.stateChanged && item.operationalState === 'failure_expected',
  )
  const firstVisibleFailure = visibleRecords.value.find((item) => item.currentFailure)
  const firstVisibleMaintenance = visibleRecords.value.find((item) => item.maintenanceEvent === 1)
  const events = [
    firstVisiblePrecursor && { item: firstVisiblePrecursor, label: '고장 예상', color: '#b47c1c' },
    firstVisibleFailure && {
      item: firstVisibleFailure,
      label: isSuddenFailure.value ? '급작 고장' : '고장',
      color: '#b8403a',
    },
    firstVisibleMaintenance && { item: firstVisibleMaintenance, label: '정비', color: '#2f7d57' },
  ].filter(Boolean)
  events.forEach(({ item, label, color }) => {
    const x = xForTime(item.timestamp)
    context.setLineDash([5, 4])
    context.strokeStyle = color
    context.beginPath()
    context.moveTo(x, padding.top)
    context.lineTo(x, padding.top + plotHeight)
    context.stroke()
    context.setLineDash([])
    context.fillStyle = color
    context.textAlign = 'center'
    context.textBaseline = 'bottom'
    context.fillText(label, x, padding.top - 5)
  })

  context.strokeStyle = metric.color
  context.lineWidth = 2
  context.lineJoin = 'round'
  context.beginPath()
  visibleRecords.value.forEach((item, index) => {
    const x = xForTime(item.timestamp)
    const y = yForValue(item[selectedMetric.value])
    if (index === 0) context.moveTo(x, y)
    else context.lineTo(x, y)
  })
  context.stroke()

  const last = visibleRecords.value.at(-1)
  context.fillStyle = metric.color
  context.beginPath()
  context.arc(xForTime(last.timestamp), yForValue(last[selectedMetric.value]), 4, 0, Math.PI * 2)
  context.fill()
}

watch([visibleRecords, selectedMetric, axisMode], () => nextTick(drawChart))

watch(observationTime, (nextTime, previousTime) => {
  predictiveDemoSession.observationTime = nextTime

  // 알림은 시간축을 정방향으로 통과할 때만 생성한다.
  // 슬라이더나 -6시간 버튼으로 과거로 돌아갈 때는 어떤 알림도 요청하지 않는다.
  if (!isDemoEquipment.value || !Number.isFinite(previousTime) || nextTime <= previousTime) return

  if (previousTime < predictiveDemoSession.alertAt && nextTime >= predictiveDemoSession.alertAt) {
    requestDemoNotification('FAILURE_EXPECTED', predictiveDemoSession.alertAt)
  }
  if (previousTime < predictiveDemoSession.failureAt && nextTime >= predictiveDemoSession.failureAt) {
    requestDemoNotification('FAILURE', predictiveDemoSession.failureAt)
  }
})

watch(selectedEquipment, async () => {
  stopPlayback()
  maintenancePlaybackMode.value = false
  predictiveDemoSession.selectedEquipmentId = selectedEquipment.value
  try {
    await loadEquipmentRows(selectedEquipment.value)
  } catch (error) {
    loadError.value = error.message
    return
  }
  const nextTime = hasMaintenanceEpisode.value
    ? maintenanceStartTime.value
    : Math.max(minTime.value, maxTime.value - WINDOW_MS)
  setObservationTime(nextTime)
})

watch(playbackHours, () => {
  if (!isPlaying.value) return
  stopPlayback()
  startPlayback()
})

onMounted(async () => {
  try {
    const [equipment, metadata] = await Promise.all([
      fetchPredictiveEquipment(),
      fetchPredictiveMetadata(),
    ])
    equipmentCodes.value = equipment.map((item) => item.equipmentCode)
    datasetVersion.value = `${metadata.datasetVersion || 'CSV'} · DB API`
    sensorLimits.value = metadata.sensorLimits || {}

    await loadEquipmentRows(DEMO_SOURCE_EQUIPMENT_ID)
    const sourceRows = equipmentData.value[DEMO_SOURCE_EQUIPMENT_ID] || []
    const sourceAlert = alertEvents.value
      .filter((item) => item.equipmentId === DEMO_SOURCE_EQUIPMENT_ID)
      .at(-1)
    const sourceAlertAt = sourceAlert ? parseCollectedAt(sourceAlert.occurredAt) : 0
    const sessionWasInitialized = predictiveDemoSession.initialized
    const targetAlertAt = sessionWasInitialized
      ? predictiveDemoSession.alertAt
      : new Date().setMinutes(0, 0, 0) + 6 * HOUR_MS
    const demoOffset = targetAlertAt - sourceAlertAt
    const demoRows = sourceRows.map((item) => {
      const timestamp = item.timestamp + demoOffset
      return {
        ...item,
        equipmentId: DEMO_EQUIPMENT_ID,
        timestamp,
        collectedAt: formatCollectedAt(timestamp),
      }
    })
    const demoAlert = sourceAlert && {
      ...sourceAlert,
      id: `${DEMO_EQUIPMENT_ID}-${formatCollectedAt(targetAlertAt)}`,
      equipmentId: DEMO_EQUIPMENT_ID,
      occurredAt: formatCollectedAt(targetAlertAt),
      deliveryStatus: 'DEMO_PENDING',
      message: `[시연용 고장 예상] ${DEMO_EQUIPMENT_ID}에서 고장 전조가 확인되었습니다.`,
    }

    equipmentData.value = {
      ...equipmentData.value,
      [DEMO_EQUIPMENT_ID]: demoRows,
    }
    sensorLimits.value[DEMO_EQUIPMENT_ID] = sensorLimits.value[DEMO_SOURCE_EQUIPMENT_ID]
    if (demoAlert) alertEvents.value.push(demoAlert)

    const demoFailure = demoRows.find((item) => item.currentFailure)
    const demoMaintenance = demoRows.find((item) => item.maintenanceEvent === 1)
    predictiveDemoSession.initialized = true
    predictiveDemoSession.alertAt = targetAlertAt
    predictiveDemoSession.failureAt = demoFailure?.timestamp || 0
    predictiveDemoSession.maintenanceAt = demoMaintenance?.timestamp || 0
    predictiveDemoSession.anomalyCountAtAlert = demoAlert?.anomalyCount || 0
    predictiveDemoSession.anomalyCountAtFailure = demoFailure?.anomalyCount || 0
    predictiveDemoSession.maintenanceState = demoMaintenance?.operationalStateKo || '정상'

    if (!isDemoEquipment.value) await loadEquipmentRows(selectedEquipment.value)
    observationTime.value = isDemoEquipment.value && sessionWasInitialized && predictiveDemoSession.observationTime
      ? predictiveDemoSession.observationTime
      : hasMaintenanceEpisode.value
        ? maintenanceStartTime.value
        : Math.max(minTime.value, maxTime.value - WINDOW_MS)
  } catch (error) {
    loadError.value = error.message
  } finally {
    loading.value = false
    await nextTick()
    drawChart()
  }

  resizeObserver = new ResizeObserver(drawChart)
  if (chartWrap.value) resizeObserver.observe(chartWrap.value)
})

onBeforeUnmount(() => {
  stopPlayback()
  resizeObserver?.disconnect()
})
</script>

<template>
  <section class="panel playback-panel">
    <div class="section-title playback-heading">
      <h2>안테나 상태 기록 재생</h2>
      <span class="status-pill blue">{{ datasetVersion }} · 최근 14일</span>
    </div>

    <div v-if="loading" class="message-state">데이터를 불러오는 중입니다.</div>
    <div v-else-if="loadError" class="message-state error">{{ loadError }}</div>

    <template v-else>
      <div class="playback-toolbar">
        <label>
          <span>안테나</span>
          <select v-model="selectedEquipment">
            <option v-for="equipment in equipmentOptions" :key="equipment" :value="equipment">
              {{ equipment === DEMO_EQUIPMENT_ID ? '시연용 안테나 (ANT-018 복제)' : equipment }}
            </option>
          </select>
        </label>

        <label>
          <span>관찰 시점</span>
          <input
            type="datetime-local"
            :value="toDateTimeLocal(observationTime)"
            :min="toDateTimeLocal(minTime)"
            :max="toDateTimeLocal(maxTime)"
            @change="onDateTimeChange"
          />
        </label>

        <div class="maintenance-range">
          <span>정비 구간</span>
          <div v-if="hasMaintenanceEpisode" class="maintenance-range-row">
            <button type="button" class="maintenance-play-button" @click="startMaintenancePlayback">
              정비 구간 재생
            </button>
            <small>{{ formatDateTime(maintenanceStartTime) }} ~ {{ formatDateTime(maintenanceEndTime) }}</small>
            <span v-if="currentAlertEvent" class="alert-badge available">
              카카오 알림 기준 {{ formatDateTime(parseCollectedAt(currentAlertEvent.occurredAt)) }}
            </span>
            <span v-else class="alert-badge unavailable">사전 알림 없음 · 급작 고장</span>
          </div>
          <div v-else class="no-maintenance">이 안테나는 고장·정비 기록이 없습니다.</div>
        </div>
      </div>

      <div class="playback-summary">
        <div>
          <span>관찰 중인 시점</span>
          <strong>{{ formatDateTime(observationTime) }}</strong>
        </div>
        <div>
          <span>운영 상태</span>
          <strong><span class="status-pill" :class="riskClass">{{ operationalDecision }}</span></strong>
          <small>센서 5개 이상 6시간 지속 시 고장 예상</small>
        </div>
        <div>
          <span>이상 센서</span>
          <strong>{{ currentRecord?.anomalyCount ?? 0 }} / 8개</strong>
          <small>L2 현재고장 확률 {{ ((currentRecord?.currentFaultProbability ?? 0) * 100).toFixed(1) }}%</small>
        </div>
        <div>
          <span>전조 진행 보조판단</span>
          <strong><span class="status-pill" :class="progressionAdvisory.className">{{ progressionAdvisory.label }}</span></strong>
          <small>{{ progressionAdvisory.detail }}</small>
        </div>
      </div>

      <div class="metric-tabs" aria-label="그래프 지표 선택">
        <button
          v-for="(metric, key) in metrics"
          :key="key"
          type="button"
          :class="[{ active: selectedMetric === key }, metricComparisons[key]?.status]"
          @click="selectedMetric = key"
        >
          <span class="metric-label">
            <span>{{ metric.label }}</span>
            <small>{{ metricComparisons[key]?.text }}</small>
          </span>
          <strong>{{ formatValue(currentRecord?.[key], key) }}</strong>
        </button>
      </div>

      <div class="chart-view-options">
        <div class="axis-mode" aria-label="Y축 표시 방식 선택">
          <span>Y축 표시</span>
          <button
            type="button"
            :class="{ active: axisMode === 'operational' }"
            @click="axisMode = 'operational'"
          >
            운영 범위
          </button>
          <button
            type="button"
            :class="{ active: axisMode === 'zoom' }"
            @click="axisMode = 'zoom'"
          >
            확대 보기
          </button>
        </div>
        <div class="axis-guide">
          <span v-if="axisMode === 'operational'">
            표시 범위
            {{ formatValue(metrics[selectedMetric].range[0], selectedMetric) }} ~
            {{ formatValue(metrics[selectedMetric].range[1], selectedMetric) }}
          </span>
          <span v-else>현재 표시 데이터의 최솟값·최댓값에 맞춤</span>
          <template v-if="axisMode === 'operational' && selectedMetricLimit">
            <span class="guide normal">정상</span>
            <span class="guide danger">장비 한계 이탈</span>
          </template>
          <small v-if="selectedMetricLimit">
            {{ selectedEquipment }} 정상 한계
            {{ selectedMetricLimit.direction === 'high' ? '≤' : '≥' }}
            {{ formatValue(selectedMetricLimit.limit, selectedMetric) }}
          </small>
          <small v-else>상태 판정에서 제외하는 참고 지표</small>
        </div>
      </div>

      <div ref="chartWrap" class="chart-wrap">
        <canvas ref="canvas" aria-label="선택한 관찰 시점까지의 최근 14일 안테나 상태 그래프"></canvas>
      </div>

      <div class="timeline-controls">
        <button type="button" class="step-button" aria-label="6시간 이전" @click="moveTime(-6)">−6시간</button>
        <button type="button" class="play-button" :class="{ playing: isPlaying }" @click="startPlayback">
          {{ isPlaying ? '일시정지' : '재생' }}
        </button>
        <button type="button" class="step-button" aria-label="6시간 이후" @click="moveTime(6)">+6시간</button>

        <input
          v-model.number="observationTime"
          class="timeline-slider"
          type="range"
          :min="activeMinTime"
          :max="activeMaxTime"
          :step="HOUR_MS"
          aria-label="관찰 시점 이동"
          @pointerdown="stopPlayback"
        />

        <label class="speed-control">
          <span>재생 속도</span>
          <select v-model.number="playbackHours">
            <option :value="1">1시간씩</option>
            <option :value="6">6시간씩</option>
            <option :value="24">1일씩</option>
          </select>
        </label>
      </div>

      <p class="playback-note">
        공식 `고장 예상`은 장비별 센서 5개 이상이 6시간 지속될 때 확정합니다. Random Forest는 전조 진행 가능성을 보조하며 센서 경보를 취소하지 않습니다. 카카오 연동은 시연용 개인 전송 시험 상태로 유지합니다.
      </p>
    </template>
  </section>
</template>

<style scoped>
.playback-panel {
  overflow: hidden;
}

.playback-heading {
  flex-wrap: wrap;
}

.message-state {
  display: grid;
  min-height: 260px;
  place-items: center;
  color: var(--ink-500);
  background: #f5f8fb;
  border: 1px dashed var(--line);
}

.message-state.error {
  color: var(--red-500);
}

.playback-toolbar {
  display: grid;
  grid-template-columns: 150px minmax(220px, 280px) minmax(300px, 1fr);
  gap: 10px;
  padding: 10px;
  background: #edf3f8;
  border: 1px solid #c4d0dc;
}

.playback-toolbar label,
.maintenance-range,
.speed-control {
  display: grid;
  gap: 4px;
}

.playback-toolbar label > span,
.maintenance-range > span,
.speed-control > span {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.playback-toolbar select,
.playback-toolbar input,
.speed-control select {
  min-height: 34px;
  padding: 5px 8px;
  background: #ffffff;
  border: 1px solid #9faebd;
}

.maintenance-range-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.maintenance-play-button,
.metric-tabs button,
.step-button {
  min-height: 34px;
  color: #29445f;
  background: #ffffff;
  border: 1px solid #a9b8c7;
  font-weight: 700;
}

.maintenance-play-button {
  padding: 5px 10px;
}

.maintenance-range-row small,
.no-maintenance {
  color: var(--ink-500);
  font-size: 11px;
}

.alert-badge {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 3px 7px;
  border: 1px solid;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}

.alert-badge.available {
  color: #7a5400;
  background: #fff7d6;
  border-color: #e2c35d;
}

.alert-badge.unavailable {
  color: #a23a35;
  background: #fff0ef;
  border-color: #dfaaa6;
}

.no-maintenance {
  display: flex;
  min-height: 34px;
  align-items: center;
  padding: 5px 8px;
  background: #f7f9fb;
  border: 1px dashed #a9b8c7;
}

.playback-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 10px;
  border: 1px solid #c4d0dc;
}

.playback-summary > div {
  min-height: 64px;
  padding: 9px 10px;
  background: #ffffff;
  border-right: 1px solid #d4dde6;
}

.playback-summary > div:last-child {
  border-right: 0;
}

.playback-summary span:not(.status-pill) {
  display: block;
  color: var(--ink-500);
  font-size: 11px;
}

.playback-summary strong {
  display: block;
  margin-top: 5px;
  color: #20364f;
  font-size: 16px;
}

.playback-summary small {
  color: var(--ink-500);
  font-size: 10px;
}

.metric-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin-top: 10px;
}

.metric-tabs button {
  display: flex;
  min-width: 0;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 9px;
}

.metric-tabs button.active {
  color: #ffffff;
  background: #28496d;
  border-color: #28496d;
}

.metric-tabs button.caution:not(.active) {
  background: #fff8e8;
  border-color: #d8b45d;
}

.metric-tabs button.danger:not(.active) {
  background: #fff0ef;
  border-color: #d98e89;
}

.metric-label {
  display: grid;
  min-width: 0;
  gap: 2px;
  text-align: left;
}

.metric-label > span {
  font-weight: 700;
}

.metric-label small {
  overflow: hidden;
  color: var(--ink-500);
  font-size: 9px;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-tabs button.active .metric-label small {
  color: #dce8f4;
}

.metric-tabs strong {
  flex: 0 0 auto;
  white-space: nowrap;
}

.chart-view-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
  padding: 7px 8px;
  background: #f5f8fb;
  border: 1px solid #c4d0dc;
}

.axis-mode,
.axis-guide {
  display: flex;
  align-items: center;
  gap: 6px;
}

.axis-mode > span {
  margin-right: 2px;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.axis-mode button {
  min-height: 30px;
  padding: 4px 10px;
  color: #29445f;
  background: #ffffff;
  border: 1px solid #a9b8c7;
  font-weight: 700;
}

.axis-mode button.active {
  color: #ffffff;
  background: #28496d;
  border-color: #28496d;
}

.axis-guide {
  flex-wrap: wrap;
  justify-content: flex-end;
  color: var(--ink-500);
  font-size: 11px;
}

.axis-guide small {
  color: #7b8794;
}

.guide {
  display: inline-flex;
  min-height: 20px;
  align-items: center;
  padding: 2px 6px;
  border: 1px solid;
}

.guide.normal {
  color: #2f7d57;
  background: #f1f8f4;
  border-color: #b7d7c4;
}

.guide.caution {
  color: #8a5a00;
  background: #fff8e8;
  border-color: #ecd59b;
}

.guide.danger {
  color: #a23a35;
  background: #fff0ef;
  border-color: #efc1be;
}

.chart-wrap {
  width: 100%;
  margin-top: 8px;
  overflow-x: auto;
  border: 1px solid #c4d0dc;
}

.chart-wrap canvas {
  display: block;
}

.timeline-controls {
  display: grid;
  grid-template-columns: auto auto auto minmax(180px, 1fr) 120px;
  align-items: end;
  gap: 6px;
  margin-top: 8px;
}

.play-button {
  min-width: 84px;
  min-height: 36px;
  color: #ffffff;
  background: #23639c;
  border: 1px solid #174c79;
  font-weight: 700;
}

.play-button.playing {
  background: #b47c1c;
  border-color: #8a5a00;
}

.timeline-slider {
  width: 100%;
  min-height: 36px;
  accent-color: #23639c;
}

.playback-note {
  margin: 8px 0 0;
  color: var(--ink-500);
  font-size: 11px;
}

@media (max-width: 900px) {
  .playback-toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .maintenance-range {
    grid-column: 1 / -1;
  }

  .playback-summary,
  .metric-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .timeline-controls {
    grid-template-columns: repeat(3, auto) minmax(120px, 1fr);
  }

  .speed-control {
    grid-column: 1 / -1;
  }

  .chart-view-options {
    align-items: stretch;
    flex-direction: column;
  }

  .axis-guide {
    justify-content: flex-start;
  }
}

@media (max-width: 600px) {
  .playback-toolbar,
  .playback-summary,
  .metric-tabs {
    grid-template-columns: 1fr;
  }

  .maintenance-range {
    grid-column: auto;
  }

  .maintenance-range-row {
    flex-wrap: wrap;
  }

  .playback-summary > div {
    border-right: 0;
    border-bottom: 1px solid #d4dde6;
  }

  .timeline-controls {
    grid-template-columns: repeat(3, 1fr);
  }

  .timeline-slider,
  .speed-control {
    grid-column: 1 / -1;
  }
}
</style>
