<script setup>
import { computed, ref } from 'vue'
import AntennaHistoryPlayback from '@/components/predictive/AntennaHistoryPlayback.vue'
import MaintenanceRecordBoard from '@/components/predictive/MaintenanceRecordBoard.vue'
import PredictiveTypeSelector from '@/components/predictive/PredictiveTypeSelector.vue'
import {
  predictiveMaintenanceSections,
  predictiveMaintenanceTypes,
} from '@/config/predictiveMaintenance'

const selectedType = ref('ANTENNA')
const selectedSection = ref('dashboard')

const currentType = computed(() =>
  predictiveMaintenanceTypes.find((type) => type.code === selectedType.value),
)

const sectionDescriptions = {
  equipment: '설비 기본 정보와 현재 상태를 조회하고 등록·수정·사용 중지합니다.',
  readings: '설비별 시간 순서 센서 데이터를 조회하고 관리합니다.',
  alerts: '예측 경보를 접수하고 담당자와 처리 상태를 기록합니다.',
  maintenance: '점검·예방정비·수리·교체 일정과 처리 결과를 관리합니다.',
  models: '적용 모델, 버전, 임계값과 검증 지표를 확인합니다.',
}
</script>

<template>
  <div class="page-stack predictive-page">
    <PredictiveTypeSelector v-model="selectedType" />

    <nav class="section-tabs" aria-label="예지보전 하위 메뉴">
      <button
        v-for="section in predictiveMaintenanceSections"
        :key="section.code"
        type="button"
        :class="{ active: selectedSection === section.code }"
        @click="selectedSection = section.code"
      >
        {{ section.label }}
      </button>
    </nav>

    <template v-if="selectedSection === 'dashboard'">
      <section class="panel intro-panel">
        <div>
          <span class="eyebrow">PREDICTIVE MAINTENANCE</span>
          <h2>{{ currentType?.label }}</h2>
          <p>{{ currentType?.description }}</p>
        </div>
        <div class="data-basis">
          <span>현재 데이터 기준</span>
          <strong>합성 CSV 기록 재생</strong>
        </div>
      </section>

      <AntennaHistoryPlayback v-if="selectedType === 'ANTENNA'" />

      <section class="grid-2 dashboard-panels">
        <article class="panel">
          <div class="section-title">
            <h2>관리자 판단</h2>
            <span class="status-pill gray">기능 연결 예정</span>
          </div>
          <div class="empty-state">
            모델 판단에 동의하거나 다른 판단을 선택하고, 근거와 점검 필요 여부를 기록하는 영역입니다.
          </div>
        </article>

        <article class="panel">
          <div class="section-title">
            <h2>경보·정비 이력</h2>
            <span class="status-pill gray">기능 연결 예정</span>
          </div>
          <div class="empty-state">
            선택한 안테나의 모델 경보, 실제 고장, 관리자 조치와 정비 결과를 시간순으로 표시합니다.
          </div>
        </article>
      </section>
    </template>

    <MaintenanceRecordBoard
      v-else-if="selectedSection === 'maintenance' && selectedType === 'ANTENNA'"
    />

    <section v-else class="panel feature-placeholder">
      <div class="section-title">
        <h2>{{ predictiveMaintenanceSections.find((item) => item.code === selectedSection)?.label }}</h2>
        <span class="status-pill gray">구현 예정</span>
      </div>
      <p>{{ sectionDescriptions[selectedSection] }}</p>
      <p>공통 설비 유형 선택과 연결되며, 이후 목록·상세·등록·수정 기능을 이 영역에 추가합니다.</p>
    </section>
  </div>
</template>

<style scoped>
.predictive-page {
  --pm-danger: #b8403a;
  --pm-caution: #b47c1c;
  --pm-normal: #2f7d57;
}

.section-tabs {
  display: flex;
  min-width: 0;
  overflow-x: auto;
  background: #ffffff;
  border: 1px solid var(--line);
}

.section-tabs button {
  min-width: 100px;
  min-height: 36px;
  color: #334a60;
  background: #f4f7fa;
  border: 0;
  border-right: 1px solid var(--line);
  font-weight: 700;
}

.section-tabs button.active,
.section-tabs button:hover {
  color: #ffffff;
  background: #28496d;
}

.intro-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #ffffff, #e9f1f7);
}

.intro-panel h2,
.intro-panel p {
  margin: 0;
}

.intro-panel h2 {
  margin-top: 3px;
  color: #183b5d;
  font-size: 21px;
}

.intro-panel p {
  margin-top: 4px;
  color: var(--ink-500);
}

.eyebrow {
  color: var(--blue-700);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.data-basis {
  min-width: 220px;
  padding: 8px 10px;
  background: #ffffff;
  border: 1px solid #aebdca;
}

.data-basis span,
.data-basis strong {
  display: block;
}

.data-basis span {
  color: var(--ink-500);
  font-size: 11px;
}

.data-basis strong {
  margin-top: 3px;
  color: #29445f;
}

.dashboard-panels .panel {
  min-height: 160px;
}

.empty-state {
  display: grid;
  min-height: 95px;
  place-items: center;
  padding: 20px;
  color: var(--ink-500);
  text-align: center;
  background: #f5f8fb;
  border: 1px dashed #aebdca;
}

.feature-placeholder {
  min-height: 300px;
}

.feature-placeholder p {
  color: var(--ink-500);
}

@media (max-width: 700px) {
  .intro-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .data-basis {
    min-width: 0;
  }
}
</style>
