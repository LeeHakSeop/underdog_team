<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { yardMapLayout } from '@/config/yardMapLayout'
import { useYardMapStore } from '@/stores/adminStore/yardMapStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useWeatherStore } from '@/stores/weatherStore'
import WeatherCard from '@/components/WeatherCard.vue'

const mapElement = ref(null)
const mapReady = ref(false)
const mapError = ref('')
const selectedSectorId = ref(null)
const selectedWorkOrderId = ref(null)
const searchQuery = ref('')
const statusFilter = ref('ALL')
const showAllPrioritySectors = ref(false)
const showNormalGates = ref(false)
const capacityInput = ref('')
const capacitySaving = ref(false)
const capacityMessage = ref('')
let map
let operationLayer
let refreshTimer
let sectorLayers = new Map()

const yardMapStore = useYardMapStore()
const notificationStore = useNotificationStore()
const weatherStore = useWeatherStore()
const {
  blockSummary,
  containerCountBySectorId,
  error: storeError,
  failedAt,
  failureCount,
  gateSummary,
  lastUpdatedAt,
  loading,
  stale,
  vehicleCountBySectorId,
  vehicles,
  yardSectors,
} = storeToRefs(yardMapStore)
const { weatherInfo, loading: weatherLoading, errMsg: weatherError } = storeToRefs(weatherStore)
const { notifications } = storeToRefs(notificationStore)

const statusOptions = [
  { value: 'ALL', label: '전체' },
  { value: 'NORMAL', label: '정상' },
  { value: 'WARNING', label: '주의' },
  { value: 'DANGER', label: '위험' },
]

const error = computed(() => mapError.value || storeError.value)
const normalizedSearch = computed(() => searchQuery.value.trim().toLowerCase())
const selectedSector = computed(() => yardSectors.value.find((sector) => sector.sectorId === selectedSectorId.value) || null)
const selectedSectorVehicles = computed(() => vehicles.value.filter((vehicle) => vehicle.sectorId === selectedSectorId.value))
const selectedWorkVehicle = computed(() => {
  const selectedFromId = vehicles.value.find((vehicle) => vehicle.workOrderId === selectedWorkOrderId.value)
  return selectedFromId || selectedSectorVehicles.value[0] || null
})

watch(selectedSector, (sector) => {
  capacityInput.value = sector?.capacity ?? ''
  capacityMessage.value = ''
})

async function saveSectorCapacity() {
  const capacity = Number(capacityInput.value)

  if (!selectedSector.value) return
  if (!Number.isInteger(capacity) || capacity < 1 || capacity > 10000) {
    capacityMessage.value = '1~10000 사이 정수를 입력하세요.'
    return
  }

  capacitySaving.value = true
  capacityMessage.value = ''

  try {
    await yardMapStore.updateSectorCapacity(selectedSector.value.sectorId, capacity)
    capacityInput.value = capacity
    capacityMessage.value = '저장됨'
  } catch (saveError) {
    capacityMessage.value = saveError.message || '수용량을 저장하지 못했습니다.'
  } finally {
    capacitySaving.value = false
  }
}

const getNotificationValue = (item, ...keys) => {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

const openDispatchExceptions = computed(() => (notifications.value || []).filter((item) => {
  const status = getNotificationValue(item, 'processStatus', 'process_status') || 'UNPROCESSED'
  return status !== 'PROCESSED'
}))

const selectedSectorExceptions = computed(() => {
  const plates = new Set(selectedSectorVehicles.value.flatMap((vehicle) => [
    vehicle.tractorPlateNumber,
    vehicle.trailerPlateNumber,
  ]).filter(Boolean).map((value) => String(value).trim()))

  return openDispatchExceptions.value.filter((item) => {
    const plate = String(getNotificationValue(item, 'plateNumber', 'plate_number') || '').trim()
    return plate && plates.has(plate)
  })
})

const selectedSectorExceptionSummary = computed(() => {
  if (!selectedSector.value) {
    return { label: '선택 없음', tone: 'normal', message: '섹터를 선택하면 관련 차량 예외를 함께 확인할 수 있습니다.' }
  }

  if (selectedSectorExceptions.value.length === 0) {
    return { label: '예외 없음', tone: 'normal', message: '선택 섹터 작업 차량 기준으로 미처리 배차 예외가 없습니다.' }
  }

  const first = selectedSectorExceptions.value[0]
  const type = getNotificationValue(first, 'exceptionType', 'exception_type') || 'EXCEPTION'
  return {
    label: `예외 ${selectedSectorExceptions.value.length}건`,
    tone: ['VEHICLE_NOT_REGISTERED', 'CARRIER_INACTIVE', 'YARD_SECTOR_CAPACITY_EXCEEDED'].includes(type) ? 'danger' : 'warning',
    message: getNotificationValue(first, 'exceptionMessage', 'exception_message') || '선택 섹터 차량의 배차 예외를 확인하세요.',
  }
})

const statusCounts = computed(() => yardSectors.value.reduce((counts, sector) => {
  const level = sector.statusLevel || 'NORMAL'
  counts[level] = (counts[level] || 0) + 1
  return counts
}, { NORMAL: 0, WARNING: 0, DANGER: 0 }))

const selectedSectorMetrics = computed(() => {
  const sector = selectedSector.value

  return [
    { label: '컨테이너', value: formatCount(sector?.containerCount) },
    { label: '작업 차량', value: String(selectedSectorVehicles.value.length) + '대' },
    { label: '진행 작업', value: formatCount(sector?.workOrderCount) },
    { label: '사용률', value: formatPercent(sector?.usageRate) },
  ]
})

const matchesSectorFilter = (sector) => {
  const matchesStatus = statusFilter.value === 'ALL' || (sector.statusLevel || 'NORMAL') === statusFilter.value
  if (!matchesStatus) return false
  if (!normalizedSearch.value) return true
  const sectorVehicles = vehicles.value.filter((vehicle) => vehicle.sectorId === sector.sectorId)

  return [
    sector.sectorName,
    sector.blockName,
    sector.statusLevel,
    sector.guideMessage,
    ...sectorVehicles.flatMap((vehicle) => [
      vehicle.workOrderId,
      vehicle.workStatus,
      vehicle.containerNumber,
      vehicle.tractorPlateNumber,
      vehicle.trailerPlateNumber,
      vehicle.driverName,
      vehicle.carrierName,
      vehicle.originLocation,
      vehicle.destinationSectorName,
      vehicle.routeSummary,
    ]),
  ].some((value) => String(value || '').toLowerCase().includes(normalizedSearch.value))
}

const visibleSectors = computed(() => yardSectors.value.filter(matchesSectorFilter))
const prioritySectors = computed(() => [...yardSectors.value]
  .filter((sector) => ['DANGER', 'WARNING'].includes(sector.statusLevel))
  .sort((left, right) => {
    const rank = { DANGER: 0, WARNING: 1, NORMAL: 2 }
    return (rank[left.statusLevel] ?? 2) - (rank[right.statusLevel] ?? 2)
      || Number(right.usageRate || 0) - Number(left.usageRate || 0)
      || Number(right.waitingVehicleCount || 0) - Number(left.waitingVehicleCount || 0)
  })
)
const visiblePrioritySectors = computed(() => (showAllPrioritySectors.value ? prioritySectors.value : prioritySectors.value.slice(0, 3)))
const hiddenPriorityCount = computed(() => Math.max(prioritySectors.value.length - visiblePrioritySectors.value.length, 0))
const sortedGates = computed(() => [...gateSummary.value].sort((left, right) => {
  const rank = { warning: 0, normal: 1 }
  return rank[gateTone(left)] - rank[gateTone(right)] || String(left.gateNumber).localeCompare(String(right.gateNumber))
}))
const warningGates = computed(() => sortedGates.value.filter((gate) => gateTone(gate) !== 'normal'))
const normalGates = computed(() => sortedGates.value.filter((gate) => gateTone(gate) === 'normal'))
const visibleGates = computed(() => {
  if (showNormalGates.value || warningGates.value.length === 0) return sortedGates.value
  return warningGates.value
})
const weatherRiskTone = computed(() => {
  if (weatherInfo.value?.riskLevel === 'DANGER') return 'danger'
  if (weatherInfo.value?.riskLevel === 'CAUTION') return 'warning'
  if (weatherInfo.value?.riskLevel === 'NORMAL') return 'normal'
  return 'info'
})
const yardRiskTone = computed(() => {
  if ((statusCounts.value.DANGER || 0) > 0) return 'danger'
  if ((statusCounts.value.WARNING || 0) > 0) return 'warning'
  return 'normal'
})
const gateRiskTone = computed(() => gateSummary.value.some((gate) => gateTone(gate) !== 'normal') ? 'warning' : 'normal')
const weatherReasonSummary = computed(() => {
  if (weatherError.value || !weatherInfo.value?.available) return '기상 데이터 확인 필요'

  const reasons = []
  if (Number(weatherInfo.value.windSpeed || 0) >= 10) reasons.push('강풍')
  if (Number(weatherInfo.value.rainfall || 0) >= 5) reasons.push('강수')
  if (Number(weatherInfo.value.visibility || 0) <= 5000) reasons.push('저시정')
  return reasons.join(' / ') || '영향 없음'
})

const gateIssueSummary = computed(() => {
  if (warningGates.value.length === 0) return '특이 게이트 없음'
  return warningGates.value.map((gate) => gate.gateNumber).join(', ')
})

const operationRisk = computed(() => {
  const dangerSectors = statusCounts.value.DANGER || 0
  const warningSectors = statusCounts.value.WARNING || 0
  const weatherRisk = weatherInfo.value?.riskLevel

  if (weatherError.value || !weatherInfo.value?.available) {
    return {
      tone: 'warning',
      label: '확인 필요',
      cause: '기상 데이터 미수신',
      action: '현장 공지와 야드/게이트 상태를 우선 기준으로 판단하세요.',
      message: '기상 정보가 없으므로 운영 맵 수치와 현장 연락을 함께 확인해야 합니다.',
    }
  }

  if (weatherRisk === 'DANGER' || dangerSectors > 0) {
    return {
      tone: 'danger',
      label: '운영 위험',
      cause: dangerSectors > 0 ? ('위험 섹터 ' + dangerSectors + '개') : `기상 위험 (${weatherReasonSummary.value})`,
      action: dangerSectors > 0 ? '위험 섹터와 대기 차량, 게이트 병목을 우선 확인하세요.' : '현장 통제 여부와 기사 대기 지시를 우선 확인하세요.',
      message: weatherRisk === 'DANGER'
        ? '기상으로 인한 작업 제한 가능성이 있습니다.'
        : '기상보다는 야드 혼잡이 현재 주 위험 요인입니다.',
    }
  }

  if (weatherRisk === 'CAUTION' || warningSectors > 0) {
    return {
      tone: 'warning',
      label: '주의 운영',
      cause: warningSectors > 0 ? ('주의 섹터 ' + warningSectors + '개') : `기상 주의 (${weatherReasonSummary.value})`,
      action: '주의 섹터, 입출차 게이트, 배차 대기 흐름을 함께 확인하세요.',
      message: weatherRisk === 'CAUTION'
        ? '기상 주의가 있어 운영 여유 시간을 두고 확인하세요.'
        : '현재 기상은 제한 요인이 아니며, 야드 흐름 점검이 우선입니다.',
    }
  }

  return {
    tone: 'normal',
    label: '정상 운영',
    cause: '특이 원인 없음',
    action: '현재 운영 흐름을 유지하세요.',
    message: '야드와 기상 상태가 모두 정상 범위입니다.',
  }
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function formatCount(value) {
  return `${value || 0}건`
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function usageWidth(value) {
  return `${Math.min(Number(value || 0), 100)}%`
}

function formatDirection(direction) {
  if (direction === 'IN') return '입차'
  if (direction === 'OUT') return '출차'
  return '-'
}

function statusLabel(statusLevel) {
  if (statusLevel === 'DANGER') return '위험'
  if (statusLevel === 'WARNING') return '주의'
  return '정상'
}

function environmentTypeLabel(environmentType) {
  return {
    GENERAL: '일반',
    HEAVY: '중량',
    REEFER: '냉동·냉장',
    DANGEROUS: '위험물',
    EMPTY: '공컨테이너',
  }[environmentType] || '환경 미지정'
}

function workStatusLabel(status) {
  return {
    DISPATCH_WAITING: '배차 대기',
    APPROVED: '승인 완료',
    GATE_IN: '입차 완료',
    IN_PROGRESS: '작업 중',
  }[status] || status || '-'
}

function vehicleStatusLabel(status) {
  return {
    AVAILABLE: '가용',
    ASSIGNED: '배정',
    IN_OPERATION: '운행 중',
    MAINTENANCE: '정비',
    INACTIVE: '비활성',
  }[status] || status || '-'
}

function canExitLabel(value) {
  if (value === true) return '출차 가능'
  if (value === false) return '출차 보류'
  return '-'
}

function escapeHtml(value) {
  return String(value ?? '-')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function statusClass(statusLevel) {
  if (statusLevel === 'DANGER') return 'danger'
  if (statusLevel === 'WARNING') return 'warning'
  return 'normal'
}

function gateTone(gate) {
  const result = String(gate?.latestProcessResult || '').toUpperCase()
  if (result.includes('FAIL') || result.includes('ERROR') || gate?.managerCheck === false) return 'warning'
  return 'normal'
}

function getSectorCenterById(sectorId) {
  for (const block of yardMapLayout.sectorBlocks) {
    const sectors = getBlockSectors(block.sectorName).slice(0, 20)
    const index = sectors.findIndex((sector) => sector.sectorId === sectorId)
    if (index >= 0) {
      return getCenter(getSectorCellBounds(block, Math.floor(index / 4), index % 4))
    }
  }
  return null
}

function selectSector(sectorId, focusMap = true) {
  selectedSectorId.value = sectorId
  selectedWorkOrderId.value = null

  if (!focusMap || !map) return
  const layer = sectorLayers.get(sectorId)
  if (layer?.getBounds) {
    map.fitBounds(layer.getBounds(), { padding: [80, 80], maxZoom: 17 })
    return
  }
  const center = getSectorCenterById(sectorId)
  if (center) map.panTo(center, { animate: true, duration: 0.35 })
}

function highlightSector(sectorId) {
  sectorLayers.forEach((layer, id) => {
    const sector = yardSectors.value.find((item) => item.sectorId === id)
    const style = getSectorStyle(sector, !sector || !matchesSectorFilter(sector))
    layer.setStyle({
      color: selectedSectorId.value === id ? '#174d7d' : style.color,
      weight: selectedSectorId.value === id ? 3 : 1,
      fillColor: style.fillColor,
      fillOpacity: style.fillOpacity,
    })
  })

  if (!sectorId || !sectorLayers.has(sectorId)) return
  const layer = sectorLayers.get(sectorId)
  const sector = yardSectors.value.find((item) => item.sectorId === sectorId)
  const level = sector?.statusLevel || 'NORMAL'
  layer.setStyle({
    color: level === 'DANGER' ? '#8f1f1b' : level === 'WARNING' ? '#8d5b08' : '#174d7d',
    weight: 4,
    fillOpacity: Math.min((getSectorStyle(sector).fillOpacity || 0.7) + 0.08, 0.9),
  })
  layer.bringToFront()
}

function sectorAlertBadges(sector) {
  if (!sector) return []
  const badges = []
  if ((sector.statusLevel || 'NORMAL') === 'DANGER') badges.push({ type: 'danger', label: '위험' })
  if ((sector.statusLevel || 'NORMAL') === 'WARNING') badges.push({ type: 'warning', label: '주의' })
  if (Number(sector.usageRate || 0) >= 80) badges.push({ type: 'danger', label: '사용률 80%↑' })
  else if (Number(sector.usageRate || 0) >= 50) badges.push({ type: 'warning', label: '사용률 50%↑' })
  if (Number(sector.waitingVehicleCount || 0) >= 6) badges.push({ type: 'danger', label: '대기 차량 많음' })
  else if (Number(sector.waitingVehicleCount || 0) >= 3) badges.push({ type: 'warning', label: '대기 차량 증가' })
  if (Number(sector.workOrderCount || 0) >= 3) badges.push({ type: 'danger', label: '작업 집중' })
  else if (Number(sector.workOrderCount || 0) >= 1) badges.push({ type: 'info', label: '작업 진행' })
  return badges
}

function vehicleAlertBadges(vehicle) {
  if (!vehicle) return []
  const badges = []
  if (vehicle.canExit === false) badges.push({ type: 'danger', label: '출차 보류' })
  if (vehicle.tractorVehicleStatus === 'MAINTENANCE') badges.push({ type: 'warning', label: '트랙터 정비' })
  if (vehicle.trailerVehicleStatus === 'MAINTENANCE') badges.push({ type: 'warning', label: '트레일러 정비' })
  if (vehicle.workStatus === 'DISPATCH_WAITING') badges.push({ type: 'info', label: '배차 대기' })
  if (vehicle.workStatus === 'IN_PROGRESS') badges.push({ type: 'info', label: '이동 중' })
  return badges
}

function getSectorStyle(sector, muted = false) {
  const level = sector?.statusLevel || 'NORMAL'
  if (muted) return { color: '#9aa7b5', fillColor: '#d8dee6', fillOpacity: 0.34 }
  if (level === 'DANGER') return { color: '#a63733', fillColor: '#e56f67', fillOpacity: 0.82 }
  if (level === 'WARNING') return { color: '#a66b0e', fillColor: '#f0c75e', fillOpacity: 0.78 }
  return { color: '#2d7a55', fillColor: '#87d0ae', fillOpacity: 0.76 }
}

const gatePopupHtml = (gate) => `
  <div class="gate-popup">
    <b>${escapeHtml(gate.gateName || gate.gateNumber)}</b>
    <div><span>게이트 번호</span><strong>${escapeHtml(gate.gateNumber)}</strong></div>
    <div><span>구분</span><strong>${escapeHtml(formatDirection(gate.direction))}</strong></div>
    <div><span>최근 처리</span><strong>${escapeHtml(gate.latestProcessResult)}</strong></div>
    <div><span>최근 차량</span><strong>${escapeHtml(gate.latestVehicleId)}</strong></div>
    <div><span>최근 시간</span><strong>${escapeHtml(formatDateTime(gate.latestExitTime || gate.latestEntryTime))}</strong></div>
    <div><span>오늘 입차</span><strong>${escapeHtml(formatCount(gate.todayInCount))}</strong></div>
    <div><span>오늘 출차</span><strong>${escapeHtml(formatCount(gate.todayOutCount))}</strong></div>
  </div>
`

const sectorPopupHtml = (sector) => `
  <div class="sector-popup">
    <b>${escapeHtml(sector.sectorName)}</b>
    <div><span>작업환경</span><strong>${escapeHtml(environmentTypeLabel(sector.environmentType))}</strong></div>
    <div><span>블록</span><strong>${escapeHtml(sector.blockName)}</strong></div>
    <div><span>컨테이너</span><strong>${escapeHtml(formatCount(sector.containerCount))}</strong></div>
    <div><span>수용량</span><strong>${escapeHtml(formatCount(sector.capacity))}</strong></div>
    <div><span>사용률</span><strong>${escapeHtml(formatPercent(sector.usageRate))}</strong></div>
    <div><span>작업 차량</span><strong>${escapeHtml(formatCount(vehicleCountBySectorId.value.get(sector.sectorId)))}</strong></div>
    <div><span>진행 작업</span><strong>${escapeHtml(formatCount(sector.workOrderCount))}</strong></div>
    <div><span>상태</span><strong>${escapeHtml(statusLabel(sector.statusLevel))}</strong></div>
  </div>
`

const getBlockBounds = ({ center, widthMeters, heightMeters }) => {
  const [latitude, longitude] = center
  const latitudeOffset = (heightMeters / 2) / 111_320
  const longitudeOffset = (widthMeters / 2) / (111_320 * Math.cos((latitude * Math.PI) / 180))

  return [
    [latitude - latitudeOffset, longitude - longitudeOffset],
    [latitude + latitudeOffset, longitude + longitudeOffset],
  ]
}

const getCenter = ([[minLatitude, minLongitude], [maxLatitude, maxLongitude]]) => [
  (minLatitude + maxLatitude) / 2,
  (minLongitude + maxLongitude) / 2,
]

const getBlockSectors = (blockName) => yardSectors.value
  .filter((sector) => sector.blockName === blockName)
  .sort((left, right) => left.sectorName.localeCompare(right.sectorName))

const getSectorCellBounds = (block, row, column) => {
  const [[minLatitude, minLongitude], [maxLatitude, maxLongitude]] = getBlockBounds(block)
  const columns = 4
  const rows = 5
  const inset = 0.08
  const roadWidth = 0.08
  const gap = 0.012
  const cellWidth = (1 - (inset * 2) - roadWidth - (gap * (columns - 1))) / columns
  const cellHeight = (1 - (inset * 2) - roadWidth - (gap * (rows - 1))) / rows
  const longitudeStart = inset + (column * (cellWidth + gap)) + (column >= 2 ? roadWidth : 0)
  const latitudeStart = inset + (row * (cellHeight + gap)) + (row >= 2 ? roadWidth : 0)
  const latitudeSpan = maxLatitude - minLatitude
  const longitudeSpan = maxLongitude - minLongitude

  return [
    [minLatitude + (latitudeStart * latitudeSpan), minLongitude + (longitudeStart * longitudeSpan)],
    [minLatitude + ((latitudeStart + cellHeight) * latitudeSpan), minLongitude + ((longitudeStart + cellWidth) * longitudeSpan)],
  ]
}

const getRoadLines = (block) => {
  const [[minLatitude, minLongitude], [maxLatitude, maxLongitude]] = getBlockBounds(block)
  const inset = 0.08
  const roadWidth = 0.08
  const latitudeSpan = maxLatitude - minLatitude
  const longitudeSpan = maxLongitude - minLongitude
  const verticalRoadLongitude = minLongitude + ((inset + (((1 - (inset * 2) - roadWidth) / 2)) + (roadWidth / 2)) * longitudeSpan)
  const horizontalRoadLatitude = minLatitude + ((inset + ((((1 - (inset * 2) - roadWidth) / 5) * 2)) + (roadWidth / 2)) * latitudeSpan)

  return [
    [[minLatitude, verticalRoadLongitude], [maxLatitude, verticalRoadLongitude]],
    [[horizontalRoadLatitude, minLongitude], [horizontalRoadLatitude, maxLongitude]],
  ]
}

const vehiclePopupHtml = (sectorVehicles) => {
  const rows = sectorVehicles.slice(0, 6).map((vehicle) => `
    <div>
      <span>${escapeHtml(vehicle.tractorPlateNumber || vehicle.vehicleId || vehicle.workOrderId)}</span>
      <strong>
        ${escapeHtml(workStatusLabel(vehicle.workStatus))} /
        ${escapeHtml(vehicle.routeSummary || '-')} /
        T ${escapeHtml(vehicleStatusLabel(vehicle.tractorVehicleStatus))} /
        TR ${escapeHtml(vehicleStatusLabel(vehicle.trailerVehicleStatus))}
      </strong>
    </div>
  `).join('')

  return `<div class="vehicle-popup"><b>작업 차량 ${sectorVehicles.length}대</b>${rows}</div>`
}

const renderOperations = () => {
  if (!mapReady.value) return
  operationLayer?.clearLayers()
  sectorLayers = new Map()

  const sectorCenters = new Map()
  const vehiclesBySector = vehicles.value.reduce((groups, vehicle) => {
    if (!vehicle.sectorId) return groups
    groups.set(vehicle.sectorId, [...(groups.get(vehicle.sectorId) || []), vehicle])
    return groups
  }, new Map())

  yardMapLayout.sectorBlocks.forEach((block) => {
    const summary = blockSummary.value.find((item) => item.sectorName === block.sectorName)
    const style = getSectorStyle(summary)

    operationLayer.addLayer(L.rectangle(getBlockBounds(block), {
      color: '#6d7782',
      weight: 2,
      fillColor: style.fillColor,
      fillOpacity: 0.12,
    }).bindPopup(`<b>${block.label}</b><br>컨테이너 ${summary?.containerCount || 0}건<br>작업 차량 ${summary?.vehicleCount || 0}대`))

    getRoadLines(block).forEach((roadLine) => {
      operationLayer.addLayer(L.polyline(roadLine, { color: '#f8fafc', weight: 5, opacity: 0.95, dashArray: '8 5' }))
    })

    getBlockSectors(block.sectorName).slice(0, 20).forEach((yardSector, index) => {
      const isVisible = matchesSectorFilter(yardSector)
      const cellBounds = getSectorCellBounds(block, Math.floor(index / 4), index % 4)
      const sectorCenter = getCenter(cellBounds)
      sectorCenters.set(yardSector.sectorId, sectorCenter)

      const sectorStyle = getSectorStyle(yardSector, !isVisible)
      const cell = L.rectangle(cellBounds, {
        color: selectedSectorId.value === yardSector.sectorId ? '#174d7d' : sectorStyle.color,
        weight: selectedSectorId.value === yardSector.sectorId ? 3 : 1,
        fillColor: sectorStyle.fillColor,
        fillOpacity: sectorStyle.fillOpacity,
      })
        .bindTooltip(`${yardSector.sectorName} / ${environmentTypeLabel(yardSector.environmentType)} / ${statusLabel(yardSector.statusLevel)} / 사용률 ${formatPercent(yardSector.usageRate)}`, { sticky: true })
        .bindPopup(sectorPopupHtml(yardSector))
        .on('click', () => {
          selectSector(yardSector.sectorId, false)
        })
        .on('mouseover', () => {
          highlightSector(yardSector.sectorId)
        })
        .on('mouseout', () => {
          highlightSector(null)
        })
      sectorLayers.set(yardSector.sectorId, cell)
      operationLayer.addLayer(cell)
    })

    operationLayer.addLayer(L.marker(block.center, {
      interactive: false,
      icon: L.divIcon({ className: 'yard-zone-label', html: `<strong>${block.label}</strong>`, iconSize: [70, 24], iconAnchor: [35, 12] }),
    }))
  })

  const selectedRouteStart = sectorCenters.get(selectedWorkVehicle.value?.startSectorId)
  const selectedRouteDestination = sectorCenters.get(selectedWorkVehicle.value?.destinationSectorId)
  if (selectedRouteStart && selectedRouteDestination) {
    operationLayer.addLayer(L.polyline([selectedRouteStart, selectedRouteDestination], {
      color: '#23639c',
      weight: 4,
      opacity: 0.9,
      dashArray: '7 6',
      interactive: false,
    }))
  }

  vehiclesBySector.forEach((sectorVehicles, sectorId) => {
    const center = sectorCenters.get(sectorId)
    if (!center) return

    operationLayer.addLayer(L.marker(center, {
      icon: L.divIcon({
        className: 'yard-vehicle-icon',
        html: `<span class="vehicle-marker"><i></i><b>${sectorVehicles.length}</b></span>`,
        iconSize: [50, 30],
        iconAnchor: [25, 15],
      }),
    }).bindPopup(vehiclePopupHtml(sectorVehicles)))
  })

  gateSummary.value.filter((gate) => gate.position).forEach((gate) => {
    const laneClass = ['G03', 'G04'].includes(String(gate.gateNumber)) ? 'lane-secondary' : 'lane-primary'
    const icon = L.divIcon({
      className: 'yard-gate-icon',
      html: `
        <span class="yard-gate ${gate.direction.toLowerCase()} ${laneClass}">
          <small>${formatDirection(gate.direction)}</small>
          <b>${gate.gateNumber}</b>
          <i>${gate.direction === 'OUT' ? '↑' : '↓'}</i>
        </span>
      `,
      iconSize: [60, 52],
      iconAnchor: [30, 26],
    })
    operationLayer.addLayer(L.marker(gate.position, { icon }).bindPopup(gatePopupHtml(gate)))
  })
}

const refreshData = async () => {
  await Promise.allSettled([
    yardMapStore.loadYardMap(),
    weatherStore.fetchWeather(),
  ])
  await nextTick()
  renderOperations()
}

const manualRefresh = async () => {
  await refreshData()
}

watch([blockSummary, gateSummary, yardSectors, vehicles, selectedSectorId, searchQuery, statusFilter], renderOperations, { deep: true })

onMounted(async () => {
  try {
    map = L.map(mapElement.value, { zoomControl: false }).setView(yardMapLayout.center, yardMapLayout.zoom)
    L.control.zoom({ position: 'bottomright' }).addTo(map)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(map)
    operationLayer = L.layerGroup().addTo(map)
    mapReady.value = true
    await refreshData()
    refreshTimer = window.setInterval(refreshData, 10000)
  } catch (loadError) {
    mapError.value = loadError.message || '지도를 준비하지 못했습니다.'
  }
})

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer)
  map?.remove()
})
</script>

<template>
  <div class="page-stack yard-map-page">
    <section class="map-layout">
      <article class="panel map-panel">
        <div v-if="error" class="map-notice">{{ error }}</div>
        <div v-if="loading" class="map-loading">운영 맵 갱신 중</div>
        <div class="map-toolbar">
          <label>
            <span>섹터 검색</span>
            <input v-model="searchQuery" type="search" placeholder="A-01, 차량번호, 기사명" />
          </label>
          <label>
            <span>상태</span>
            <select v-model="statusFilter">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <div class="toolbar-refresh" :class="{ stale }">
            <strong>{{ stale ? '이전 데이터' : '실시간' }}</strong>
            <small>갱신 {{ formatDateTime(lastUpdatedAt) }}</small>
            <small v-if="failedAt">실패 {{ formatDateTime(failedAt) }} / {{ failureCount }}회</small>
          </div>
          <RouterLink class="toolbar-button toolbar-link" to="/admin/predictive-maintenance">
            예지보전
          </RouterLink>
          <button class="toolbar-button" type="button" :disabled="loading" @click="manualRefresh">
            {{ loading ? '갱신 중' : '새로고침' }}
          </button>
        </div>
        <WeatherCard
          :weather="weatherInfo"
          :loading="weatherLoading"
          :error="weatherError"
          title="부산항 날씨"
          mode="map"
        />
        <div ref="mapElement" class="yard-map" aria-label="감만부두 운영 지도"></div>
      </article>
      <aside class="panel summary-panel">
        <section class="summary-group operation-brief" :class="operationRisk.tone">
          <div class="summary-heading">
            <strong>운영 판단</strong>
            <small>기상 위험도 + 야드 혼잡도</small>
          </div>
          <div class="summary-content">
            <div class="operation-status">
              <div class="operation-alert-title">
                <b>{{ operationRisk.tone === 'danger' ? '위험' : operationRisk.tone === 'warning' ? '주의' : '정상' }}</b>
                <span>{{ operationRisk.label }}</span>
              </div>
              <p>{{ operationRisk.message }}</p>
              <dl>
                <div><dt>원인</dt><dd>{{ operationRisk.cause }}</dd></div>
                <div><dt>조치</dt><dd>{{ operationRisk.action }}</dd></div>
                <div><dt>기상 원인</dt><dd>{{ weatherReasonSummary }}</dd></div>
                <div><dt>게이트 확인</dt><dd>{{ gateIssueSummary }}</dd></div>
              </dl>
            </div>
            <div class="operation-metrics">
              <div :class="weatherRiskTone"><span>날씨</span><strong>{{ weatherInfo?.riskLevel || '정보 없음' }}</strong></div>
              <div :class="yardRiskTone"><span>야드</span><strong>위험 {{ statusCounts.DANGER || 0 }} · 주의 {{ statusCounts.WARNING || 0 }}</strong></div>
              <div :class="gateRiskTone"><span>게이트</span><strong>{{ warningGates.length > 0 ? `확인 필요 ${warningGates.length}` : `정상 ${gateSummary.length}` }}</strong></div>
            </div>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading">
            <strong>위험/주의 섹터</strong>
            <small>{{ prioritySectors.length ? '클릭/hover 시 지도 강조' : '현재 특이 섹터 없음' }}</small>
          </div>
          <div class="summary-content priority-list">
            <button
              v-for="sector in visiblePrioritySectors"
              :key="`priority-${sector.sectorId}`"
              type="button"
              class="priority-row"
              :class="statusClass(sector.statusLevel)"
              @click="selectSector(sector.sectorId)"
              @mouseenter="highlightSector(sector.sectorId)"
              @mouseleave="highlightSector(null)"
            >
              <strong>{{ sector.sectorName }}</strong>
              <span>{{ statusLabel(sector.statusLevel) }}</span>
              <small>사용률 {{ formatPercent(sector.usageRate) }} · 대기 {{ formatCount(sector.waitingVehicleCount) }} · 작업 {{ formatCount(sector.workOrderCount) }}</small>
            </button>
            <button
              v-if="hiddenPriorityCount > 0 || showAllPrioritySectors"
              type="button"
              class="compact-more-button"
              @click="showAllPrioritySectors = !showAllPrioritySectors"
            >
              {{ showAllPrioritySectors ? '접기' : `+ ${hiddenPriorityCount}개 더 보기` }}
            </button>
            <p v-if="prioritySectors.length === 0" class="empty">정상 운영 중입니다.</p>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading">
            <strong>게이트 현황</strong>
            <small>확인 필요 항목 우선 표시</small>
          </div>
          <div class="summary-content gate-list">
            <div v-for="gate in visibleGates" :key="gate.gateNumber" class="gate-card" :class="gateTone(gate)">
              <div>
                <strong>{{ gate.gateNumber }}</strong>
                <small>{{ formatDirection(gate.direction) }} · {{ gateTone(gate) === 'normal' ? '정상' : '확인 필요' }}</small>
              </div>
              <dl>
                <div><dt>최근 처리</dt><dd>{{ gate.latestProcessResult || '-' }}</dd></div>
                <div><dt>최근 시간</dt><dd>{{ formatDateTime(gate.latestExitTime || gate.latestEntryTime) }}</dd></div>
                <div><dt>오늘 입차</dt><dd>{{ formatCount(gate.todayInCount) }}</dd></div>
                <div><dt>오늘 출차</dt><dd>{{ formatCount(gate.todayOutCount) }}</dd></div>
              </dl>
            </div>
            <button
              v-if="normalGates.length > 0 && warningGates.length > 0"
              type="button"
              class="compact-more-button"
              @click="showNormalGates = !showNormalGates"
            >
              {{ showNormalGates ? '정상 게이트 접기' : `정상 게이트 ${normalGates.length}개 보기` }}
            </button>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading">
            <strong>야드 섹터</strong>
            <small>{{ yardSectors.length }}개 섹터</small>
          </div>
          <div class="summary-content">
            <div class="sector-list">
              <button
                v-for="sector in visibleSectors"
                :key="sector.sectorId"
                class="sector-row"
                :class="[statusClass(sector.statusLevel), { selected: sector.sectorId === selectedSectorId }]"
                type="button"
                @click="selectSector(sector.sectorId)"
              >
                <strong>{{ sector.sectorName }}</strong>
                <span>{{ statusLabel(sector.statusLevel) }}</span>
                <small>
                  {{ sector.blockName }} 구역 / 차량 {{ vehicleCountBySectorId.get(sector.sectorId) || 0 }}대 / 작업 {{ sector.workOrderCount || 0 }}건
                </small>
                <div class="usage-line">
                  <i :style="{ width: usageWidth(sector.usageRate) }"></i>
                </div>
                <small>사용률 {{ formatPercent(sector.usageRate) }}</small>
              </button>
              <p v-if="visibleSectors.length === 0" class="empty">조건에 맞는 섹터가 없습니다.</p>
            </div>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading">
            <strong>선택 섹터</strong>
            <small>{{ selectedSector?.sectorName || '선택 없음' }}</small>
          </div>
          <div v-if="selectedSector" class="metric-grid">
            <div v-for="metric in selectedSectorMetrics" :key="metric.label" class="metric-card">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </div>
          </div>
          <p class="sector-exception-note" :class="selectedSectorExceptionSummary.tone">{{ selectedSectorExceptionSummary.message }}</p>
          <div v-if="selectedSector" class="badge-list">
            <span
              v-for="badge in sectorAlertBadges(selectedSector)"
              :key="`${badge.type}-${badge.label}`"
              class="status-badge"
              :class="badge.type"
            >
              {{ badge.label }}
            </span>
            <span v-if="sectorAlertBadges(selectedSector).length === 0" class="status-badge normal">특이사항 없음</span>
          </div>
          <dl class="selected-detail">
            <div><dt>블록</dt><dd>{{ selectedSector?.blockName || '-' }}</dd></div>
            <div><dt>작업환경</dt><dd>{{ environmentTypeLabel(selectedSector?.environmentType) }}</dd></div>
            <div><dt>컨테이너</dt><dd>{{ formatCount(selectedSector?.containerCount) }}</dd></div>
            <div class="capacity-detail">
              <dt>수용량</dt>
              <dd>
                <form class="capacity-control" @submit.prevent="saveSectorCapacity">
                  <input
                    v-model="capacityInput"
                    aria-label="선택 섹터 수용량"
                    inputmode="numeric"
                    min="1"
                    max="10000"
                    step="1"
                    type="number"
                  />
                  <button type="submit" :disabled="capacitySaving">
                    {{ capacitySaving ? '저장 중' : '저장' }}
                  </button>
                </form>
                <small v-if="capacityMessage" class="capacity-message">{{ capacityMessage }}</small>
              </dd>
            </div>
            <div><dt>사용률</dt><dd>{{ formatPercent(selectedSector?.usageRate) }}</dd></div>
            <div><dt>작업 차량</dt><dd>{{ selectedSectorVehicles.length }}대</dd></div>
            <div><dt>대기 차량</dt><dd>{{ formatCount(selectedSector?.waitingVehicleCount) }}</dd></div>
            <div><dt>안내</dt><dd>{{ selectedSector?.guideMessage || '-' }}</dd></div>
          </dl>
          <div class="vehicle-list">
            <div v-for="vehicle in selectedSectorVehicles" :key="vehicle.workOrderId" class="vehicle-row">
              <button
                class="vehicle-select"
                :class="{ selected: selectedWorkVehicle?.workOrderId === vehicle.workOrderId }"
                type="button"
                @click="selectedWorkOrderId = vehicle.workOrderId"
              >
                <strong>{{ vehicle.tractorPlateNumber || vehicle.vehicleId || vehicle.workOrderId }}</strong>
                <span>{{ workStatusLabel(vehicle.workStatus) }}</span>
                <small>
                  컨테이너 {{ vehicle.containerNumber || '-' }} /
                  {{ vehicle.routeSummary || '-' }} /
                  트랙터 {{ vehicleStatusLabel(vehicle.tractorVehicleStatus) }} /
                  트레일러 {{ vehicleStatusLabel(vehicle.trailerVehicleStatus) }}
                </small>
              </button>
            </div>
            <p v-if="selectedSector && selectedSectorVehicles.length === 0" class="empty">현재 표시할 작업 차량이 없습니다.</p>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading">
            <strong>작업 상세</strong>
            <small>{{ selectedWorkVehicle?.workOrderId ? `#${selectedWorkVehicle.workOrderId}` : '선택 없음' }}</small>
          </div>
          <div v-if="selectedWorkVehicle" class="badge-list">
            <span
              v-for="badge in vehicleAlertBadges(selectedWorkVehicle)"
              :key="`${badge.type}-${badge.label}`"
              class="status-badge"
              :class="badge.type"
            >
              {{ badge.label }}
            </span>
            <span v-if="vehicleAlertBadges(selectedWorkVehicle).length === 0" class="status-badge normal">특이사항 없음</span>
          </div>
          <dl class="selected-detail">
            <div><dt>작업 상태</dt><dd>{{ workStatusLabel(selectedWorkVehicle?.workStatus) }}</dd></div>
            <div><dt>작업 유형</dt><dd>{{ selectedWorkVehicle?.workType || '-' }}</dd></div>
            <div><dt>예약 시간</dt><dd>{{ formatDateTime(selectedWorkVehicle?.reservedTime) }}</dd></div>
            <div><dt>기사</dt><dd>{{ selectedWorkVehicle?.driverName || '-' }}</dd></div>
            <div><dt>운송사</dt><dd>{{ selectedWorkVehicle?.carrierName || '-' }}</dd></div>
            <div><dt>출발</dt><dd>{{ selectedWorkVehicle?.originLocation || '-' }}</dd></div>
            <div><dt>목적</dt><dd>{{ selectedWorkVehicle?.destinationSectorName || selectedWorkVehicle?.sectorName || '-' }}</dd></div>
            <div><dt>이동 구간</dt><dd>{{ selectedWorkVehicle?.routeSummary || '-' }}</dd></div>
            <div><dt>섹터</dt><dd>{{ selectedWorkVehicle?.sectorName || selectedSector?.sectorName || '-' }}</dd></div>
            <div><dt>컨테이너</dt><dd>{{ selectedWorkVehicle?.containerNumber || '-' }}</dd></div>
            <div><dt>규격/위치</dt><dd>{{ selectedWorkVehicle?.containerSize || '-' }} / {{ selectedWorkVehicle?.containerLocation || '-' }}</dd></div>
            <div><dt>출차 상태</dt><dd>{{ canExitLabel(selectedWorkVehicle?.canExit) }}</dd></div>
            <div><dt>트랙터</dt><dd>{{ selectedWorkVehicle?.tractorPlateNumber || '-' }} / {{ vehicleStatusLabel(selectedWorkVehicle?.tractorVehicleStatus) }}</dd></div>
            <div><dt>트레일러</dt><dd>{{ selectedWorkVehicle?.trailerPlateNumber || '-' }} / {{ vehicleStatusLabel(selectedWorkVehicle?.trailerVehicleStatus) }}</dd></div>
          </dl>
        </section>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.yard-map-page {
  font-family: "Malgun Gothic", "Apple SD Gothic Neo", "Segoe UI", Arial, sans-serif;
  letter-spacing: 0;
}

.map-layout {
  display: grid;
  min-height: 650px;
  height: calc(100vh - 78px);
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 10px;
}

.map-panel {
  position: relative;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.yard-map {
  width: 100%;
  height: 100%;
  background: #dce6ec;
}

.map-toolbar,
.map-notice,
.map-loading {
  position: absolute;
  z-index: 500;
}

.map-toolbar {
  display: grid;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  grid-template-columns: minmax(220px, 1fr) 130px auto auto;
  gap: 8px;
  align-items: center;
  width: min(760px, calc(100% - 120px));
  padding: 8px;
  background: #ffffff;
  border: 1px solid #b9c5d1;
  box-shadow: 0 2px 8px #1726361a;
}

.map-toolbar label {
  display: grid;
  gap: 3px;
  color: #5d6875;
  font-size: 11px;
  font-weight: 700;
}

.map-toolbar input,
.map-toolbar select {
  min-width: 118px;
  height: 30px;
  padding: 4px 7px;
  color: #1f2933;
  background: #f8fafc;
  border: 1px solid #aeb9c5;
}

.map-toolbar input {
  width: 100%;
}

.map-notice,
.map-loading {
  left: 12px;
  padding: 7px 9px;
  border: 1px solid #d5b766;
  color: #7a5300;
  background: #fff5dc;
  font-size: 12px;
  font-weight: 700;
}

.map-notice {
  top: 76px;
}

.map-loading {
  top: 112px;
}

.toolbar-refresh {
  display: grid;
  gap: 1px;
  min-width: 104px;
  padding: 5px 7px;
  color: #1f2933;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
}

.toolbar-refresh.stale {
  color: #7a5300;
  background: #fff5dc;
  border-color: #d5b766;
}

.toolbar-refresh strong {
  font-size: 12px;
  line-height: 1.1;
}

.toolbar-refresh small {
  color: #5d6875;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.15;
}

.toolbar-button {
  min-width: 74px;
  height: 30px;
  color: #ffffff;
  background: #23639c;
  border: 1px solid #23639c;
  font-size: 12px;
  font-weight: 700;
}

.toolbar-button:disabled {
  color: #5d6875;
  background: #edf1f5;
  border-color: #c6d0da;
  cursor: wait;
}

.summary-panel {
  align-self: stretch;
  padding: 0;
  overflow-y: auto;
}

.summary-group + .summary-group {
  border-top: 1px solid var(--line);
}

.summary-heading {
  display: grid;
  gap: 2px;
  padding: 9px 10px;
  background: #e8eef5;
  border-bottom: 1px solid var(--line);
}

.summary-heading strong {
  font-size: 14px;
  font-weight: 700;
}

.summary-heading small {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 600;
}

.summary-content {
  padding: 10px;
}

.operation-brief {
  border-left: 0;
  border-top: 6px solid var(--green-600);
  background: #f2fbf5;
}

.operation-brief.warning {
  border-top-color: #d28a00;
  background: #fff5dc;
}

.operation-brief.danger {
  border-top-color: #c42f2a;
  background: #fff0ef;
}

.operation-brief.warning .summary-heading {
  color: #6c4300;
  background: #ffe1a6;
  border-bottom-color: #d9a53f;
}

.operation-brief.danger .summary-heading {
  color: #ffffff;
  background: #b52d28;
  border-bottom-color: #91231f;
}

.operation-brief.danger .summary-heading small {
  color: #ffe7e5;
}

.operation-status {
  position: relative;
  display: grid;
  gap: 10px;
  padding: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 2px solid #7fc59e;
  box-shadow: 0 10px 24px rgba(23, 38, 54, 0.14);
}

.operation-status::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 8px;
  background: #2f7d57;
  content: '';
}

.operation-brief.warning .operation-status {
  border-color: #d28a00;
  background: #fffaf0;
  box-shadow: 0 10px 24px rgba(154, 99, 13, 0.2);
}

.operation-brief.warning .operation-status::before {
  background: #d28a00;
}

.operation-brief.danger .operation-status {
  border-color: #c42f2a;
  background: #fff6f5;
  box-shadow: 0 12px 28px rgba(184, 64, 58, 0.28);
}

.operation-brief.danger .operation-status::before {
  background: repeating-linear-gradient(
    135deg,
    #c42f2a 0,
    #c42f2a 8px,
    #7f1d1d 8px,
    #7f1d1d 16px
  );
}

.operation-alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 6px;
}

.operation-alert-title b {
  display: inline-grid;
  min-width: 42px;
  min-height: 30px;
  place-items: center;
  color: #ffffff;
  background: #2f7d57;
  font-size: 13px;
  font-weight: 900;
}

.operation-brief.warning .operation-alert-title b {
  color: #3d2700;
  background: #f0b429;
}

.operation-brief.danger .operation-alert-title b {
  background: #c42f2a;
}

.operation-alert-title span {
  color: #173b60;
  font-size: 22px;
  font-weight: 900;
}

.operation-brief.warning .operation-alert-title span {
  color: #7a4b00;
}

.operation-brief.danger .operation-alert-title span {
  color: #9f1f1b;
}

.operation-status p {
  margin: 0;
  padding-left: 6px;
  color: var(--ink-900);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.45;
}

.operation-status dl {
  display: grid;
  gap: 6px;
  margin: 0;
}

.operation-status dl div {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 6px;
  padding: 6px 7px;
  background: #f8fafc;
  border: 1px solid #d8e1ea;
}

.operation-status dt {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 900;
}

.operation-status dd {
  margin: 0;
  color: var(--ink-900);
  font-size: 12px;
  font-weight: 900;
  line-height: 1.35;
}

.operation-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 8px;
}

.operation-metrics div {
  display: grid;
  gap: 3px;
  padding: 7px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  border-top: 4px solid #7b8794;
}

.operation-metrics div.normal {
  border-top-color: var(--green-600);
}

.operation-metrics div.warning {
  border-top-color: var(--amber-500);
}

.operation-metrics div.danger {
  border-top-color: var(--red-500);
}

.operation-metrics span {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 900;
}

.operation-metrics strong {
  color: var(--ink-900);
  font-size: 12px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.priority-list,
.gate-list,
.sector-list,
.vehicle-list {
  display: grid;
  gap: 8px;
}

.priority-row {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  border-left: 5px solid #7b8794;
  text-align: left;
}

.priority-row.warning {
  border-left-color: var(--amber-500);
}

.priority-row.danger {
  border-left-color: var(--red-500);
}

.priority-row strong {
  color: var(--ink-900);
  font-size: 13px;
  font-weight: 900;
}

.priority-row span {
  color: var(--ink-700);
  font-size: 11px;
  font-weight: 900;
}

.priority-row small {
  grid-column: 1 / -1;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.compact-more-button {
  width: 100%;
  min-height: 30px;
  color: #173b60;
  background: #eef4fa;
  border: 1px solid #b8c8d8;
  font-size: 12px;
  font-weight: 900;
}

.gate-card,
.vehicle-row,
.vehicle-select {
  display: grid;
  gap: 8px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
}

.gate-card.normal {
  border-left: 4px solid var(--green-600);
}

.gate-card.warning {
  border-left: 4px solid var(--amber-500);
  background: #fffaf0;
}

.gate-card strong,
.vehicle-select strong {
  display: block;
  font-size: 13px;
  font-weight: 700;
}

.gate-card small,
.vehicle-select small {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 600;
}

.gate-card dl,
.selected-detail {
  display: grid;
  gap: 4px;
  margin: 0;
}

.gate-card dl div,
.selected-detail div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 6px;
}

.gate-card dt,
.selected-detail dt {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 600;
}

.gate-card dd,
.selected-detail dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  font-size: 11px;
  font-weight: 700;
}

.toolbar-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.capacity-detail {
  align-items: start;
}

.capacity-control {
  display: grid;
  grid-template-columns: minmax(72px, 110px) 54px;
  gap: 5px;
}

.capacity-control input,
.capacity-control button {
  min-width: 0;
  height: 28px;
  border: 1px solid var(--line-strong);
  border-radius: 3px;
  font: inherit;
}

.capacity-control input {
  padding: 0 7px;
  color: var(--ink-900);
  background: #ffffff;
}

.capacity-control button {
  color: #ffffff;
  background: #23639c;
  border-color: #23639c;
  cursor: pointer;
}

.capacity-control button:disabled {
  color: var(--ink-500);
  background: #edf1f5;
  border-color: var(--line);
  cursor: wait;
}

.capacity-message {
  display: block;
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 10px;
}

.legend {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.legend-item {
  display: grid;
  min-height: 50px;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 2px 6px;
  align-items: center;
  padding: 7px;
  color: #1f2933;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  font-size: 12px;
  font-weight: 700;
  text-align: left;
}

.legend-item i {
  width: 11px;
  height: 11px;
  border: 1px solid #758595;
}

.legend-item span {
  line-height: 1;
}

.legend-item b {
  grid-column: 2;
  font-size: 15px;
  line-height: 1;
}

.legend-item.normal i {
  background: #87d0ae;
}

.legend-item.warning i {
  background: #f0c75e;
}

.legend-item.danger i {
  background: #e56f67;
}

.sector-list {
  max-height: 330px;
  overflow-y: auto;
}

.sector-row {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 3px 8px;
  padding: 8px;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
}

.sector-row span {
  font-size: 11px;
  font-weight: 800;
}

.sector-row small {
  grid-column: 1 / -1;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 600;
}

.sector-row.normal {
  border-left: 5px solid #2d7a55;
}

.sector-row.warning {
  border-left: 5px solid #a66b0e;
}

.sector-row.danger {
  border-left: 5px solid #a63733;
}

.sector-row.normal span {
  color: #2d7a55;
}

.sector-row.warning span {
  color: #93600f;
}

.sector-row.danger span {
  color: #a13631;
}

.sector-row.selected {
  color: #ffffff;
  background: #23639c;
  border-color: #23639c;
}

.sector-row.selected span,
.sector-row.selected small {
  color: #dceaff;
}

.usage-line {
  grid-column: 1 / -1;
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

.sector-row.warning .usage-line i {
  background: #c7891b;
}

.sector-row.danger .usage-line i {
  background: #b8403a;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 10px 10px 0;
}

.metric-card {
  display: grid;
  gap: 3px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
}

.metric-card span {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.metric-card strong {
  font-size: 16px;
}

.sector-exception-note {
  margin: 10px 0 0;
  padding: 10px 12px;
  border: 1px solid var(--line);
  background: #f8fafc;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.sector-exception-note.warning {
  border-color: #f5d38a;
  background: #fff8e7;
}

.sector-exception-note.danger {
  border-color: #fecaca;
  background: #fff1f2;
}

.badge-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 10px 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 3px 7px;
  color: #1f2933;
  background: #eef3f8;
  border: 1px solid #c7d1dc;
  font-size: 11px;
  font-weight: 800;
}

.status-badge.normal {
  color: #235f42;
  background: #e8f5ee;
  border-color: #8ccbac;
}

.status-badge.info {
  color: #215884;
  background: #e8f2fb;
  border-color: #8eb8da;
}

.status-badge.warning {
  color: #7a5300;
  background: #fff5dc;
  border-color: #d5b766;
}

.status-badge.danger {
  color: #9b2f2b;
  background: #fdecea;
  border-color: #df928d;
}

.selected-detail,
.vehicle-list {
  padding: 10px;
}

.vehicle-select {
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto;
  text-align: left;
}

.vehicle-select.selected {
  color: #ffffff;
  background: #23639c;
  border-color: #23639c;
}

.vehicle-select.selected span,
.vehicle-select.selected small {
  color: #dceaff;
}

.vehicle-select small {
  grid-column: 1 / -1;
}

.vehicle-select span {
  color: #23639c;
  font-size: 11px;
  font-weight: 800;
}

.vehicle-row {
  padding: 0;
  background: transparent;
  border: 0;
}

.empty {
  margin: 0;
  color: var(--ink-500);
  font-size: 12px;
}

:global(.yard-gate-complex-icon) {
  background: transparent;
  border: 0;
}

:global(.yard-gate-complex) {
  display: grid;
  gap: 2px;
  width: 124px;
  min-height: 58px;
  padding: 8px 10px;
  color: #ffffff;
  border: 2px solid rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  box-shadow: 0 10px 22px rgba(23, 38, 54, 0.24);
  text-align: left;
}

:global(.yard-gate-complex.in) {
  background: linear-gradient(135deg, #1f6aa9, #1b4f81);
}

:global(.yard-gate-complex.out) {
  background: linear-gradient(135deg, #c14a43, #93322d);
}

:global(.yard-gate-complex.warning) {
  box-shadow: 0 12px 24px rgba(143, 31, 27, 0.26);
}

:global(.yard-gate-complex span) {
  font-size: 10px;
  font-weight: 700;
  opacity: 0.86;
}

:global(.yard-gate-complex strong) {
  font-size: 13px;
  line-height: 1.15;
}

:global(.yard-gate-complex small) {
  font-size: 11px;
  font-weight: 700;
  opacity: 0.92;
}
:global(.yard-gate-icon),
:global(.yard-vehicle-icon) {
  background: transparent;
  border: 0;
}

:global(.yard-gate) {
  position: relative;
  display: grid;
  width: 52px;
  min-height: 46px;
  grid-template-rows: auto auto auto;
  justify-items: center;
  gap: 2px;
  padding: 5px 4px 4px;
  color: #fff;
  background: #23639c;
  border: 2px solid #fff;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(23, 38, 54, 0.28);
  font-weight: 900;
}

:global(.yard-gate.out) {
  background: #b8403a;
}

:global(.yard-gate.lane-primary) {
  transform: translate(-12px, -8px);
}

:global(.yard-gate.lane-secondary) {
  transform: translate(18px, 12px);
}

:global(.yard-gate::before) {
  position: absolute;
  z-index: -1;
  width: 18px;
  border-top: 2px dashed rgba(39, 52, 66, 0.55);
  content: '';
}

:global(.yard-gate::after) {
  position: absolute;
  z-index: -1;
  width: 8px;
  height: 8px;
  background: #ffffff;
  border: 2px solid rgba(39, 52, 66, 0.72);
  border-radius: 50%;
  content: '';
}

:global(.yard-gate.in.lane-primary::before) {
  right: -14px;
  bottom: 8px;
  transform: rotate(18deg);
}

:global(.yard-gate.in.lane-primary::after) {
  right: -22px;
  bottom: 3px;
}

:global(.yard-gate.in.lane-secondary::before) {
  left: -14px;
  top: 9px;
  transform: rotate(-20deg);
}

:global(.yard-gate.in.lane-secondary::after) {
  left: -22px;
  top: 4px;
}

:global(.yard-gate.out.lane-primary::before) {
  right: -14px;
  bottom: 8px;
  transform: rotate(18deg);
}

:global(.yard-gate.out.lane-primary::after) {
  right: -22px;
  bottom: 3px;
}

:global(.yard-gate.out.lane-secondary::before) {
  left: -14px;
  top: 9px;
  transform: rotate(-20deg);
}

:global(.yard-gate.out.lane-secondary::after) {
  left: -22px;
  top: 4px;
}

:global(.yard-gate b) {
  font-size: 13px;
  line-height: 1;
}

:global(.yard-gate small) {
  padding: 1px 6px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
}

:global(.yard-gate i) {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.16);
  border-radius: 50%;
  font-size: 10px;
  font-style: normal;
  line-height: 1;
}

:global(.vehicle-marker) {
  position: relative;
  display: block;
  width: 42px;
  height: 22px;
  background: #26384d;
  border: 2px solid #ffffff;
  box-shadow: 0 1px 4px #17263688;
}

:global(.vehicle-marker::before) {
  position: absolute;
  right: -8px;
  bottom: 0;
  width: 12px;
  height: 14px;
  background: #26384d;
  border: 2px solid #ffffff;
  border-left: 0;
  content: '';
}

:global(.vehicle-marker::after) {
  position: absolute;
  right: 0;
  top: 3px;
  width: 7px;
  height: 5px;
  background: #9fd3ff;
  content: '';
}

:global(.vehicle-marker i) {
  position: absolute;
  left: 5px;
  bottom: -5px;
  width: 7px;
  height: 7px;
  background: #ffffff;
  border: 2px solid #26384d;
  border-radius: 50%;
}

:global(.vehicle-marker b) {
  position: absolute;
  right: -11px;
  top: -11px;
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  color: #ffffff;
  background: #b8403a;
  border: 2px solid #ffffff;
  border-radius: 50%;
  font-size: 10px;
}

:global(.yard-zone-label) {
  display: grid;
  place-items: center;
  color: #243748;
  font-size: 12px;
  font-weight: 900;
  text-align: center;
  text-shadow: 0 1px #ffffff;
}

:global(.gate-popup),
:global(.sector-popup),
:global(.vehicle-popup) {
  display: grid;
  gap: 4px;
  min-width: 220px;
}

:global(.gate-popup b),
:global(.sector-popup b),
:global(.vehicle-popup b) {
  margin-bottom: 3px;
}

:global(.gate-popup div),
:global(.sector-popup div),
:global(.vehicle-popup div) {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 6px;
}

:global(.gate-popup span),
:global(.sector-popup span),
:global(.vehicle-popup span) {
  color: #5d6875;
  font-size: 11px;
}

:global(.gate-popup strong),
:global(.sector-popup strong),
:global(.vehicle-popup strong) {
  overflow-wrap: anywhere;
  font-size: 11px;
}

@media (max-width: 900px) {
  .map-layout {
    height: auto;
    min-height: 0;
    grid-template-columns: 1fr;
  }

  .summary-panel {
    order: -1;
  }

  .map-panel,
  .yard-map {
    min-height: 520px;
    height: 520px;
  }

  .map-toolbar {
    left: 12px;
    right: 12px;
    transform: none;
    width: auto;
    max-width: none;
    grid-template-columns: 1fr 120px;
  }

  .toolbar-refresh,
  .toolbar-button {
    grid-column: span 1;
  }
}
</style>


