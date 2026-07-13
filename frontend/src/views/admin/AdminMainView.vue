<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useContainerStore } from '@/stores/adminStore/containerStore'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { useCarrierStore } from '@/stores/carrierStore'

const gateLogStore = useGateLogStore()
const workOrderStore = useWorkOrderStore()
const containerStore = useContainerStore()
const vehicleStore = useVehicleStore()
const driverStore = useDriverStore()
const carrierStore = useCarrierStore()

const selectedGateId = ref('G-01')
const processType = ref('IN')
let refreshTimer = null

const gateSlots = [
  { id: 'G-01', gateNumber: 'G01', gateName: '입차 게이트 1', inOutType: 'IN' },
  { id: 'G-02', gateNumber: 'G02', gateName: '입차 게이트 2', inOutType: 'IN' },
  { id: 'G-03', gateNumber: 'G03', gateName: '출차 게이트 1', inOutType: 'OUT' },
  { id: 'G-04', gateNumber: 'G04', gateName: '출차 게이트 2', inOutType: 'OUT' },
]

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]
const getValue = (row, camelKey, snakeKey) => row?.[camelKey] ?? row?.[snakeKey] ?? ''

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId)
  return getValue(vehicle, 'plateNumber', 'plate_number') || vehicleId || '-'
}

const getVehicle = (vehicleId) => {
  return vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId) || null
}

const getDriverName = (driverId) => {
  const driver = driverStore.drivers.find((item) => getId(item, 'driverId') === driverId)
  return getValue(driver, 'driverName', 'driver_name') || '-'
}

const getCarrierName = (carrierId) => {
  const carrier = carrierStore.carriers.find((item) => getId(item, 'carrierId') === carrierId)
  return getValue(carrier, 'carrierName', 'carrier_name') || '-'
}

const getContainer = (containerId) => {
  return containerStore.containers.find((item) => getId(item, 'containerId') === containerId) || null
}

const getContainerNumber = (containerId) => {
  const container = getContainer(containerId)
  return getValue(container, 'containerNumber', 'container_number') || '-'
}

const getWorkStatus = (order) => getValue(order, 'workStatus', 'work_status')

const statusText = (status) => {
  if (status === 'DISPATCH_WAITING') return '배차 대기'
  if (status === 'APPROVED') return '입차 대기'
  if (status === 'GATE_IN') return '입차 완료'
  if (status === 'IN_PROGRESS') return '작업 중'
  if (status === 'COMPLETED') return '출차 대기'
  if (status === 'GATE_OUT') return '출차 완료'
  return status || '-'
}

const gateText = (type) => (type === 'OUT' ? '출차' : '입차')

const latestGateLogs = computed(() => gateLogStore.gateLogs.slice(0, 8))

const todayGateIn = computed(() => gateLogStore.gateLogs.filter((log) => getValue(log, 'inOutType', 'in_out_type') === 'IN').length)
const todayGateOut = computed(() => gateLogStore.gateLogs.filter((log) => getValue(log, 'inOutType', 'in_out_type') === 'OUT').length)
const activeWorkCount = computed(() =>
  workOrderStore.workOrders.filter((order) => ['GATE_IN', 'IN_PROGRESS'].includes(getWorkStatus(order))).length,
)
const readyOutCount = computed(() =>
  workOrderStore.workOrders.filter((order) => getWorkStatus(order) === 'COMPLETED').length,
)

const statusCards = computed(() => [
  { label: '현재 입차', value: todayGateIn.value, detail: '게이트 IN 로그', tone: 'blue' },
  { label: '현재 출차', value: todayGateOut.value, detail: '게이트 OUT 로그', tone: 'red' },
  { label: '작업 진행', value: activeWorkCount.value, detail: '입차 완료/작업 중', tone: 'green' },
  { label: '출차 대기', value: readyOutCount.value, detail: '작업 완료 차량', tone: 'amber' },
])

const gateCells = computed(() => {
  const logsByType = {
    IN: [],
    OUT: [],
  }

  gateLogStore.gateLogs.forEach((log) => {
    const inOutType = getValue(log, 'inOutType', 'in_out_type') === 'OUT' ? 'OUT' : 'IN'
    logsByType[inOutType].push(log)
  })

  const usedLogs = new Set()

  return gateSlots.map((slot) => {
    const log =
      gateLogStore.gateLogs.find((item) => {
        const gateNumber = getValue(item, 'gateNumber', 'gate_number')
        return gateNumber === slot.gateNumber && !usedLogs.has(item)
      }) ||
      logsByType[slot.inOutType].find((item) => !usedLogs.has(item)) ||
      null

    if (log) {
      usedLogs.add(log)
    }

    const vehicleId = log ? getId(log, 'vehicleId') : null

    return {
      ...slot,
      processResult: log ? getValue(log, 'processResult', 'process_result') || '대기' : '대기',
      vehicleId,
      recognizedVehicleNo: vehicleId ? getPlateNumber(vehicleId) : '',
      entryTime: log ? getValue(log, 'entryTime', 'entry_time') : '',
      exitTime: log ? getValue(log, 'exitTime', 'exit_time') : '',
    }
  })
})

const selectedGate = computed(() => gateCells.value.find((gate) => gate.id === selectedGateId.value) || gateCells.value[0])

const matchedOrder = computed(() => {
  const vehicleId = selectedGate.value?.vehicleId
  if (!vehicleId) return null

  return (
    workOrderStore.workOrders.find((order) =>
      [getId(order, 'vehicleId'), getId(order, 'tractorVehicleId'), getId(order, 'trailerVehicleId')].includes(vehicleId),
    ) || null
  )
})

const matchedVehicle = computed(() => getVehicle(selectedGate.value?.vehicleId))
const matchedContainer = computed(() => (matchedOrder.value ? getContainer(getId(matchedOrder.value, 'containerId')) : null))

const selectedDriverName = computed(() => {
  if (matchedOrder.value) return getDriverName(getId(matchedOrder.value, 'driverId'))
  const driverId = getId(matchedVehicle.value, 'driverId')
  return driverId ? getDriverName(driverId) : '-'
})

const selectedCarrierName = computed(() => {
  const orderCarrierId = getId(matchedOrder.value, 'carrierId')
  if (orderCarrierId) return getCarrierName(orderCarrierId)

  const vehicleCarrierId = getId(matchedVehicle.value, 'carrierId')
  return vehicleCarrierId ? getCarrierName(vehicleCarrierId) : '-'
})

const activeOrders = computed(() =>
  workOrderStore.workOrders
    .filter((order) => ['APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED'].includes(getWorkStatus(order)))
    .slice(0, 6),
)

const yardSectors = computed(() => {
  const sectorMap = new Map()

  containerStore.containers.forEach((container) => {
    const sectorId = getId(container, 'sectorId') || `${container.block || '미지정'}-${container.bay || '-'}`
    const current = sectorMap.get(sectorId) || {
      id: sectorId,
      name: getValue(container, 'sectorName', 'sector_name') || container.block || '미지정',
      total: 0,
      canExit: 0,
      hold: 0,
    }

    current.total += 1
    if (container.canExit ?? container.can_exit) {
      current.canExit += 1
    } else {
      current.hold += 1
    }

    sectorMap.set(sectorId, current)
  })

  return Array.from(sectorMap.values()).slice(0, 8)
})

const processLabel = computed(() => (processType.value === 'IN' ? '입차 처리' : '출차 처리'))

const loadData = () => {
  gateLogStore.loadGateLogs().catch(() => {})
  workOrderStore.loadWorkOrders().catch(() => {})
  containerStore.loadContainers().catch(() => {})
  vehicleStore.loadVehicles().catch(() => {})
  driverStore.loadDrivers().catch(() => {})
  carrierStore.loadCarriers().catch(() => {})
}

watch(selectedGate, (gate) => {
  processType.value = gate?.inOutType || 'IN'
})

onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="control-room">
    <section class="ops-strip">
      <article v-for="card in statusCards" :key="card.label" class="ops-card" :class="card.tone">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}건</strong>
        <small>{{ card.detail }}</small>
      </article>
    </section>

    <section class="control-layout">
      <article class="cctv-wall" aria-label="4개 게이트 관제 현황">
        <button
          v-for="gate in gateCells"
          :key="gate.id"
          class="cctv-cell"
          :class="{ active: gate.id === selectedGateId, empty: !gate.vehicleId, out: gate.inOutType === 'OUT' }"
          type="button"
          @click="selectedGateId = gate.id"
        >
          <span class="gate-head">
            <b>{{ gate.gateName }}</b>
            <i>{{ gateText(gate.inOutType) }}</i>
          </span>
          <span class="gate-body">
            <strong v-if="gate.vehicleId" class="detected-number">
              {{ gate.recognizedVehicleNo }}
            </strong>
            <em v-else>{{ gateText(gate.inOutType) }} 대기</em>
          </span>
          <small class="gate-foot">{{ gate.processResult }}</small>
        </button>
      </article>

      <aside class="recognition-panel">
        <div class="result-card">
          <small>선택 게이트</small>
          <strong>{{ selectedGate?.recognizedVehicleNo || '미인식' }}</strong>
          <span>{{ selectedGate?.gateName }} / {{ gateText(selectedGate?.inOutType) }}</span>
        </div>

        <div class="decision-box">
          <button class="decision-button in" :class="{ selected: processType === 'IN' }" type="button" @click="processType = 'IN'">
            입차
          </button>
          <button class="decision-button out" :class="{ selected: processType === 'OUT' }" type="button" @click="processType = 'OUT'">
            출차
          </button>
        </div>

        <div class="info-stack">
          <section>
            <h3>작업 정보</h3>
            <dl>
              <div><dt>작업 ID</dt><dd>{{ getId(matchedOrder, 'workOrderId') || '-' }}</dd></div>
              <div><dt>작업 유형</dt><dd>{{ getValue(matchedOrder, 'workType', 'work_type') || '-' }}</dd></div>
              <div><dt>예약 시간</dt><dd>{{ getValue(matchedOrder, 'reservedTime', 'reserved_time') || '-' }}</dd></div>
              <div><dt>작업 상태</dt><dd>{{ statusText(getWorkStatus(matchedOrder)) }}</dd></div>
            </dl>
          </section>

          <section>
            <h3>차량 / 기사 / 운송사</h3>
            <dl>
              <div><dt>차량</dt><dd>{{ selectedGate?.vehicleId ? getPlateNumber(selectedGate.vehicleId) : '-' }}</dd></div>
              <div><dt>기사</dt><dd>{{ selectedDriverName }}</dd></div>
              <div><dt>운송사</dt><dd>{{ selectedCarrierName }}</dd></div>
            </dl>
          </section>

          <section>
            <h3>컨테이너 / 야드 섹터</h3>
            <dl>
              <div><dt>컨테이너</dt><dd>{{ matchedOrder ? getContainerNumber(getId(matchedOrder, 'containerId')) : '-' }}</dd></div>
              <div>
                <dt>규격/선사</dt>
                <dd>
                  {{
                    matchedContainer
                      ? `${getValue(matchedContainer, 'containerSize', 'container_size') || '-'} / ${getValue(matchedContainer, 'shippingLine', 'shipping_line') || '-'}`
                      : '-'
                  }}
                </dd>
              </div>
              <div>
                <dt>야드 위치</dt>
                <dd>
                  {{
                    matchedContainer
                      ? `${matchedContainer.block || '-'}-${matchedContainer.bay || '-'}-${matchedContainer.rowNo || matchedContainer.row_no || '-'}`
                      : '-'
                  }}
                </dd>
              </div>
            </dl>
          </section>
        </div>

        <div class="process-footer">
          <RouterLink class="primary-button process-button" :class="{ out: processType === 'OUT' }" to="/admin/plate-recognition">
            AI 인식 메뉴에서 {{ processLabel }}
          </RouterLink>
        </div>
      </aside>
    </section>

    <section class="monitor-grid">
      <article class="panel dark-panel">
        <div class="section-title dark-title">
          <h2>작업 진행 요약</h2>
          <span class="status-pill">{{ activeOrders.length }}건</span>
        </div>
        <div class="work-lane">
          <div v-for="order in activeOrders" :key="getId(order, 'workOrderId')" class="work-row">
            <b>{{ getPlateNumber(getId(order, 'vehicleId') || getId(order, 'trailerVehicleId')) }}</b>
            <span>{{ getContainerNumber(getId(order, 'containerId')) }}</span>
            <small>{{ statusText(getWorkStatus(order)) }}</small>
          </div>
          <div v-if="activeOrders.length === 0" class="empty-dark">진행 중인 작업이 없습니다.</div>
        </div>
      </article>

      <article class="panel dark-panel">
        <div class="section-title dark-title">
          <h2>야드 섹터 요약</h2>
          <span class="status-pill">{{ yardSectors.length }}개</span>
        </div>
        <div class="yard-grid">
          <div v-for="sector in yardSectors" :key="sector.id" class="yard-node">
            <b>{{ sector.name }}</b>
            <strong>{{ sector.total }}</strong>
            <span>반출 가능 {{ sector.canExit }} / 보류 {{ sector.hold }}</span>
          </div>
          <div v-if="yardSectors.length === 0" class="empty-dark">야드 섹터 데이터가 없습니다.</div>
        </div>
      </article>
    </section>

    <section class="panel log-panel">
      <div class="section-title dark-title">
        <h2>최근 게이트 입출차 기록</h2>
        <span class="status-pill">5초 갱신</span>
      </div>
      <div class="compact-table">
        <div class="compact-row head">
          <span>시간</span>
          <span>차량</span>
          <span>게이트</span>
          <span>구분</span>
          <span>처리 결과</span>
        </div>
        <div v-for="log in latestGateLogs" :key="log.gateLogId || log.gate_log_id" class="compact-row">
          <span>{{ log.entryTime || log.entry_time || log.exitTime || log.exit_time || '-' }}</span>
          <span>{{ getPlateNumber(getId(log, 'vehicleId')) }}</span>
          <span>{{ log.gateName || log.gate_name || '-' }}</span>
          <span>{{ gateText(log.inOutType || log.in_out_type) }}</span>
          <span>{{ log.processResult || log.process_result || '-' }}</span>
        </div>
        <div v-if="latestGateLogs.length === 0" class="compact-row">
          <span>-</span>
          <span>게이트 로그 데이터가 없습니다.</span>
          <span>-</span>
          <span>-</span>
          <span>-</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.control-room {
  display: grid;
  gap: 10px;
  color: #dceaff;
}

.ops-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.ops-card,
.cctv-wall,
.recognition-panel,
.log-panel,
.dark-panel {
  background: #101624;
  border: 1px solid #263353;
  border-radius: 2px;
  box-shadow: none;
}

.ops-card {
  display: grid;
  min-height: 76px;
  gap: 2px;
  padding: 10px 12px;
}

.ops-card span,
.ops-card small {
  color: #91a0c0;
  font-weight: 700;
}

.ops-card strong {
  color: #ffffff;
  font-size: 24px;
}

.ops-card.blue {
  border-left: 4px solid #2d75ae;
}

.ops-card.red {
  border-left: 4px solid #b8403a;
}

.ops-card.green {
  border-left: 4px solid #2f7d57;
}

.ops-card.amber {
  border-left: 4px solid #b47c1c;
}

.control-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(360px, 0.85fr);
  gap: 10px;
}

.cctv-wall {
  display: grid;
  min-height: 420px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(180px, 1fr));
  gap: 6px;
  padding: 8px;
}

.cctv-cell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  padding: 8px;
  color: #dceaff;
  background: #020407;
  border: 1px solid #303b5c;
  border-radius: 1px;
  text-align: left;
}

.cctv-cell.active {
  border-color: #f6c34a;
  box-shadow: inset 0 0 0 2px #f6c34a;
}

.cctv-cell.empty {
  background: #070b12;
}

.cctv-cell.out .gate-type {
  background: #b8403a;
}

.gate-head {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.gate-head b,
.gate-head i {
  display: inline-flex;
  min-height: 22px;
  align-items: center;
  padding: 3px 7px;
  color: #ffffff;
  border-radius: 1px;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 1.1;
  white-space: nowrap;
}

.gate-head b {
  min-width: 0;
  overflow: hidden;
  background: #23639c;
  border: 1px solid #6e94b7;
  text-overflow: ellipsis;
}

.gate-head i {
  flex: 0 0 auto;
  background: #2f7d57;
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.gate-body {
  display: grid;
  min-height: 0;
  place-items: center;
  padding: 8px 6px;
  text-align: center;
}

.detected-number {
  display: block;
  max-width: 100%;
  overflow-wrap: anywhere;
  color: #f6c34a;
  font-size: clamp(18px, 2.2vw, 28px);
  font-weight: 700;
  line-height: 1.15;
}

.cctv-cell em {
  color: #7f8aa6;
  color: #57627b;
  font-style: normal;
  font-weight: 700;
  line-height: 1.2;
}

.gate-foot {
  display: block;
  min-width: 0;
  overflow: hidden;
  padding: 5px 7px;
  color: #91a0c0;
  background: #0c1220;
  border: 1px solid #263353;
  border-radius: 1px;
  font-weight: 700;
  line-height: 1.2;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recognition-panel {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 8px;
}

.dark-title {
  background: #0c1220;
  border-color: #263353;
}

.dark-title h2 {
  color: #ffffff;
}

.result-card {
  display: grid;
  gap: 2px;
  padding: 10px;
  background: #e9eef4;
  border: 1px solid #9eb0c1;
  border-radius: 2px;
}

.result-card small {
  color: var(--ink-500);
  font-weight: 700;
}

.result-card strong {
  color: #172033;
  font-size: 28px;
  font-weight: 700;
}

.result-card span {
  color: #1f64b7;
  font-weight: 700;
}

.decision-box,
.process-footer {
  display: grid;
  gap: 6px;
}

.decision-box {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.decision-button {
  min-height: 34px;
  color: #dceaff;
  background: #1a233a;
  border: 1px solid #303b5c;
  border-radius: 2px;
  font-weight: 700;
}

.decision-button.in.selected {
  color: #ffffff;
  background: #1f64b7;
  border-color: #3f8beb;
}

.decision-button.out.selected,
.process-button.out {
  color: #ffffff;
  background: #b8403a;
  border-color: #b8403a;
}

.info-stack {
  display: grid;
  gap: 6px;
}

.info-stack section {
  padding: 8px;
  background: #151d31;
  border: 1px solid #263353;
  border-radius: 2px;
}

.info-stack h3 {
  margin: 0 0 6px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.info-stack dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.info-stack dl div {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 10px;
}

.info-stack dt {
  color: #91a0c0;
  font-size: 12px;
  font-weight: 700;
}

.info-stack dd {
  margin: 0;
  color: #dceaff;
  font-weight: 700;
}

.monitor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.work-lane,
.yard-grid {
  display: grid;
  gap: 6px;
}

.work-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(110px, 1fr) 96px;
  gap: 8px;
  padding: 8px;
  background: #151d31;
  border: 1px solid #263353;
}

.work-row b {
  color: #f6c34a;
}

.work-row span,
.work-row small {
  color: #dceaff;
  font-weight: 700;
}

.yard-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.yard-node {
  display: grid;
  gap: 3px;
  min-height: 74px;
  padding: 8px;
  background: #151d31;
  border: 1px solid #263353;
}

.yard-node b,
.yard-node span {
  color: #91a0c0;
}

.yard-node strong {
  color: #ffffff;
  font-size: 22px;
}

.empty-dark {
  padding: 12px;
  color: #91a0c0;
  background: #151d31;
  border: 1px solid #263353;
  font-weight: 700;
}

.log-panel {
  padding: 8px;
}

.compact-table {
  display: grid;
  gap: 3px;
  overflow-x: auto;
}

.compact-row {
  display: grid;
  min-width: 760px;
  grid-template-columns: 160px 1fr 100px 100px 130px;
  gap: 6px;
  padding: 6px 8px;
  color: #dceaff;
  background: #151d31;
  border: 1px solid #263353;
  border-radius: 1px;
  font-size: 12px;
  font-weight: 700;
}

.compact-row.head {
  color: #91a0c0;
  background: #0c1220;
}

@media (max-width: 1180px) {
  .control-layout,
  .monitor-grid {
    grid-template-columns: 1fr;
  }

  .cctv-wall {
    min-height: 420px;
  }
}

@media (max-width: 900px) {
  .ops-strip,
  .yard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .ops-strip,
  .yard-grid {
    grid-template-columns: 1fr;
  }

  .cctv-wall {
    min-height: 720px;
    grid-template-columns: 1fr;
    grid-template-rows: repeat(4, 180px);
  }
}
</style>
