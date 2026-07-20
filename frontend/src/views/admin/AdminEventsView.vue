<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { displayTone, inOutTypeLabel, processResultLabel } from '@/config/displayLabels'

const gateLogStore = useGateLogStore()
const notificationStore = useNotificationStore()
const vehicleStore = useVehicleStore()
<<<<<<< HEAD
const { notifications } = storeToRefs(notificationStore)
const exceptionFilter = ref('ALL')

const getValue = (item, camelCaseKey, snakeCaseKey) => item?.[camelCaseKey] ?? item?.[snakeCaseKey]

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR')
=======
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
>>>>>>> origin/pjh
}

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => (item.vehicleId || item.vehicle_id) === vehicleId)
  return vehicle?.plateNumber || vehicle?.plate_number || vehicleId || '-'
}

<<<<<<< HEAD
const events = computed(() => gateLogStore.gateLogs.map((log) => ({
  key: getValue(log, 'gateLogId', 'gate_log_id'),
  time: formatDateTime(getValue(log, 'entryTime', 'entry_time') || getValue(log, 'exitTime', 'exit_time')),
  type: getValue(log, 'inOutType', 'in_out_type') || '-',
  target: getPlateNumber(getValue(log, 'vehicleId', 'vehicle_id')),
  message: `${getValue(log, 'gateName', 'gate_name') || '-'} / ${getValue(log, 'processResult', 'process_result') || '-'}`,
})))

const exceptionItems = computed(() => notifications.value.map((item) => ({
  ...item,
  key: getValue(item, 'exceptionLogId', 'exception_log_id')
    || `${getValue(item, 'exceptionType', 'exception_type')}-${getValue(item, 'occurredTime', 'occurred_time')}`,
  occurredTime: formatDateTime(getValue(item, 'occurredTime', 'occurred_time')),
  processedTime: formatDateTime(getValue(item, 'processedTime', 'processed_time')),
  status: getValue(item, 'processStatus', 'process_status') || 'UNPROCESSED',
  type: getValue(item, 'exceptionType', 'exception_type') || '-',
  plateNumber: getValue(item, 'plateNumber', 'plate_number')
    || getPlateNumber(getValue(item, 'vehicleId', 'vehicle_id')),
  message: getValue(item, 'exceptionMessage', 'exception_message') || '-',
  managerAction: getValue(item, 'managerAction', 'manager_action') || '조치 대기',
})))

const exceptions = computed(() => exceptionItems.value.filter((item) => (
  exceptionFilter.value === 'ALL' || item.status === exceptionFilter.value
)))

const openExceptionCount = computed(() => exceptionItems.value.filter((item) => item.status !== 'PROCESSED').length)
=======
const getLogVehiclePair = (log) => {
  const tractorVehicleId = log.tractorVehicleId || log.tractor_vehicle_id
  const trailerVehicleId = log.trailerVehicleId || log.trailer_vehicle_id
  const fallbackVehicleId = log.vehicleId || log.vehicle_id

  return {
    tractor: getPlateNumber(tractorVehicleId || fallbackVehicleId),
    trailer: getPlateNumber(trailerVehicleId || fallbackVehicleId),
  }
}

const events = computed(() => {
  return gateLogStore.gateLogs.map((log) => {
    const vehicles = getLogVehiclePair(log)

    return {
      key: log.gateLogId || log.gate_log_id,
      time: formatDateTime(log.entryTime || log.entry_time || log.exitTime || log.exit_time),
      inOutType: log.inOutType || log.in_out_type || '',
      tractorPlateNumber: vehicles.tractor,
      trailerPlateNumber: vehicles.trailer,
      gateName: log.gateName || log.gate_name || '-',
      processResult: log.processResult || log.process_result || '',
    }
  })
})

const getInOutClass = (value) => (value === 'OUT' ? 'red' : 'blue')

const getProcessClass = (value) => displayTone('process', value)

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
>>>>>>> origin/pjh

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
              <div class="gate-event-head">
                <span class="status-pill" :class="getInOutClass(event.inOutType)">
                  {{ inOutTypeLabel(event.inOutType) }}
                </span>
                <b>트랙터 {{ event.tractorPlateNumber }}</b>
                <b>트레일러 {{ event.trailerPlateNumber }}</b>
                <span class="status-pill" :class="getProcessClass(event.processResult)">
                  {{ processResultLabel(event.processResult) }}
                </span>
              </div>
              <span>{{ event.gateName }}</span>
            </div>
          </div>
          <div v-if="events.length === 0" class="timeline-row">
            <time>-</time>
            <div><b>게이트 처리 이력이 없습니다.</b><span>-</span></div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <div>
            <h2>예외 처리 내역</h2>
            <p class="section-hint">미처리 {{ openExceptionCount }}건 / 전체 {{ exceptionItems.length }}건</p>
          </div>
          <select v-model="exceptionFilter" class="status-filter" aria-label="예외 처리 상태 필터">
            <option value="ALL">전체</option>
            <option value="UNPROCESSED">미처리</option>
            <option value="PROCESSED">처리 완료</option>
          </select>
        </div>
        <div class="timeline">
<<<<<<< HEAD
          <div v-for="item in exceptions" :key="item.key" class="timeline-row alert">
            <div class="exception-times">
              <time>발생 {{ item.occurredTime }}</time>
              <time v-if="item.processedTime !== '-'">처리 {{ item.processedTime }}</time>
            </div>
            <div>
              <div class="exception-title">
                <b>{{ item.type }} / {{ item.plateNumber }}</b>
                <span class="status-pill" :class="item.status === 'PROCESSED' ? 'green' : 'red'">{{ item.status }}</span>
              </div>
              <span>{{ item.message }}</span>
              <small>관리자 조치: {{ item.managerAction }}</small>
=======
          <div v-if="loading" class="timeline-row alert">
            <time>-</time>
            <div>
              <b>예외 처리 기록을 불러오는 중입니다.</b>
              <span>-</span>
>>>>>>> origin/pjh
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
<<<<<<< HEAD
            <div><b>표시할 예외 처리 내역이 없습니다.</b><span>-</span></div>
=======
            <div>
              <b>예외 처리 기록이 없습니다.</b>
              <span>{{ error || '-' }}</span>
            </div>
>>>>>>> origin/pjh
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
<<<<<<< HEAD
.timeline { display: grid; gap: 10px; }
.timeline-row { display: grid; grid-template-columns: 160px 1fr; gap: 12px; padding: 12px; background: #f6f9fd; border: 1px solid var(--line); border-radius: 8px; }
.timeline-row.alert { background: #fff8f5; border-color: #f0cec5; }
.timeline-row time { color: var(--blue-700); font-weight: 900; }
.exception-times { display: grid; align-content: start; gap: 4px; }
.exception-times time + time { color: var(--green-600); }
.timeline-row b, .timeline-row span, .timeline-row small { display: block; }
.timeline-row span { margin-top: 4px; color: var(--ink-500); font-size: 13px; font-weight: 700; }
.timeline-row small { margin-top: 8px; color: var(--ink-500); font-weight: 700; }
.section-hint { margin: 4px 0 0; color: var(--ink-500); font-size: 12px; font-weight: 700; }
.status-filter { min-width: 110px; padding: 8px 10px; border: 1px solid var(--line); border-radius: 6px; background: #fff; color: var(--ink-700); font-weight: 700; }
.exception-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.exception-title .status-pill { margin-top: 0; flex: 0 0 auto; }
@media (max-width: 620px) { .timeline-row { grid-template-columns: 1fr; } }
=======
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

.gate-event-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.gate-event-head b {
  color: var(--ink-900);
  font-size: 15px;
  font-weight: 900;
}

.gate-event-head .status-pill {
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
>>>>>>> origin/pjh
</style>
