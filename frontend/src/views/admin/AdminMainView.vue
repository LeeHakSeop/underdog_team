<script setup>
import { computed, ref, watch } from 'vue'
import {
  gateLogs,
  getCarrierName,
  getContainer,
  getContainerNumber,
  getDriverName,
  getPlateNumber,
  getSectorByContainerId,
  plateRecognitions,
  workOrders,
} from '../../data/dbData'

const gateCells = computed(() => {
  const rows = gateLogs.map((log) => {
    const recognition = plateRecognitions.find((item) => item.gate_log_id === log.gate_log_id)
    return { ...log, recognition }
  })

  while (rows.length < 9) {
    const next = rows.length + 1
    rows.push({
      gate_log_id: `empty-${next}`,
      gate_number: `G-${String(next).padStart(2, '0')}`,
      gate_name: `게이트 ${String(next).padStart(2, '0')}`,
      in_out_type: next % 2 === 0 ? 'OUT' : 'IN',
      recognition: null,
    })
  }

  return rows.slice(0, 9)
})

const selectedGateId = ref(gateCells.value[0]?.gate_log_id || '')
const processType = ref(gateCells.value[0]?.in_out_type || 'IN')

const selectedGate = computed(() => {
  return gateCells.value.find((gate) => gate.gate_log_id === selectedGateId.value) || gateCells.value[0]
})

const matchedOrder = computed(() => {
  const vehicleId = selectedGate.value?.vehicle_id
  if (!vehicleId) return null
  return workOrders.find((order) => order.vehicle_id === vehicleId) || null
})

const matchedContainer = computed(() => {
  if (!matchedOrder.value) return null
  return getContainer(matchedOrder.value.container_id)
})

const matchedSector = computed(() => {
  if (!matchedOrder.value) return null
  return getSectorByContainerId(matchedOrder.value.container_id)
})

const processLabel = computed(() => (processType.value === 'IN' ? '입차 처리' : '출차 처리'))

watch(selectedGate, (gate) => {
  processType.value = gate?.in_out_type || 'IN'
})
</script>

<template>
  <div class="control-room">
    <section class="control-layout">
      <article class="cctv-wall">
        <button
          v-for="gate in gateCells"
          :key="gate.gate_log_id"
          class="cctv-cell"
          :class="{ active: gate.gate_log_id === selectedGateId, empty: !gate.recognition }"
          type="button"
          @click="selectedGateId = gate.gate_log_id"
        >
          <span class="gate-label">{{ gate.gate_name }}</span>
          <strong v-if="gate.recognition" class="detected-number">
            {{ gate.recognition.recognized_plate }}
          </strong>
          <em v-else>CCTV 대기</em>
        </button>
      </article>

      <aside class="recognition-panel">
        <div class="result-card">
          <small>번호판 인식 결과</small>
          <strong>{{ selectedGate?.recognition?.recognized_plate || '미인식' }}</strong>
          <span>{{ selectedGate?.gate_name }}</span>
        </div>

        <div class="decision-box">
          <button
            class="decision-button in"
            :class="{ selected: processType === 'IN' }"
            type="button"
            @click="processType = 'IN'"
          >
            입차
          </button>
          <button
            class="decision-button out"
            :class="{ selected: processType === 'OUT' }"
            type="button"
            @click="processType = 'OUT'"
          >
            출차
          </button>
        </div>

        <div class="info-stack">
          <section>
            <h3>작업 정보</h3>
            <dl>
              <div>
                <dt>작업 ID</dt>
                <dd>{{ matchedOrder?.work_order_id || '-' }}</dd>
              </div>
              <div>
                <dt>작업 유형</dt>
                <dd>{{ matchedOrder?.work_type || '-' }}</dd>
              </div>
              <div>
                <dt>예약 시간</dt>
                <dd>{{ matchedOrder?.reserved_time || '-' }}</dd>
              </div>
              <div>
                <dt>작업 상태</dt>
                <dd>{{ matchedOrder?.work_status || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section>
            <h3>차량 / 기사 / 운송사</h3>
            <dl>
              <div>
                <dt>차량</dt>
                <dd>{{ matchedOrder ? getPlateNumber(matchedOrder.vehicle_id) : '-' }}</dd>
              </div>
              <div>
                <dt>기사</dt>
                <dd>{{ matchedOrder ? getDriverName(matchedOrder.driver_id) : '-' }}</dd>
              </div>
              <div>
                <dt>운송사</dt>
                <dd>{{ matchedOrder ? getCarrierName(matchedOrder.carrier_id) : '-' }}</dd>
              </div>
            </dl>
          </section>

          <section>
            <h3>컨테이너 / 야드 섹터</h3>
            <dl>
              <div>
                <dt>컨테이너</dt>
                <dd>{{ matchedOrder ? getContainerNumber(matchedOrder.container_id) : '-' }}</dd>
              </div>
              <div>
                <dt>규격/유형</dt>
                <dd>{{ matchedContainer ? `${matchedContainer.container_size} / ${matchedContainer.container_type}` : '-' }}</dd>
              </div>
              <div>
                <dt>섹터</dt>
                <dd>{{ matchedSector?.sector_name || '-' }}</dd>
              </div>
            </dl>
          </section>
        </div>

        <div class="process-footer">
          <button class="primary-button process-button" :class="{ out: processType === 'OUT' }" type="button">
            {{ processLabel }}
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
        <div v-for="log in gateLogs" :key="log.gate_log_id" class="compact-row">
          <span>{{ log.entry_time || log.exit_time }}</span>
          <span>{{ getPlateNumber(log.vehicle_id) }}</span>
          <span>{{ log.gate_name }}</span>
          <span>{{ log.in_out_type }}</span>
          <span>{{ log.process_result }}</span>
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
