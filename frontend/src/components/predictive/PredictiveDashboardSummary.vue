<script setup>
import { computed, ref, watch } from 'vue'
import { fetchPredictiveEvents } from '@/api/predictiveMaintenanceApi'
import {
  DEMO_EQUIPMENT_ID,
  predictiveDemoSession,
} from '@/config/predictiveDemoSession'

defineEmits(['open-maintenance'])

const events = ref([])
const loading = ref(false)
const loadError = ref('')

const eventLabels = {
  FAILURE_EXPECTED: '고장 예상',
  FAILURE: '실제 고장',
  MAINTENANCE_COMPLETED: '수리 완료',
  RECOVERY: '회복',
}

const eventClasses = {
  FAILURE_EXPECTED: 'caution',
  FAILURE: 'danger',
  MAINTENANCE_COMPLETED: 'normal',
  RECOVERY: 'normal',
}

const currentEquipment = computed(() => predictiveDemoSession.selectedEquipmentId)
const observationTime = computed(() => predictiveDemoSession.observationTime)
const demoEvents = computed(() => [
  {
    eventId: 'demo-alert',
    eventType: 'FAILURE_EXPECTED',
    occurredAt: predictiveDemoSession.alertAt,
    anomalyCount: predictiveDemoSession.anomalyCountAtAlert,
    notificationStatus: predictiveDemoSession.notificationRequests
      .some((item) => item.eventType === 'FAILURE_EXPECTED') ? 'REQUESTED' : 'PENDING',
  },
  {
    eventId: 'demo-failure',
    eventType: 'FAILURE',
    occurredAt: predictiveDemoSession.failureAt,
    anomalyCount: predictiveDemoSession.anomalyCountAtFailure,
    notificationStatus: predictiveDemoSession.notificationRequests
      .some((item) => item.eventType === 'FAILURE') ? 'REQUESTED' : 'PENDING',
  },
  {
    eventId: 'demo-maintenance',
    eventType: 'MAINTENANCE_COMPLETED',
    occurredAt: predictiveDemoSession.maintenanceAt,
    anomalyCount: 0,
    notificationStatus: 'NOT_REQUESTED',
  },
].filter((event) => event.occurredAt))

const visibleEvents = computed(() => {
  const source = currentEquipment.value === DEMO_EQUIPMENT_ID ? demoEvents.value : events.value
  return source
    .filter((event) => new Date(event.occurredAt).getTime() <= observationTime.value)
    .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())
    .slice(0, 3)
})

const formatDateTime = (value) => {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

const notificationText = (status) => {
  if (status === 'SENT') return '카카오 발송 완료'
  if (status === 'REQUESTED') return '카카오 요청 생성'
  if (status === 'FAILED') return '카카오 발송 실패'
  if (status === 'DEMO_NOT_SENT') return '과거 기록 · 미발송'
  return ''
}

const loadEvents = async (equipmentId) => {
  events.value = []
  loadError.value = ''
  if (!equipmentId || equipmentId === DEMO_EQUIPMENT_ID) return
  loading.value = true
  try {
    events.value = await fetchPredictiveEvents(equipmentId)
  } catch (error) {
    loadError.value = error.message
  } finally {
    loading.value = false
  }
}

watch(currentEquipment, loadEvents, { immediate: true })
</script>

<template>
  <section class="dashboard-insights">
    <article class="panel history-panel">
      <div class="section-title">
        <h2>최근 고장·정비 이력</h2>
        <button type="button" class="history-link" @click="$emit('open-maintenance')">전체 기록 보기</button>
      </div>

      <div v-if="loading" class="history-empty">기록을 불러오는 중입니다.</div>
      <div v-else-if="loadError" class="history-empty error">{{ loadError }}</div>
      <ol v-else-if="visibleEvents.length" class="event-list">
        <li v-for="event in visibleEvents" :key="event.eventId">
          <span class="event-dot" :class="eventClasses[event.eventType]"></span>
          <div>
            <strong>{{ eventLabels[event.eventType] || event.eventType }}</strong>
            <small>{{ formatDateTime(event.occurredAt) }} · 이상 센서 {{ event.anomalyCount ?? 0 }}개</small>
          </div>
          <span class="notification-state">{{ notificationText(event.notificationStatus) }}</span>
        </li>
      </ol>
      <div v-else class="history-empty">현재 관찰 시점까지 발생한 고장·정비 기록이 없습니다.</div>
    </article>
  </section>
</template>

<style scoped>
.history-panel { min-height: 150px; }
.history-link { padding: 4px 8px; color: #285f8f; background: #fff; border: 1px solid #aebdca; font-size: 11px; font-weight: 700; }
.event-list { display: grid; gap: 0; margin: 0; padding: 0; list-style: none; }
.event-list li { display: grid; grid-template-columns: 10px 1fr auto; align-items: center; gap: 9px; min-height: 38px; border-bottom: 1px solid #e0e6ec; }
.event-list li:last-child { border-bottom: 0; }
.event-list div { display: grid; gap: 2px; }
.event-list strong { color: #29445f; font-size: 12px; }
.event-list small, .notification-state { color: var(--ink-500); font-size: 10px; }
.event-dot { width: 8px; height: 8px; border-radius: 50%; background: #8292a2; }
.event-dot.caution { background: #b47c1c; }
.event-dot.danger { background: #b8403a; }
.event-dot.normal { background: #2f7d57; }
.history-empty { display: grid; min-height: 88px; place-items: center; color: var(--ink-500); background: #f6f8fa; border: 1px dashed #bdc9d5; font-size: 12px; }
.history-empty.error { color: #b8403a; }
@media (max-width: 800px) {
  .event-list li { grid-template-columns: 10px 1fr; }
  .notification-state { grid-column: 2; }
}
</style>
