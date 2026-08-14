<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  fetchPredictiveEquipment,
  fetchPredictiveEvents,
} from '@/api/predictiveMaintenanceApi'
import {
  DEMO_EQUIPMENT_ID,
  predictiveDemoSession,
} from '@/config/predictiveDemoSession'

const historicalRecords = ref([])
const equipmentOptions = ref([])
const loading = ref(true)
const loadError = ref('')
const selectedEquipment = ref(predictiveDemoSession.selectedEquipmentId || DEMO_EQUIPMENT_ID)
const selectedRecordId = ref('')
const viewMode = ref('equipment')

const demoRecord = computed(() => ({
  id: DEMO_EQUIPMENT_ID,
  equipmentId: DEMO_EQUIPMENT_ID,
  isDemo: true,
  alertAt: predictiveDemoSession.alertAt,
  alertAnomalyCount: predictiveDemoSession.anomalyCountAtAlert,
  failureAt: predictiveDemoSession.failureAt,
  failureAnomalyCount: predictiveDemoSession.anomalyCountAtFailure,
  maintenanceAt: predictiveDemoSession.maintenanceAt,
  maintenanceState: predictiveDemoSession.maintenanceState,
}))
const records = computed(() => [demoRecord.value, ...historicalRecords.value])

const parseCollectedAt = (value) => {
  if (!value) return 0
  return new Date(value.includes('T') ? value : value.replace(' ', 'T')).getTime()
}

const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(timestamp))
}

const filteredRecords = computed(() => {
  if (selectedEquipment.value === 'ALL') return records.value
  return records.value.filter((record) => record.equipmentId === selectedEquipment.value)
})

const timelineEvents = computed(() =>
  filteredRecords.value
    .flatMap((record) => {
      const events = []
      if (record.alertAt && eventReached(record, record.alertAt)) {
        events.push({
          id: `${record.id}-alert`,
          recordId: record.id,
          equipmentId: record.equipmentId,
          isDemo: record.isDemo,
          type: 'alert',
          label: '고장 예상',
          timestamp: record.alertAt,
          note: record.isDemo ? '카카오 알림 요청 생성' : `이상 센서 ${record.alertAnomalyCount}개`,
        })
      }
      if (record.failureAt && eventReached(record, record.failureAt)) {
        events.push({
          id: `${record.id}-failure`,
          recordId: record.id,
          equipmentId: record.equipmentId,
          isDemo: record.isDemo,
          type: 'failure',
          label: '실제 고장',
          timestamp: record.failureAt,
          note: record.isDemo ? '카카오 알림 요청 생성' : `이상 센서 ${record.failureAnomalyCount}개`,
        })
      }
      if (record.maintenanceAt && eventReached(record, record.maintenanceAt)) {
        events.push({
          id: `${record.id}-maintenance`,
          recordId: record.id,
          equipmentId: record.equipmentId,
          isDemo: record.isDemo,
          type: 'maintenance',
          label: '수리 완료',
          timestamp: record.maintenanceAt,
          note: `운영 상태 ${record.maintenanceState}`,
        })
      }
      return events
    })
    .sort((a, b) => b.timestamp - a.timestamp),
)

const selectedRecord = computed(() =>
  records.value.find((record) => record.id === selectedRecordId.value),
)

const alertedRecordCount = computed(() => records.value.filter((record) => record.alertAt).length)
const suddenFailureCount = computed(() => records.value.filter((record) => !record.alertAt).length)
const eventReached = (record, timestamp) => !record.isDemo || predictiveDemoSession.observationTime >= timestamp
const notificationRequested = (eventType) =>
  predictiveDemoSession.notificationRequests.some((item) => item.eventType === eventType)
const demoStage = computed(() => {
  if (predictiveDemoSession.observationTime >= predictiveDemoSession.maintenanceAt) return '수리 완료'
  if (predictiveDemoSession.observationTime >= predictiveDemoSession.failureAt) return '고장 발생'
  if (predictiveDemoSession.observationTime >= predictiveDemoSession.alertAt) return '고장 예상'
  return '정상 관찰'
})

const openTimelineRecord = (event) => {
  selectedEquipment.value = event.equipmentId
  selectedRecordId.value = event.recordId
  viewMode.value = 'equipment'
}

const openAllRecords = () => {
  selectedEquipment.value = 'ALL'
  viewMode.value = 'timeline'
}

const buildMaintenanceRecords = (equipment, events) => {
  const result = []
  const equipmentIds = equipment.map((item) => item.equipmentCode).sort()
  equipmentOptions.value = [DEMO_EQUIPMENT_ID, ...equipmentIds]

  equipmentIds.forEach((equipmentId) => {
    const equipmentEvents = events
      .filter((event) => event.equipmentId === equipmentId)
      .sort((a, b) => parseCollectedAt(a.occurredAt) - parseCollectedAt(b.occurredAt))
    const maintenanceEvents = equipmentEvents.filter(
      (event) => event.eventType === 'MAINTENANCE_COMPLETED',
    )
    let previousMaintenanceAt = 0

    maintenanceEvents.forEach((maintenance) => {
      const maintenanceAt = parseCollectedAt(maintenance.occurredAt)
      const episodeEvents = equipmentEvents.filter((event) => {
        const timestamp = parseCollectedAt(event.occurredAt)
        return timestamp > previousMaintenanceAt && timestamp <= maintenanceAt
      })
      const failure = episodeEvents.filter((event) => event.eventType === 'FAILURE').at(-1)
      const alert = episodeEvents.filter((event) => event.eventType === 'FAILURE_EXPECTED').at(-1)
      const failureAt = failure ? parseCollectedAt(failure.occurredAt) : 0

      result.push({
        id: `${equipmentId}-${maintenance.occurredAt}`,
        equipmentId,
        alertAt: alert ? parseCollectedAt(alert.occurredAt) : 0,
        alertAnomalyCount: alert?.anomalyCount ?? 0,
        failureAt,
        maintenanceAt,
        failureAnomalyCount: failure?.anomalyCount ?? 0,
        maintenanceState: '정상',
      })
      previousMaintenanceAt = maintenanceAt
    })
  })

  historicalRecords.value = result.sort((a, b) => b.maintenanceAt - a.maintenanceAt)
  selectedRecordId.value = filteredRecords.value[0]?.id || ''
}

watch(filteredRecords, (items) => {
  if (!items.some((item) => item.id === selectedRecordId.value)) {
    selectedRecordId.value = items[0]?.id || ''
  }
})

watch(selectedEquipment, (equipmentId) => {
  if (equipmentId !== 'ALL') predictiveDemoSession.selectedEquipmentId = equipmentId
})

watch(selectedRecordId, (recordId) => {
  const record = records.value.find((item) => item.id === recordId)
  if (record) predictiveDemoSession.selectedEquipmentId = record.equipmentId
})

onMounted(async () => {
  try {
    const [equipment, events] = await Promise.all([
      fetchPredictiveEquipment(),
      fetchPredictiveEvents(),
    ])
    buildMaintenanceRecords(equipment, events)
  } catch (error) {
    loadError.value = error.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="maintenance-board">
    <div class="board-header">
      <div>
        <span class="eyebrow">FAILURE & MAINTENANCE HISTORY</span>
        <h2>안테나 고장·정비 기록</h2>
        <p>고장 예상 알림부터 실제 고장과 수리 완료까지의 사건을 시간순으로 확인합니다.</p>
      </div>
      <span class="prototype-note">시연용 안테나는 그래프 재생 시점과 연동</span>
    </div>

    <div v-if="loading" class="message-state">고장·정비 기록을 불러오는 중입니다.</div>
    <div v-else-if="loadError" class="message-state error">{{ loadError }}</div>

    <template v-else>
      <div class="record-summary">
        <div><span>전체 수리 기록</span><strong>{{ records.length }}건</strong></div>
        <div><span>사전 알림 대상</span><strong>{{ alertedRecordCount }}건</strong></div>
        <div><span>급작 고장</span><strong>{{ suddenFailureCount }}건</strong></div>
        <label>
          <span>안테나별 보기</span>
          <select v-model="selectedEquipment" :disabled="viewMode === 'timeline'">
            <option value="ALL">전체 안테나</option>
            <option v-for="equipment in equipmentOptions" :key="equipment" :value="equipment">
              {{ equipment === DEMO_EQUIPMENT_ID ? '시연용 안테나' : equipment }}
            </option>
          </select>
        </label>
      </div>

      <div class="record-view-tabs" role="tablist" aria-label="고장·정비 기록 보기 방식">
        <button
          type="button"
          :class="{ active: viewMode === 'equipment' }"
          @click="viewMode = 'equipment'"
        >
          안테나별 기록
        </button>
        <button
          type="button"
          :class="{ active: viewMode === 'timeline' }"
          @click="openAllRecords"
        >
          모든 안테나 기록
        </button>
      </div>

      <div v-if="viewMode === 'equipment'" class="record-layout">
        <aside class="record-list" aria-label="고장·정비 기록 목록">
          <div class="list-heading">
            <strong>고장·정비 기록</strong>
            <span>{{ filteredRecords.length }}건</span>
          </div>
          <button
            v-for="record in filteredRecords"
            :key="record.id"
            type="button"
            class="record-item"
            :class="{ active: selectedRecordId === record.id }"
            @click="selectedRecordId = record.id"
          >
            <span class="record-item-top">
              <strong>{{ record.isDemo ? '시연용 안테나' : record.equipmentId }}</strong>
              <span v-if="record.isDemo" class="status-pill blue">{{ demoStage }}</span>
              <span v-else class="status-pill green">수리 완료</span>
            </span>
            <span v-if="record.isDemo">관찰 {{ formatDateTime(predictiveDemoSession.observationTime) }}</span>
            <span v-else>{{ formatDateTime(record.maintenanceAt) }}</span>
            <small v-if="record.isDemo">ANT-018 고장 사례 실시간 재생</small>
            <small v-else-if="record.alertAt">사전 알림 {{ formatDateTime(record.alertAt) }}</small>
            <small v-else class="sudden-text">사전 알림 없음 · 급작 고장</small>
          </button>
          <div v-if="!filteredRecords.length" class="empty-list">
            선택한 안테나에는 수리 기록이 없습니다.
          </div>
        </aside>

        <article v-if="selectedRecord" class="record-detail panel">
          <div class="detail-title">
            <div>
              <span>{{ selectedRecord.isDemo ? '시연용 안테나' : selectedRecord.equipmentId }}</span>
              <h3>고장·수리 이력</h3>
            </div>
            <span v-if="selectedRecord.isDemo" class="status-pill blue">{{ demoStage }}</span>
            <span v-else class="status-pill green">조치 완료</span>
          </div>

          <div class="event-flow">
            <section
              class="event-card alert"
              :class="{
                missing: !selectedRecord.alertAt,
                pending: !eventReached(selectedRecord, selectedRecord.alertAt),
              }"
            >
              <span class="step-number">1</span>
              <template v-if="eventReached(selectedRecord, selectedRecord.alertAt)">
                <span class="event-label">고장 예상</span>
                <strong>{{ formatDateTime(selectedRecord.alertAt) }}</strong>
                <small v-if="selectedRecord.isDemo">
                  {{ notificationRequested('FAILURE_EXPECTED') ? '카카오 알림 요청 생성' : '카카오 알림 예정' }}
                </small>
                <small v-else>과거 알림 시점 · 이상 센서 {{ selectedRecord.alertAnomalyCount }}개</small>
              </template>
              <template v-else-if="selectedRecord.isDemo">
                <span class="event-label">고장 예상</span>
                <strong>대기 중</strong>
              </template>
              <template v-else>
                <span class="event-label">고장 예상</span>
                <strong>사전 알림 없음</strong>
                <small class="sudden-text">전조 없이 급작 고장</small>
              </template>
            </section>

            <div class="event-arrow" aria-hidden="true">→</div>

            <section class="event-card failure" :class="{ pending: !eventReached(selectedRecord, selectedRecord.failureAt) }">
              <span class="step-number">2</span>
              <span class="event-label">실제 고장</span>
              <strong v-if="eventReached(selectedRecord, selectedRecord.failureAt)">{{ formatDateTime(selectedRecord.failureAt) }}</strong>
              <strong v-else>대기 중</strong>
              <small v-if="eventReached(selectedRecord, selectedRecord.failureAt)">
                <template v-if="selectedRecord.isDemo">
                  {{ notificationRequested('FAILURE') ? '카카오 알림 요청 생성' : '카카오 알림 예정' }}
                </template>
                <template v-else>과거 고장 시점 · 이상 센서 {{ selectedRecord.failureAnomalyCount }}개</template>
              </small>
            </section>

            <div class="event-arrow" aria-hidden="true">→</div>

            <section class="event-card repaired" :class="{ pending: !eventReached(selectedRecord, selectedRecord.maintenanceAt) }">
              <span class="step-number">3</span>
              <span class="event-label">수리 완료</span>
              <strong v-if="eventReached(selectedRecord, selectedRecord.maintenanceAt)">{{ formatDateTime(selectedRecord.maintenanceAt) }}</strong>
              <strong v-else>대기 중</strong>
              <small v-if="eventReached(selectedRecord, selectedRecord.maintenanceAt)">운영 상태 {{ selectedRecord.maintenanceState }}</small>
            </section>
          </div>
        </article>

        <div v-else class="record-detail-empty">
          왼쪽에서 확인할 고장·정비 기록을 선택하세요.
        </div>
      </div>

      <section v-else class="timeline-history" aria-label="모든 안테나 고장·정비 기록">
        <div class="timeline-heading">
          <div>
            <strong>모든 안테나 기록</strong>
            <span>전체 안테나의 고장 예상·실제 고장·수리 완료를 최근순으로 표시합니다.</span>
          </div>
          <strong>{{ timelineEvents.length }}건</strong>
        </div>

        <div v-if="timelineEvents.length" class="timeline-list">
          <button
            v-for="event in timelineEvents"
            :key="event.id"
            type="button"
            class="timeline-item"
            @click="openTimelineRecord(event)"
          >
            <time>{{ formatDateTime(event.timestamp) }}</time>
            <span class="timeline-type" :class="event.type">{{ event.label }}</span>
            <strong>{{ event.isDemo ? '시연용 안테나' : event.equipmentId }}</strong>
            <span class="timeline-note">{{ event.note }}</span>
            <span class="timeline-link">상세 보기</span>
          </button>
        </div>
        <div v-else class="empty-timeline">선택한 범위에 발생한 고장·정비 기록이 없습니다.</div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.maintenance-board { display: grid; gap: 10px; }
.board-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px; background: linear-gradient(135deg, #fff, #e9f1f7); border: 1px solid var(--line); }
.board-header h2, .board-header p { margin: 0; }
.board-header h2 { margin-top: 2px; color: #183b5d; font-size: 20px; }
.board-header p { margin-top: 4px; color: var(--ink-500); }
.eyebrow { color: var(--blue-700); font-size: 10px; font-weight: 700; letter-spacing: .08em; }
.prototype-note { max-width: 280px; padding: 7px 9px; color: #6b5317; background: #fff8e8; border: 1px solid #dec47d; font-size: 11px; }
.message-state, .record-detail-empty { display: grid; min-height: 260px; place-items: center; color: var(--ink-500); background: #f5f8fb; border: 1px dashed var(--line); }
.message-state.error { color: var(--red-500); }
.record-summary { display: grid; grid-template-columns: 150px 150px 150px minmax(220px, 1fr); border: 1px solid var(--line); }
.record-summary > div, .record-summary label { min-height: 64px; padding: 9px 10px; background: #fff; border-right: 1px solid #d4dde6; }
.record-summary span { display: block; color: var(--ink-500); font-size: 11px; font-weight: 700; }
.record-summary strong { display: block; margin-top: 4px; color: #20364f; font-size: 18px; }
.record-summary label { display: flex; align-items: center; justify-content: flex-end; gap: 8px; border-right: 0; }
.record-summary select { min-height: 34px; padding: 6px 8px; background: #fff; border: 1px solid #aeb9c5; }
.record-view-tabs { display: flex; gap: 4px; padding: 5px; background: #edf2f6; border: 1px solid var(--line); }
.record-view-tabs button { min-height: 34px; padding: 6px 14px; color: #29445f; background: #fff; border: 1px solid #aeb9c5; font-weight: 700; }
.record-view-tabs button.active { color: #fff; background: #244b70; border-color: #244b70; }
.record-view-tabs span { margin-left: 4px; font-size: 10px; opacity: .8; }
.record-layout { display: grid; grid-template-columns: 290px minmax(0, 1fr); gap: 10px; min-width: 0; }
.record-list { min-height: 520px; max-height: 720px; overflow-y: auto; background: #fff; border: 1px solid var(--line); }
.list-heading { position: sticky; top: 0; z-index: 1; display: flex; min-height: 40px; align-items: center; justify-content: space-between; padding: 8px 10px; color: #20364f; background: #e3eaf2; border-bottom: 1px solid var(--line); }
.record-item { display: grid; width: 100%; gap: 4px; padding: 10px; color: #29445f; text-align: left; background: #fff; border: 0; border-bottom: 1px solid #d4dde6; }
.record-item:hover, .record-item.active { background: #edf5fc; }
.record-item.active { box-shadow: inset 4px 0 #23639c; }
.record-item-top { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.record-item > span:not(.record-item-top), .record-item small { color: var(--ink-500); font-size: 11px; }
.record-item .sudden-text { color: #a23a35; font-weight: 700; }
.empty-list { padding: 24px 12px; color: var(--ink-500); text-align: center; }
.record-detail { min-width: 0; }
.detail-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin: -10px -10px 10px; padding: 9px 10px; background: #e3eaf2; border-bottom: 1px solid var(--line); }
.detail-title span:first-child { color: var(--blue-700); font-size: 11px; font-weight: 700; }
.detail-title h3 { margin: 1px 0 0; color: #20364f; font-size: 15px; }
.event-flow { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr); align-items: stretch; gap: 7px; }
.event-card { position: relative; display: grid; min-width: 0; min-height: 96px; align-content: center; gap: 3px; padding: 13px 14px 12px 44px; border: 1px solid; }
.event-card.alert { background: #fff9df; border-color: #e3c96f; }
.event-card.alert.missing { background: #f5f6f8; border-color: #b9c1c9; }
.event-card.failure { background: #fff4f3; border-color: #dfaaa6; }
.event-card.repaired { background: #f0f8f4; border-color: #a9d0bb; }
.event-card.pending { color: #7d8791; background: #f5f6f8; border-color: #c4cbd2; opacity: .68; }
.event-card.pending .step-number { background: #8b949d; }
.step-number { position: absolute; top: 50%; left: 12px; display: grid; width: 22px; height: 22px; place-items: center; color: #fff; background: #63778a; border-radius: 50%; font-size: 11px; font-weight: 700; transform: translateY(-50%); }
.event-card.alert .step-number { background: #b47c1c; }
.event-card.failure .step-number { background: #b8403a; }
.event-card.repaired .step-number { background: #2f7d57; }
.event-label { display: block; color: var(--ink-500); font-size: 11px; font-weight: 700; }
.event-card > strong { color: #20364f; font-size: 14px; }
.event-card > small { display: block; color: var(--ink-500); font-size: 10px; }
.event-card .sudden-text { color: #a23a35; font-weight: 700; }
.event-arrow { display: grid; place-items: center; color: #63778a; font-size: 22px; }
.timeline-history { overflow: hidden; background: #fff; border: 1px solid var(--line); }
.timeline-heading { display: flex; min-height: 48px; align-items: center; justify-content: space-between; gap: 16px; padding: 8px 12px; color: #20364f; background: #e3eaf2; border-bottom: 1px solid var(--line); }
.timeline-heading div { display: grid; gap: 2px; }
.timeline-heading span { color: var(--ink-500); font-size: 11px; font-weight: 400; }
.timeline-list { display: grid; max-height: 650px; overflow-y: auto; }
.timeline-item { display: grid; grid-template-columns: 170px 82px 130px minmax(180px, 1fr) 65px; min-height: 46px; align-items: center; gap: 10px; padding: 7px 12px; color: #29445f; text-align: left; background: #fff; border: 0; border-bottom: 1px solid #dbe2e9; }
.timeline-item:hover { background: #f2f7fb; }
.timeline-item time { color: #526579; font-size: 12px; font-weight: 700; }
.timeline-type { width: fit-content; padding: 3px 7px; border: 1px solid; font-size: 10px; font-weight: 700; }
.timeline-type.alert { color: #865a0f; background: #fff8dc; border-color: #dfc366; }
.timeline-type.failure { color: #a23a35; background: #fff0ef; border-color: #e1aaa6; }
.timeline-type.maintenance { color: #247050; background: #edf8f3; border-color: #a8d1bc; }
.timeline-note { color: var(--ink-500); font-size: 11px; }
.timeline-link { color: var(--blue-700); font-size: 11px; font-weight: 700; text-align: right; }
.empty-timeline { display: grid; min-height: 260px; place-items: center; color: var(--ink-500); }

@media (max-width: 1100px) {
  .event-flow { grid-template-columns: 1fr; }
  .event-arrow { transform: rotate(90deg); }
}
@media (max-width: 900px) {
  .board-header { align-items: stretch; flex-direction: column; }
  .record-layout { grid-template-columns: 1fr; }
  .record-list { min-height: 0; max-height: 280px; }
  .timeline-item { grid-template-columns: 150px 80px 1fr; }
  .timeline-note { grid-column: 2 / -1; }
  .timeline-link { display: none; }
}
@media (max-width: 700px) {
  .record-summary { grid-template-columns: 1fr; }
  .record-summary > div, .record-summary label { border-right: 0; border-bottom: 1px solid #d4dde6; }
  .record-summary label { align-items: stretch; flex-direction: column; }
  .record-view-tabs { display: grid; grid-template-columns: 1fr 1fr; }
  .timeline-item { grid-template-columns: 1fr auto; gap: 5px 8px; }
  .timeline-item time { grid-column: 1 / -1; }
  .timeline-note { grid-column: 1 / -1; }
}
</style>
