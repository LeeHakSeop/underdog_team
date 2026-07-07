<script setup>
import { computed, ref, watch } from 'vue'
import { containers, gateCameras, gateLogs, workOrders } from '../../data/mockData'

const selectedGateId = ref(gateCameras[0]?.id || '')
const processType = ref(gateCameras[0]?.gateType || 'IN')

const selectedGate = computed(() => {
  return gateCameras.find((camera) => camera.id === selectedGateId.value) || gateCameras[0]
})

const matchedOrder = computed(() => {
  if (!selectedGate.value?.matchedOrderNo) return null
  return workOrders.find((order) => order.orderNo === selectedGate.value.matchedOrderNo) || null
})

const matchedContainer = computed(() => {
  if (!matchedOrder.value) return null
  return containers.find((container) => container.containerNo === matchedOrder.value.containerNo) || null
})

const gateRows = computed(() => {
  const rows = [...gateCameras]
  while (rows.length < 9) {
    const next = rows.length + 1
    rows.push({
      id: `G-${String(next).padStart(2, '0')}`,
      name: `게이트 ${String(next).padStart(2, '0')}`,
      gateType: next % 2 === 0 ? 'OUT' : 'IN',
      status: '대기',
      recognizedVehicleNo: '',
      matchedOrderNo: '',
    })
  }
  return rows.slice(0, 9)
})

const processLabel = computed(() => (processType.value === 'IN' ? '입차 처리' : '출차 처리'))

watch(selectedGate, (gate) => {
  processType.value = gate?.gateType || 'IN'
})
</script>

<template>
  <div class="control-room">
    <section class="control-layout">
      <article class="cctv-wall">
        <button
          v-for="gate in gateRows"
          :key="gate.id"
          class="cctv-cell"
          :class="{ active: gate.id === selectedGateId, empty: !gate.recognizedVehicleNo }"
          type="button"
          @click="selectedGateId = gate.id"
        >
          <span class="gate-label">{{ gate.name }}</span>
          <strong v-if="gate.recognizedVehicleNo" class="detected-number">
            {{ gate.recognizedVehicleNo }}
          </strong>
          <em v-else>CCTV 대기</em>
        </button>
      </article>

      <aside class="recognition-panel">
        <div class="result-card">
          <small>선택 게이트</small>
          <strong>{{ selectedGate.recognizedVehicleNo || '미확인' }}</strong>
          <span>{{ selectedGate.name }}</span>
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
                <dt>작업번호</dt>
                <dd>{{ matchedOrder?.orderNo || '-' }}</dd>
              </div>
              <div>
                <dt>작업유형</dt>
                <dd>{{ matchedOrder?.workType || '-' }}</dd>
              </div>
              <div>
                <dt>예약시간</dt>
                <dd>{{ matchedOrder?.time || '-' }}</dd>
              </div>
              <div>
                <dt>작업상태</dt>
                <dd>{{ matchedOrder?.status || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section>
            <h3>차량 · 기사 · 운송사</h3>
            <dl>
              <div>
                <dt>차량번호</dt>
                <dd>{{ matchedOrder?.vehicleNo || '-' }}</dd>
              </div>
              <div>
                <dt>기사명</dt>
                <dd>{{ matchedOrder?.driverName || '-' }}</dd>
              </div>
              <div>
                <dt>운송사</dt>
                <dd>{{ matchedOrder?.carrierName || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section>
            <h3>컨테이너 · 섹터</h3>
            <dl>
              <div>
                <dt>컨테이너</dt>
                <dd>{{ matchedOrder?.containerNo || '-' }}</dd>
              </div>
              <div>
                <dt>규격/유형</dt>
                <dd>{{ matchedContainer ? `${matchedContainer.sizeType} / ${matchedContainer.type}` : '-' }}</dd>
              </div>
              <div>
                <dt>배정 섹터</dt>
                <dd>{{ matchedOrder?.sectorCode || '-' }}</dd>
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
        <h2>최근 게이트 기록</h2>
        <span class="status-pill">DB gate_log</span>
      </div>
      <div class="compact-table">
        <div class="compact-row head">
          <span>시간</span>
          <span>차량번호</span>
          <span>게이트</span>
          <span>구분</span>
          <span>처리 결과</span>
        </div>
        <div v-for="log in gateLogs" :key="log.logNo" class="compact-row">
          <span>{{ log.time }}</span>
          <span>{{ log.vehicleNo }}</span>
          <span>{{ log.gate }}</span>
          <span>{{ log.type }}</span>
          <span>{{ log.result }}</span>
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

.decision-button.out.selected {
  color: #ffffff;
  background: #d93a32;
  border-color: #f08b85;
}

.process-button.out {
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
  grid-template-columns: 88px 1fr;
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
  grid-template-columns: 80px 1fr 100px 90px 130px;
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
