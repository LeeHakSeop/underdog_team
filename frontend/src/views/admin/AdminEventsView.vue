<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useVehicleStore } from '@/stores/vehicleStore'

const gateLogStore = useGateLogStore()
const notificationStore = useNotificationStore()
const vehicleStore = useVehicleStore()
const { notifications, loading, error } = storeToRefs(notificationStore)

const eventTypeText = {
  GATE: '게이트',
  EXCEPTION: '예외 발생',
  PROCESSED: '처리 완료',
}

const getValue = (item, ...keys) => {
  for (const key of keys) {
    const value = item?.[key]

    if (value !== undefined && value !== null && value !== '') {
      return value
    }
  }

  return ''
}

const formatDateTime = (value) => {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => (item.vehicleId || item.vehicle_id) === vehicleId)
  return vehicle?.plateNumber || vehicle?.plate_number || vehicleId || '-'
}

const events = computed(() => {
  return gateLogStore.gateLogs.map((log) => ({
    key: log.gateLogId || log.gate_log_id,
    time: formatDateTime(log.entryTime || log.entry_time || log.exitTime || log.exit_time),
    title: `${log.inOutType || log.in_out_type || '-'} / ${getPlateNumber(log.vehicleId || log.vehicle_id)}`,
    message: `${log.gateName || log.gate_name || '-'} / ${log.processResult || log.process_result || '-'}`,
  }))
})

const buildFallbackTimeline = (item) => [{
  key: 'exception-fallback',
  type: 'EXCEPTION',
  label: eventTypeText.EXCEPTION,
  time: formatDateTime(getValue(item, 'occurredTime', 'occurred_time')),
  description: getValue(item, 'exceptionMessage', 'exception_message') || '-',
}]

const normalizeTimeline = (item) => {
  const timeline = getValue(item, 'timeline') || []

  if (!Array.isArray(timeline) || timeline.length === 0) {
    return buildFallbackTimeline(item)
  }

  return timeline.map((event, index) => {
    const eventType = getValue(event, 'eventType', 'event_type')

    return {
      key: `${eventType || 'event'}-${index}`,
      type: eventType,
      label: eventTypeText[eventType] || getValue(event, 'eventLabel', 'event_label') || '기록',
      time: formatDateTime(getValue(event, 'eventTime', 'event_time')),
      description: getValue(event, 'description') || getValue(event, 'eventLabel', 'event_label') || '-',
    }
  })
}

const exceptions = computed(() => {
  return notifications.value.map((item, index) => {
    const exceptionLogId = getValue(item, 'exceptionLogId', 'exception_log_id', 'id') || index
    const plateNumber = getValue(item, 'plateNumber', 'plate_number') || '-'
    const exceptionType = getValue(item, 'exceptionType', 'exception_type') || '예외'

    return {
      key: exceptionLogId,
      status: getValue(item, 'processStatus', 'process_status') || '미처리',
      title: `${exceptionType} / ${plateNumber}`,
      timeline: normalizeTimeline(item),
    }
  })
})

const exceptionTimelineRows = computed(() => {
  return exceptions.value.flatMap((item) => {
    return item.timeline.map((event) => ({
      key: `${item.key}-${event.key}`,
      time: event.time,
      title: `${event.label} / ${item.title}`,
      message: event.description,
      status: item.status,
      type: event.type,
    }))
  })
})

onMounted(() => {
  gateLogStore.loadGateLogs().catch(() => {})
  notificationStore.loadNotifications().catch(() => {})
  vehicleStore.loadVehicles().catch(() => {})
})
</script>

<template>
  <div class="page-stack">
    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>게이트 처리 이력</h2>
          <span class="status-pill">{{ events.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="event in events" :key="event.key" class="timeline-row">
            <time>{{ event.time }}</time>
            <div>
              <b>{{ event.title }}</b>
              <span>{{ event.message }}</span>
            </div>
          </div>
          <div v-if="events.length === 0" class="timeline-row">
            <time>-</time>
            <div>
              <b>게이트 처리 이력이 없습니다.</b>
              <span>-</span>
            </div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>예외 처리 기록</h2>
          <span class="status-pill red">{{ exceptions.length }}건</span>
        </div>
        <div class="timeline">
          <div v-if="loading" class="timeline-row alert">
            <time>-</time>
            <div>
              <b>예외 처리 기록을 불러오는 중입니다.</b>
              <span>-</span>
            </div>
          </div>

          <div
            v-for="event in exceptionTimelineRows"
            :key="event.key"
            :class="['timeline-row', 'alert', event.type?.toLowerCase()]"
          >
            <time>{{ event.time }}</time>
            <div>
              <div class="timeline-heading">
                <b>{{ event.title }}</b>
                <span class="status-pill red">{{ event.status }}</span>
              </div>
              <span>{{ event.message }}</span>
            </div>
          </div>

          <div v-if="!loading && exceptionTimelineRows.length === 0" class="timeline-row alert">
            <time>-</time>
            <div>
              <b>예외 처리 기록이 없습니다.</b>
              <span>{{ error || '-' }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.timeline {
  display: grid;
  gap: 10px;
}

.timeline-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.timeline-row.alert {
  background: #fff8f5;
  border-color: #f0cec5;
}

.timeline-row.alert.gate {
  background: #f6f9fd;
  border-color: #c7d6e5;
}

.timeline-row.alert.processed {
  background: #f5fbf7;
  border-color: #bddfc8;
}

.timeline-row time {
  color: var(--blue-700);
  font-weight: 900;
  word-break: keep-all;
}

.timeline-row b,
.timeline-row span {
  display: block;
}

.timeline-row span {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.timeline-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.timeline-heading .status-pill {
  flex: 0 0 auto;
}

@media (max-width: 760px) {
  .timeline-row {
    grid-template-columns: 1fr;
  }

  .timeline-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
