<script setup>
import CarrierVehicleRegisterView from './CarrierVehicleRegisterView.vue'
import CarrierWorkOrdersView from './CarrierWorkOrdersView.vue'
</script>

<template>
  <div class="page-stack">
    <section class="panel workflow-panel">
      <div class="section-title">
        <h2>배정·작업 입력 및 수정</h2>
        <span class="status-pill amber">입력·수정</span>
      </div>
      <p>트레일러 배정 정보를 입력한 뒤, 기사 작업지시를 등록하거나 승인 대기 중인 내용을 수정합니다.</p>
      <div class="workflow-steps">
        <span>01 트레일러 배정 입력</span>
        <span>02 작업지시 입력</span>
        <span>03 승인 전 수정</span>
      </div>
    </section>

    <CarrierVehicleRegisterView
      :embedded="true"
      :show-summary="false"
      :show-input="true"
    />

    <CarrierWorkOrdersView
      :embedded="true"
      :show-input="true"
      :show-list="false"
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
  background: #fffaf0;
  border: 1px solid #ead4a7;
  font-weight: 800;
}

@media (max-width: 760px) {
  .workflow-steps {
    grid-template-columns: 1fr;
  }
}
</style>
