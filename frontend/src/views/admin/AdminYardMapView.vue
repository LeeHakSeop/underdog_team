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
let map
let operationLayer
let refreshTimer

const yardMapStore = useYardMapStore()
const { blockSummary, containerCountBySectorId, error: storeError, gateSummary, yardSectors } = storeToRefs(yardMapStore)
const error = computed(() => mapError.value || storeError.value)

const gateLogFields = [
  { key: 'gateLogId', label: '게이트 로그 ID' },
  { key: 'vehicleId', label: '차량 ID' },
  { key: 'tractorVehicleId', label: '트랙터 차량 ID' },
  { key: 'trailerVehicleId', label: '트레일러 차량 ID' },
  { key: 'gateNumber', label: '게이트 번호' },
  { key: 'gateName', label: '게이트명' },
  { key: 'entryTime', label: '입차 시간' },
  { key: 'exitTime', label: '출차 시간' },
  { key: 'inOutType', label: '입출차 구분' },
  { key: 'processResult', label: '처리 결과' },
  { key: 'managerCheck', label: '관리자 확인' },
]

const formatGateValue = (log, key) => {
  const value = log?.[key]
  if (key === 'entryTime' || key === 'exitTime') return value ? String(value).replace('T', ' ') : '-'
  if (key === 'managerCheck') return value === true ? '확인' : value === false ? '미확인' : '-'
  return value ?? '-'
}

const escapeHtml = (value) => String(value ?? '-')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')

const gatePopupHtml = (gate) => {
  const log = gate.latestLog
  if (!log) return `<b>${escapeHtml(gate.gateName)}</b><br>gate_log 기록이 없습니다.`

  const details = gateLogFields
    .map((field) => `<div><span>${field.label}</span><strong>${escapeHtml(formatGateValue(log, field.key))}</strong></div>`)
    .join('')
  return `<div class="gate-popup"><b>${escapeHtml(gate.gateName)}</b>${details}</div>`
}

const getSectorStyle = (sector) => {
  const count = blockSummary.value.find((item) => item.sectorName === sector.sectorName)?.containerCount || 0
  if (count >= 20) return { color: '#b8403a', fillColor: '#e88b84' }
  if (count >= 10) return { color: '#b47c1c', fillColor: '#efd28f' }
  return { color: '#2f7d57', fillColor: '#93d4b5' }
}

const getBlockBounds = ({ center, widthMeters, heightMeters }) => {
  const [latitude, longitude] = center
  const latitudeOffset = (heightMeters / 2) / 111_320
  const longitudeOffset = (widthMeters / 2) / (111_320 * Math.cos((latitude * Math.PI) / 180))

  return [
    [latitude - latitudeOffset, longitude - longitudeOffset],
    [latitude + latitudeOffset, longitude + longitudeOffset],
  ]
}

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

const renderOperations = () => {
  if (!mapReady.value) return
  operationLayer?.clearLayers()

  yardMapLayout.sectorBlocks.forEach((shape) => {
    const sector = blockSummary.value.find((item) => item.sectorName === shape.sectorName)
    const count = sector?.containerCount || 0
    const style = getSectorStyle(shape)
    const zone = L.rectangle(getBlockBounds(shape), {
      color: '#6d7782',
      weight: 2,
      fillColor: style.fillColor,
      fillOpacity: 0.12,
    }).bindPopup(`<b>${shape.label}</b><br>컨테이너 ${count}대<br>대기 차량 ${sector?.waitingVehicleCount || 0}대`)
    operationLayer.addLayer(zone)

    getRoadLines(shape).forEach((roadLine) => {
      operationLayer.addLayer(L.polyline(roadLine, { color: '#f8fafc', weight: 5, opacity: 0.95, dashArray: '8 5' }))
    })

    getBlockSectors(shape.sectorName).slice(0, 20).forEach((yardSector, index) => {
      const containerCount = containerCountBySectorId.value.get(yardSector.sectorId) || 0
      const cell = L.rectangle(getSectorCellBounds(shape, Math.floor(index / 4), index % 4), {
        color: '#4c5966',
        weight: 1,
        fillColor: containerCount > 0 ? '#4a91b6' : '#d9e5ec',
        fillOpacity: 0.78,
      }).bindTooltip(`${yardSector.sectorName} · ${containerCount}대`, { sticky: true })
        .bindPopup(`<b>${yardSector.sectorName}</b><br>컨테이너 ${containerCount}대<br>대기 차량 ${yardSector.waitingVehicleCount || 0}대`)
      operationLayer.addLayer(cell)
    })

    operationLayer.addLayer(L.marker(shape.center, {
      interactive: false,
      icon: L.divIcon({ className: 'yard-zone-label', html: `<strong>${shape.label}</strong>`, iconSize: [70, 24], iconAnchor: [35, 12] }),
    }))
  })

  gateSummary.value.filter((gate) => gate.position).forEach((gate) => {
    const succeeded = gate.latestLog?.processResult === 'SUCCESS'
    const icon = L.divIcon({
      className: 'yard-gate-icon',
      html: `<span class="yard-gate ${gate.direction.toLowerCase()} ${succeeded ? 'active' : ''}">${gate.direction === 'IN' ? '↓' : '↑'}<small>${gate.gateNumber}</small></span>`,
      iconSize: [42, 42],
      iconAnchor: [21, 21],
    })
    const marker = L.marker(gate.position, { icon }).bindPopup(gatePopupHtml(gate))
    operationLayer.addLayer(marker)
  })
}

const refreshData = async () => {
  await yardMapStore.loadYardMap()
  await nextTick()
  renderOperations()
}

watch([blockSummary, gateSummary, yardSectors], renderOperations, { deep: true })

onMounted(async () => {
  try {
    map = L.map(mapElement.value, { zoomControl: true }).setView(yardMapLayout.center, yardMapLayout.zoom)
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
  <div class="page-stack">
    <section class="map-layout">
      <article class="panel map-panel">
        <div v-if="error" class="map-notice">{{ error }}</div>
        <div ref="mapElement" class="yard-map" aria-label="감만부두 운영 지도"></div>
      </article>

      <aside class="panel summary-panel">
        <section class="summary-group">
          <div class="summary-heading"><strong>게이트 현황</strong><small>{{ gateSummary.length }}개 게이트 · 최신 로그 1건씩</small></div>
          <div class="summary-content gate-list">
            <details v-for="gate in gateSummary" :key="gate.gateNumber" class="gate-card">
              <summary>
                <span>
                  <strong>{{ gate.gateName || gate.gateNumber }}</strong>
                  <small>{{ gate.gateNumber }}</small>
                </span>
                <b>최근 {{ formatGateValue(gate.latestLog, 'processResult') }}</b>
              </summary>
              <dl v-if="gate.latestLog" class="gate-log-detail">
                <div v-for="field in gateLogFields" :key="field.key">
                  <dt>{{ field.label }}</dt>
                  <dd>{{ formatGateValue(gate.latestLog, field.key) }}</dd>
                </div>
              </dl>
              <p v-else class="empty">gate_log 기록이 없습니다.</p>
            </details>
          </div>
        </section>

        <section class="summary-group">
          <div class="summary-heading"><strong>야드 섹터</strong><small>yard_sector {{ yardSectors.length }}건</small></div>
          <div class="summary-content">
            <div class="legend"><span class="green"></span>여유 <span class="amber"></span>주의 <span class="red"></span>혼잡</div>
            <div class="sector-list">
              <details
                v-for="sector in yardSectors"
                :key="sector.sectorId"
                class="sector-row"
              >
                <summary>
                  <strong>{{ sector.sectorName }}</strong>
                  <span>{{ sector.sectorStatus || '-' }}</span>
                  <small>{{ sector.blockName }} · 대기 차량 {{ sector.waitingVehicleCount || 0 }}대</small>
                </summary>
                <dl class="sector-detail">
                  <div><dt>섹터 ID</dt><dd>{{ sector.sectorId }}</dd></div>
                  <div><dt>블록명</dt><dd>{{ sector.blockName }}</dd></div>
                  <div><dt>안내</dt><dd>{{ sector.guideMessage || '-' }}</dd></div>
                  <div><dt>대체 대기장</dt><dd>{{ sector.altWaitingArea || '-' }}</dd></div>
                </dl>
              </details>
              <p v-if="yardSectors.length === 0" class="empty">야드 섹터 데이터가 없습니다.</p>
            </div>
          </div>
        </section>

      </aside>
    </section>
  </div>
</template>

<style scoped>
.map-layout { display: grid; height: calc(100vh - 78px); min-height: 650px; grid-template-columns: minmax(0, 1fr) 270px; gap: 10px; }
.map-panel { position: relative; height: 100%; padding: 0; overflow: hidden; }
.yard-map { width: 100%; height: 100%; background: #dce6ec; }
.map-notice { position: absolute; z-index: 500; margin: 12px; padding: 7px 9px; color: #8a5a00; background: #fff5dc; border: 1px solid #efd28f; }
.summary-panel { align-self: stretch; padding: 0; overflow-y: auto; }
.summary-group + .summary-group { border-top: 1px solid var(--line); }
.summary-heading { display: grid; gap: 2px; padding: 9px 10px; background: linear-gradient(#f8fafc, #e3eaf2); border-bottom: 1px solid var(--line); }.summary-heading strong { font-size: 14px; }.summary-heading small { color: var(--ink-500); font-size: 11px; }
.summary-content { padding: 10px; }.gate-list { display: grid; gap: 8px; }
.gate-card, .sector-row { background: #f6f9fd; border: 1px solid var(--line); }.gate-card summary, .sector-row summary { display: grid; width: 100%; cursor: pointer; list-style: none; }.gate-card summary::-webkit-details-marker, .sector-row summary::-webkit-details-marker { display: none; }.gate-card summary { grid-template-columns: minmax(0, 1fr) auto; gap: 8px; padding: 8px; }.gate-card summary span { display: grid; gap: 2px; }.gate-card summary small { color: var(--ink-500); font-size: 11px; }.gate-card summary b { align-self: center; color: var(--blue-700); font-size: 11px; }.gate-card[open], .sector-row[open] { background: #eaf2f9; border-color: #9eb8d0; }
.gate-log-detail, .sector-detail { display: grid; gap: 3px; margin: 0 8px 8px; padding-top: 7px; border-top: 1px solid #d4dde7; }.gate-log-detail div, .sector-detail div { display: grid; grid-template-columns: 102px minmax(0, 1fr); gap: 6px; }.gate-log-detail dt, .sector-detail dt { color: var(--ink-500); font-size: 11px; }.gate-log-detail dd, .sector-detail dd { min-width: 0; margin: 0; overflow-wrap: anywhere; font-size: 11px; font-weight: 700; }
.legend { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-bottom: 10px; color: var(--ink-500); font-size: 11px; font-weight: 700; }
.legend span { width: 11px; height: 11px; border: 1px solid #758595; }
.legend .green { background: #93d4b5; }.legend .amber { background: #efd28f; }.legend .red { background: #e88b84; }
.sector-list { display: grid; max-height: 380px; gap: 6px; overflow-y: auto; }
.sector-row summary { grid-template-columns: minmax(0, 1fr) auto; gap: 3px 8px; padding: 8px; }.sector-row summary small { grid-column: 1 / -1; color: var(--ink-500); }.empty { color: var(--ink-500); }
:global(.yard-gate-icon) { background: transparent; border: 0; }
:global(.yard-gate) { display: grid; width: 38px; height: 38px; place-items: center; color: #fff; background: #23639c; border: 2px solid #fff; border-radius: 50%; box-shadow: 0 1px 4px #17263688; font-size: 18px; font-weight: 900; }
:global(.yard-gate.out) { background: #b8403a; }:global(.yard-gate.active) { box-shadow: 0 0 0 4px #f6c34a, 0 1px 4px #17263688; }
:global(.yard-gate small) { margin-top: -5px; font-size: 8px; }
:global(.yard-zone-label) { display: grid; place-items: center; color: #243748; font-size: 12px; font-weight: 900; text-align: center; text-shadow: 0 1px #ffffff; }
:global(.gate-popup) { display: grid; gap: 4px; min-width: 220px; }:global(.gate-popup b) { margin-bottom: 3px; }:global(.gate-popup div) { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 6px; }:global(.gate-popup span) { color: #5d6875; font-size: 11px; }:global(.gate-popup strong) { overflow-wrap: anywhere; font-size: 11px; }
@media (max-width: 900px) { .map-layout { height: auto; min-height: 0; grid-template-columns: 1fr; }.summary-panel { order: -1; }.map-panel, .yard-map { min-height: 480px; height: 480px; } }
</style>
