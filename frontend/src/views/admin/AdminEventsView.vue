<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useVehicleStore } from '@/stores/vehicleStore'

const gateLogStore = useGateLogStore()
const notificationStore = useNotificationStore()
const vehicleStore = useVehicleStore()
const { notifications } = storeToRefs(notificationStore)
const exceptionFilter = ref('ALL')

const getValue = (item, camelCaseKey, snakeCaseKey) => item?.[camelCaseKey] ?? item?.[snakeCaseKey]

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR')
}

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => (item.vehicleId || item.vehicle_id) === vehicleId)
  return vehicle?.plateNumber || vehicle?.plate_number || vehicleId || '-'
}

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

onMounted(() => {
  gateLogStore.loadGateLogs()
  notificationStore.loadNotifications()
  vehicleStore.loadVehicles()
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
              <b>{{ event.type }} / {{ event.target }}</b>
              <span>{{ event.message }}</span>
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
            </div>
          </div>
          <div v-if="exceptions.length === 0" class="timeline-row alert">
            <time>-</time>
            <div><b>표시할 예외 처리 내역이 없습니다.</b><span>-</span></div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
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
</style>
