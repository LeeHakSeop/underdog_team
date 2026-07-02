<script setup>
import { computed, ref } from 'vue'
import SectorMap from '../../components/yard/SectorMap.vue'
import { useLogisticsData } from '@/composables/useLogisticsData'

const {
  driverActions,
  getCarrierName,
  getContainerNumber,
  getSectorByContainerId,
  workOrders,
} = useLogisticsData()

const task = computed(() => workOrders.value[0] || null)
const decision = ref('pending')

const approved = computed(() => decision.value === 'approved')
const taskSector = computed(() => getSectorByContainerId(task.value?.container_id))
</script>

<template>
  <div class="page-stack driver-page">
    <section class="panel request-panel">
      <div class="section-title">
        <h2>배정 작업</h2>
        <span class="status-pill" :class="{ green: approved }">
          {{ approved ? '승인 완료' : '승인 대기' }}
        </span>
      </div>

      <div class="request-summary">
        <div>
          <span>작업 ID</span>
          <strong>{{ task?.work_order_id }}</strong>
        </div>
        <div>
          <span>운송사</span>
          <strong>{{ getCarrierName(task?.carrier_id) }}</strong>
        </div>
        <div>
          <span>컨테이너</span>
          <strong>{{ getContainerNumber(task?.container_id) }}</strong>
        </div>
        <div>
          <span>작업 유형</span>
          <strong>{{ task?.work_type }}</strong>
        </div>
        <div>
          <span>예약 시간</span>
          <strong>{{ task?.reserved_time }}</strong>
        </div>
        <div>
          <span>배정 섹터</span>
          <strong>{{ taskSector?.sector_name || '-' }}</strong>
        </div>
      </div>

      <div class="decision-actions">
        <button class="primary-button" type="button" @click="decision = 'approved'">수락</button>
        <button class="danger-button" type="button" @click="decision = 'rejected'">거절</button>
      </div>
    </section>

    <section v-if="approved" class="grid-2 driver-grid">
      <article class="panel map-panel">
        <SectorMap :selected-code="taskSector?.sector_name" />
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>작업 상태 입력</h2>
          <span class="status-pill green">기사 입력</span>
        </div>
        <div class="action-grid">
          <button v-for="action in driverActions" :key="action" class="action-button" type="button">
            {{ action }}
          </button>
        </div>
      </article>
    </section>

    <section v-else-if="decision === 'rejected'" class="panel rejected-panel">
      <strong>거절 처리됨</strong>
      <span>해당 작업은 기사 화면에서 거절 상태로 표시됩니다.</span>
    </section>
  </div>
</template>

<style scoped>
.driver-page {
  max-width: 1180px;
}

.request-panel {
  display: grid;
  gap: 16px;
}

.request-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.request-summary div {
  display: grid;
  gap: 4px;
  padding: 14px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.request-summary span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.request-summary strong {
  font-size: 17px;
  font-weight: 900;
}

.decision-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 160px));
  gap: 10px;
}

.danger-button {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: #d93a32;
  border: 1px solid #d93a32;
  border-radius: 7px;
  font-weight: 900;
}

.driver-grid {
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr);
}

.map-panel {
  min-width: 0;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-button {
  min-height: 70px;
  color: var(--blue-700);
  background: #f4f9ff;
  border: 1px solid #bfd8f8;
  border-radius: 8px;
  font-size: 17px;
  font-weight: 900;
}

.action-button:hover {
  color: #ffffff;
  background: var(--blue-700);
}

.rejected-panel {
  display: grid;
  gap: 4px;
  color: #9b3a31;
  background: #fff8f5;
  border-color: #f0cec5;
}

.rejected-panel strong {
  font-size: 18px;
  font-weight: 900;
}

@media (max-width: 900px) {
  .request-summary,
  .driver-grid {
    grid-template-columns: 1fr;
  }
}
</style>
