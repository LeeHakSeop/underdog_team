<script setup>
import CarrierVehicleRegisterView from './CarrierVehicleRegisterView.vue'
import CarrierWorkOrdersView from './CarrierWorkOrdersView.vue'
</script>

<template>
  <div class="page-stack">
    <section class="panel workflow-panel">
      <div class="section-title">
        <h2>배정·작업 조회</h2>
        <span class="status-pill green">조회 전용</span>
      </div>
      <p>기사·트레일러 배정 현황과 작업지시 처리 기록을 한 화면에서 확인합니다.</p>
      <div class="workflow-steps">
        <span>01 배정 현황</span>
        <span>02 작업지시 목록</span>
        <span>03 완료 내역</span>
      </div>
    </section>

    <CarrierVehicleRegisterView
      :embedded="true"
      :show-summary="true"
      :show-input="false"
    />

    <CarrierWorkOrdersView
      :embedded="true"
      :show-input="false"
      :show-list="true"
    />
  </div>
</template>

<style scoped>
.workflow-panel p {
  margin: 0 0 10px;
  color: var(--ink-700);
  font-weight: 700;
}

.workflow-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.workflow-steps span {
  padding: 9px 10px;
  color: var(--ink-700);
  background: #f6f9fd;
  border: 1px solid var(--line);
  font-weight: 800;
}

@media (max-width: 760px) {
  .workflow-steps {
    grid-template-columns: 1fr;
  }
}
</style>
