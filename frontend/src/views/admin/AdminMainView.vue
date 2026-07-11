<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]
const getValue = (row, camelKey, snakeKey) => row?.[camelKey] ?? row?.[snakeKey] ?? ''

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId)
  return getValue(vehicle, 'plateNumber', 'plate_number') || vehicleId || '-'
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

const gateCells = computed(() => {
  const rows = gateLogStore.gateLogs.slice(0, 9).map((log, index) => ({
    id: getValue(log, 'gateLogId', 'gate_log_id') || `G-${String(index + 1).padStart(2, '0')}`,
    gateNumber: getValue(log, 'gateNumber', 'gate_number') || `G-${String(index + 1).padStart(2, '0')}`,
    gateName: getValue(log, 'gateName', 'gate_name') || `게이트 ${String(index + 1).padStart(2, '0')}`,
    inOutType: getValue(log, 'inOutType', 'in_out_type') || 'IN',
    processResult: getValue(log, 'processResult', 'process_result') || '대기',
    vehicleId: getId(log, 'vehicleId'),
    recognizedVehicleNo: getPlateNumber(getId(log, 'vehicleId')),
    entryTime: getValue(log, 'entryTime', 'entry_time'),
    exitTime: getValue(log, 'exitTime', 'exit_time'),
  }))

  while (rows.length < 9) {
    const next = rows.length + 1
    rows.push({
      id: `G-${String(next).padStart(2, '0')}`,
      gateNumber: `G-${String(next).padStart(2, '0')}`,
      gateName: `게이트 ${String(next).padStart(2, '0')}`,
      inOutType: next % 2 === 0 ? 'OUT' : 'IN',
      processResult: '대기',
      vehicleId: null,
      recognizedVehicleNo: '',
      entryTime: '',
      exitTime: '',
    })
  }

  return rows
})

const selectedGate = computed(() => {
  return gateCells.value.find((gate) => gate.id === selectedGateId.value) || gateCells.value[0]
})

const matchedOrder = computed(() => {
  const vehicleId = selectedGate.value?.vehicleId
  if (!vehicleId) return null
  return workOrderStore.workOrders.find((order) => getId(order, 'vehicleId') === vehicleId) || null
})

const matchedContainer = computed(() => {
  if (!matchedOrder.value) return null
  return getContainer(getId(matchedOrder.value, 'containerId'))
})

const processLabel = computed(() => (processType.value === 'IN' ? '입차 처리' : '출차 처리'))

watch(selectedGate, (gate) => {
  processType.value = gate?.inOutType || 'IN'
})

onMounted(() => {
  gateLogStore.loadGateLogs()
  workOrderStore.loadWorkOrders()
  containerStore.loadContainers()
  vehicleStore.loadVehicles()
  driverStore.loadDrivers()
  carrierStore.loadCarriers()
})
</script>

<template>
  <div class="control-room">
    <section class="control-layout">
      <article class="cctv-wall">
        <button
          v-for="gate in gateCells"
          :key="gate.id"
          class="cctv-cell"
          :class="{ active: gate.id === selectedGateId, empty: !gate.vehicleId }"
          type="button"
          @click="selectedGateId = gate.id"
        >
          <span class="gate-label">{{ gate.gateName }}</span>
          <strong v-if="gate.vehicleId" class="detected-number">
            {{ gate.recognizedVehicleNo }}
          </strong>
          <em v-else>CCTV 대기</em>
        </button>
      </article>

      <aside class="recognition-panel">
        <div class="result-card">
          <small>번호판 인식 결과</small>
          <strong>{{ selectedGate?.recognizedVehicleNo || '미인식' }}</strong>
          <span>{{ selectedGate?.gateName }}</span>
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
              <div><dt>작업 상태</dt><dd>{{ getValue(matchedOrder, 'workStatus', 'work_status') || '-' }}</dd></div>
            </dl>
          </section>

          <section>
            <h3>차량 / 기사 / 운송사</h3>
            <dl>
              <div><dt>차량</dt><dd>{{ matchedOrder ? getPlateNumber(getId(matchedOrder, 'vehicleId')) : '-' }}</dd></div>
              <div><dt>기사</dt><dd>{{ matchedOrder ? getDriverName(getId(matchedOrder, 'driverId')) : '-' }}</dd></div>
              <div><dt>운송사</dt><dd>{{ matchedOrder ? getCarrierName(getId(matchedOrder, 'carrierId')) : '-' }}</dd></div>
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
          <button
            class="primary-button process-button"
            :class="{ out: processType === 'OUT' }"
            type="button"
            disabled
            title="트랙터/트레일러 검증은 AI 번호판 인식 메뉴에서 처리합니다."
          >
            AI 인식 메뉴에서 {{ processLabel }}
          </button>
        </div>
      </aside>
    </section>

    <section class="panel log-panel">
      <div class="section-title">
        <h2>게이트 입출차 기록</h2>
        <span class="status-pill">DB 기록</span>
      </div>
      <div class="compact-table">
        <div class="compact-row head">
          <span>시간</span>
          <span>차량</span>
          <span>게이트</span>
          <span>구분</span>
          <span>처리 결과</span>
        </div>
        <div v-for="log in gateLogStore.gateLogs" :key="log.gateLogId || log.gate_log_id" class="compact-row">
          <span>{{ log.entryTime || log.entry_time || log.exitTime || log.exit_time || '-' }}</span>
          <span>{{ getPlateNumber(getId(log, 'vehicleId')) }}</span>
          <span>{{ log.gateName || log.gate_name || '-' }}</span>
          <span>{{ log.inOutType || log.in_out_type || '-' }}</span>
          <span>{{ log.processResult || log.process_result || '-' }}</span>
        </div>
        <div v-if="gateLogStore.gateLogs.length === 0" class="compact-row">
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

.cctv-wall,
.recognition-panel,
.log-panel {
  background: #101624;
  border: 1px solid #263353;
  border-radius: 2px;
  box-shadow: none;
}

.control-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(360px, 0.9fr);
  gap: 10px;
}

.cctv-wall {
  display: grid;
  min-height: 650px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 4px;
  padding: 6px;
}

.cctv-cell {
  position: relative;
  min-width: 0;
  overflow: hidden;
  color: #dceaff;
  background: #000000;
  border: 1px solid #303b5c;
  border-radius: 1px;
}

.cctv-cell.active {
  border-color: #d93a32;
  box-shadow: inset 0 0 0 2px #d93a32;
}

.cctv-cell.empty {
  background: #05070b;
}

.gate-label {
  position: absolute;
  left: 8px;
  top: 8px;
  z-index: 2;
  padding: 3px 6px;
  color: #ffffff;
  background: #23639c;
  border: 1px solid #6e94b7;
  border-radius: 1px;
  font-size: 11px;
  font-weight: 700;
}

.detected-number {
  position: absolute;
  left: 50%;
  top: 52%;
  transform: translate(-50%, -50%);
  color: #f6c34a;
  font-size: clamp(16px, 2vw, 24px);
  font-weight: 700;
}

.cctv-cell em {
  position: absolute;
  left: 50%;
  top: 52%;
  transform: translate(-50%, -50%);
  color: #57627b;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.recognition-panel {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 8px;
}

.log-panel .section-title h2 {
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
  background: #d93a32;
  border-color: #d93a32;
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
  .control-layout {
    grid-template-columns: 1fr;
  }

  .cctv-wall {
    min-height: 560px;
  }
}

@media (max-width: 760px) {
  .cctv-wall {
    min-height: 720px;
    grid-template-columns: 1fr;
    grid-template-rows: repeat(9, 180px);
  }
}
</style>
