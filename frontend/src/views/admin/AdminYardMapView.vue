<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { yardMapLayout } from '@/config/yardMapLayout'
import { useYardMapStore } from '@/stores/adminStore/yardMapStore'

const mapElement = ref(null)
const mapReady = ref(false)
const mapError = ref('')
const selectedSectorId = ref(null)
const selectedWorkOrderId = ref(null)
const searchQuery = ref('')
const statusFilter = ref('ALL')
let map
let operationLayer
let refreshTimer

const yardMapStore = useYardMapStore()
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

const statusOptions = [
  { value: 'ALL', label: '전체' },
  { value: 'NORMAL', label: '정상' },
  { value: 'WARNING', label: '주의' },
  { value: 'DANGER', label: '혼잡' },
]

const error = computed(() => mapError.value || storeError.value)
const normalizedSearch = computed(() => searchQuery.value.trim().toLowerCase())
const selectedSector = computed(() => yardSectors.value.find((sector) => sector.sectorId === selectedSectorId.value) || null)
const selectedSectorVehicles = computed(() => vehicles.value.filter((vehicle) => vehicle.sectorId === selectedSectorId.value))
const selectedWorkVehicle = computed(() => {
  const selectedFromId = vehicles.value.find((vehicle) => vehicle.workOrderId === selectedWorkOrderId.value)
  return selectedFromId || selectedSectorVehicles.value[0] || null
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
    { label: '작업 차량', value: `${selectedSectorVehicles.value.length}대` },
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
  if (statusLevel === 'DANGER') return '혼잡'
  if (statusLevel === 'WARNING') return '주의'
  return '정상'
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

function sectorAlertBadges(sector) {
  if (!sector) return []
  const badges = []
  if ((sector.statusLevel || 'NORMAL') === 'DANGER') badges.push({ type: 'danger', label: '혼잡' })
  if ((sector.statusLevel || 'NORMAL') === 'WARNING') badges.push({ type: 'warning', label: '주의' })
  if (Number(sector.usageRate || 0) >= 80) badges.push({ type: 'danger', label: '사용률 80%↑' })
  else if (Number(sector.usageRate || 0) >= 50) badges.push({ type: 'warning', label: '사용률 50%↑' })
  if (Number(sector.waitingVehicleCount || 0) >= 6) badges.push({ type: 'danger', label: '대기차량 많음' })
  else if (Number(sector.waitingVehicleCount || 0) >= 3) badges.push({ type: 'warning', label: '대기차량 증가' })
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
        .bindTooltip(`${yardSector.sectorName} / ${statusLabel(yardSector.statusLevel)} / 사용률 ${formatPercent(yardSector.usageRate)}`, { sticky: true })
        .bindPopup(sectorPopupHtml(yardSector))
        .on('click', () => {
          selectedSectorId.value = yardSector.sectorId
          selectedWorkOrderId.value = null
        })
      operationLayer.addLayer(cell)
    })

    operationLayer.addLayer(L.marker(block.center, {
      interactive: false,
      icon: L.divIcon({ className: 'yard-zone-label', html: `<strong>${block.label}</strong>`, iconSize: [70, 24], iconAnchor: [35, 12] }),
    }))
  })

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
    const icon = L.divIcon({
      className: 'yard-gate-icon',
      html: `<span class="yard-gate ${gate.direction.toLowerCase()}"><b>${formatDirection(gate.direction)}</b><small>${gate.gateNumber}</small></span>`,
      iconSize: [52, 42],
      iconAnchor: [26, 21],
    })
    operationLayer.addLayer(L.marker(gate.position, { icon }).bindPopup(gatePopupHtml(gate)))
  })
}

const refreshData = async () => {
  await yardMapStore.loadYardMap()
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
        <div class="refresh-panel" :class="{ stale }">
          <div>
            <strong>{{ stale ? '이전 데이터 표시 중' : '실시간 갱신' }}</strong>
            <small>최근 갱신 {{ formatDateTime(lastUpdatedAt) }}</small>
            <small v-if="failedAt">최근 실패 {{ formatDateTime(failedAt) }} / {{ failureCount }}회</small>
          </div>
          <button type="button" :disabled="loading" @click="manualRefresh">
            {{ loading ? '갱신 중' : '새로고침' }}
          </button>
        </div>
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
        </div>
        <div ref="mapElement" class="yard-map" aria-label="감만부두 운영 지도"></div>
      </article>

      <aside class="panel summary-panel">
        <section class="summary-group">
          <div class="summary-heading">
            <strong>게이트 현황</strong>
            <small>입차 2개 / 출차 2개</small>
          </div>
          <div class="summary-content gate-list">
            <div v-for="gate in gateSummary" :key="gate.gateNumber" class="gate-card">
              <div>
                <strong>{{ gate.gateName || gate.gateNumber }}</strong>
                <small>{{ gate.gateNumber }} / {{ formatDirection(gate.direction) }}</small>
              </div>
              <dl>
                <div><dt>최근 처리</dt><dd>{{ gate.latestProcessResult || '-' }}</dd></div>
                <div><dt>최근 시간</dt><dd>{{ formatDateTime(gate.latestExitTime || gate.latestEntryTime) }}</dd></div>
                <div><dt>오늘 입차</dt><dd>{{ formatCount(gate.todayInCount) }}</dd></div>
                <div><dt>오늘 출차</dt><dd>{{ formatCount(gate.todayOutCount) }}</dd></div>
              </dl>
            </div>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading">
            <strong>상태 범례</strong>
            <small>검색 결과 {{ visibleSectors.length }}개</small>
          </div>
          <div class="summary-content">
            <div class="legend">
              <button type="button" class="legend-item normal" @click="statusFilter = 'NORMAL'">
                <i></i><span>정상</span><b>{{ statusCounts.NORMAL }}</b>
              </button>
              <button type="button" class="legend-item warning" @click="statusFilter = 'WARNING'">
                <i></i><span>주의</span><b>{{ statusCounts.WARNING }}</b>
              </button>
              <button type="button" class="legend-item danger" @click="statusFilter = 'DANGER'">
                <i></i><span>혼잡</span><b>{{ statusCounts.DANGER }}</b>
              </button>
            </div>
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
                @click="selectedSectorId = sector.sectorId"
              >
                <strong>{{ sector.sectorName }}</strong>
                <span>{{ statusLabel(sector.statusLevel) }}</span>
                <small>
                  {{ sector.blockName }}구역 / 차량 {{ vehicleCountBySectorId.get(sector.sectorId) || 0 }}대 / 작업 {{ sector.workOrderCount || 0 }}건
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
            <div><dt>컨테이너</dt><dd>{{ formatCount(selectedSector?.containerCount) }}</dd></div>
            <div><dt>수용량</dt><dd>{{ formatCount(selectedSector?.capacity) }}</dd></div>
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
            <div><dt>작업상태</dt><dd>{{ workStatusLabel(selectedWorkVehicle?.workStatus) }}</dd></div>
            <div><dt>작업유형</dt><dd>{{ selectedWorkVehicle?.workType || '-' }}</dd></div>
            <div><dt>예약시간</dt><dd>{{ formatDateTime(selectedWorkVehicle?.reservedTime) }}</dd></div>
            <div><dt>기사</dt><dd>{{ selectedWorkVehicle?.driverName || '-' }}</dd></div>
            <div><dt>운송사</dt><dd>{{ selectedWorkVehicle?.carrierName || '-' }}</dd></div>
            <div><dt>출발</dt><dd>{{ selectedWorkVehicle?.originLocation || '-' }}</dd></div>
            <div><dt>목적</dt><dd>{{ selectedWorkVehicle?.destinationSectorName || selectedWorkVehicle?.sectorName || '-' }}</dd></div>
            <div><dt>이동구간</dt><dd>{{ selectedWorkVehicle?.routeSummary || '-' }}</dd></div>
            <div><dt>섹터</dt><dd>{{ selectedWorkVehicle?.sectorName || selectedSector?.sectorName || '-' }}</dd></div>
            <div><dt>컨테이너</dt><dd>{{ selectedWorkVehicle?.containerNumber || '-' }}</dd></div>
            <div><dt>규격/위치</dt><dd>{{ selectedWorkVehicle?.containerSize || '-' }} / {{ selectedWorkVehicle?.containerLocation || '-' }}</dd></div>
            <div><dt>출차상태</dt><dd>{{ canExitLabel(selectedWorkVehicle?.canExit) }}</dd></div>
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
.map-loading,
.refresh-panel {
  position: absolute;
  z-index: 500;
}

.map-toolbar {
  display: flex;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  gap: 8px;
  align-items: end;
  max-width: calc(100% - 100px);
  padding: 8px;
  background: #ffffff;
  border: 1px solid #b9c5d1;
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
  width: min(250px, 34vw);
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

.refresh-panel {
  display: flex;
  left: 12px;
  bottom: 12px;
  gap: 10px;
  align-items: center;
  max-width: min(360px, calc(100% - 84px));
  padding: 8px 9px;
  color: #1f2933;
  background: #ffffff;
  border: 1px solid #b9c5d1;
}

.refresh-panel.stale {
  color: #7a5300;
  background: #fff5dc;
  border-color: #d5b766;
}

.refresh-panel div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.refresh-panel strong {
  font-size: 12px;
}

.refresh-panel small {
  color: #5d6875;
  font-size: 11px;
  font-weight: 700;
}

.refresh-panel button {
  min-width: 74px;
  height: 30px;
  color: #ffffff;
  background: #23639c;
  border: 1px solid #23639c;
  font-size: 12px;
  font-weight: 700;
}

.refresh-panel button:disabled {
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

.gate-list,
.sector-list,
.vehicle-list {
  display: grid;
  gap: 8px;
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

:global(.yard-gate-icon),
:global(.yard-vehicle-icon) {
  background: transparent;
  border: 0;
}

:global(.yard-gate) {
  display: grid;
  width: 48px;
  height: 38px;
  place-items: center;
  color: #fff;
  background: #23639c;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px #17263688;
  font-weight: 900;
}

:global(.yard-gate.out) {
  background: #b8403a;
}

:global(.yard-gate b) {
  font-size: 12px;
}

:global(.yard-gate small) {
  margin-top: -5px;
  font-size: 9px;
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
    max-width: none;
  }
}
</style>
