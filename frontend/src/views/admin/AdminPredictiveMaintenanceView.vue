<script setup>
import { computed, onMounted, ref } from 'vue'
import AntennaHistoryPlayback from '@/components/predictive/AntennaHistoryPlayback.vue'
import MaintenanceRecordBoard from '@/components/predictive/MaintenanceRecordBoard.vue'
import PredictiveDashboardSummary from '@/components/predictive/PredictiveDashboardSummary.vue'
import PredictiveTypeSelector from '@/components/predictive/PredictiveTypeSelector.vue'
import {
  predictiveDemoSession,
  clearKakaoRuntime,
  configureKakaoRuntime,
  fetchKakaoRuntimeStatus,
  setKakaoNotificationsEnabled,
} from '@/config/predictiveDemoSession'
import {
  predictiveMaintenanceSections,
  predictiveMaintenanceTypes,
} from '@/config/predictiveMaintenance'

const selectedType = ref('GATE_EQUIPMENT')
const selectedSection = ref('dashboard')
const kakaoAccessToken = ref('')
const kakaoConfigured = ref(false)
const kakaoBusy = ref(false)
const kakaoMessage = ref('연결 상태를 확인하는 중입니다.')

const refreshKakaoStatus = async () => {
  try {
    const status = await fetchKakaoRuntimeStatus()
    kakaoConfigured.value = status.configured
    kakaoMessage.value = status.message
    if (!status.configured) setKakaoNotificationsEnabled(false)
  } catch (error) {
    kakaoMessage.value = error.message
  }
}

const connectKakao = async () => {
  if (!kakaoAccessToken.value.trim() || kakaoBusy.value) return
  kakaoBusy.value = true
  try {
    const result = await configureKakaoRuntime(kakaoAccessToken.value)
    kakaoAccessToken.value = ''
    kakaoConfigured.value = true
    kakaoMessage.value = result.message
    setKakaoNotificationsEnabled(true)
  } catch (error) {
    kakaoMessage.value = error.message
  } finally {
    kakaoBusy.value = false
  }
}

const disconnectKakao = async () => {
  if (kakaoBusy.value) return
  kakaoBusy.value = true
  try {
    const result = await clearKakaoRuntime()
    kakaoConfigured.value = false
    kakaoMessage.value = result.message
    setKakaoNotificationsEnabled(false)
  } catch (error) {
    kakaoMessage.value = error.message
  } finally {
    kakaoBusy.value = false
  }
}

const setKakaoEnabled = (enabled) => {
  if (enabled && !kakaoConfigured.value) {
    kakaoMessage.value = '아래에 본인 액세스 토큰을 먼저 연결하세요.'
    return
  }
  setKakaoNotificationsEnabled(enabled)
}

onMounted(refreshKakaoStatus)

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
        <div class="intro-actions">
          <div class="kakao-setting">
            <span>카카오 알림 연동</span>
            <div class="kakao-options" role="group" aria-label="카카오 알림 연동 설정">
              <button
                type="button"
                :class="{ active: !predictiveDemoSession.kakaoNotificationsEnabled }"
                :aria-pressed="!predictiveDemoSession.kakaoNotificationsEnabled"
                @click="setKakaoEnabled(false)"
              >
                사용 안 함
              </button>
              <button
                type="button"
                class="enable"
                :class="{ active: predictiveDemoSession.kakaoNotificationsEnabled }"
                :aria-pressed="predictiveDemoSession.kakaoNotificationsEnabled"
                @click="setKakaoEnabled(true)"
              >
                사용
              </button>
            </div>
            <small>
              {{ predictiveDemoSession.kakaoNotificationsEnabled
                ? '시연용 고장 예상·고장 시 알림을 보냅니다.'
                : '알림 API를 호출하지 않습니다.' }}
            </small>
            <form v-if="!kakaoConfigured" class="kakao-connect" @submit.prevent="connectKakao">
              <input
                v-model="kakaoAccessToken"
                type="password"
                autocomplete="off"
                placeholder="카카오 액세스 토큰"
                aria-label="카카오 액세스 토큰"
              />
              <button type="submit" :disabled="kakaoBusy || !kakaoAccessToken.trim()">
                이번 실행에 연결
              </button>
            </form>
            <button v-else type="button" class="kakao-disconnect" :disabled="kakaoBusy" @click="disconnectKakao">
              연결 해제
            </button>
            <small class="kakao-status">{{ kakaoMessage }}</small>
          </div>
          <div class="data-basis">
            <span>현재 데이터 기준</span>
            <strong>게이트 설비 상태 기록 재생</strong>
          </div>
        </div>
      </section>

      <AntennaHistoryPlayback v-if="selectedType === 'GATE_EQUIPMENT'" />

      <PredictiveDashboardSummary @open-maintenance="selectedSection = 'maintenance'" />
    </template>

    <MaintenanceRecordBoard
      v-else-if="selectedSection === 'maintenance' && selectedType === 'GATE_EQUIPMENT'"
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

.intro-actions {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.kakao-setting {
  min-width: 250px;
  padding: 7px 9px;
  background: #ffffff;
  border: 1px solid #aebdca;
}

.kakao-setting > span,
.kakao-setting small {
  display: block;
  color: var(--ink-500);
  font-size: 10px;
}

.kakao-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 4px;
}

.kakao-options button {
  min-height: 28px;
  color: #40566b;
  background: #f4f7fa;
  border: 1px solid #aebdca;
  font-size: 11px;
  font-weight: 800;
}

.kakao-options button + button {
  border-left: 0;
}

.kakao-options button.active {
  color: #ffffff;
  background: #596b7b;
}

.kakao-options button.enable.active {
  background: #2f7d57;
}

.kakao-setting small {
  margin-top: 4px;
}

.kakao-connect {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) auto;
  gap: 4px;
  margin-top: 6px;
}

.kakao-connect input,
.kakao-connect button,
.kakao-disconnect {
  min-height: 28px;
  border: 1px solid #aebdca;
  font-size: 11px;
}

.kakao-connect input {
  min-width: 0;
  padding: 0 7px;
}

.kakao-connect button,
.kakao-disconnect {
  color: #ffffff;
  background: #28496d;
  font-weight: 800;
}

.kakao-disconnect {
  width: 100%;
  margin-top: 6px;
  background: #596b7b;
}

.kakao-status {
  color: #755713 !important;
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

  .intro-actions {
    flex-direction: column;
  }

  .kakao-setting {
    min-width: 0;
  }
}
</style>
