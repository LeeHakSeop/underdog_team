<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useVehicleStore } from '@/stores/vehicleStore'

const gateLogStore = useGateLogStore()
const notificationStore = useNotificationStore()
const vehicleStore = useVehicleStore()
const { notifications } = storeToRefs(notificationStore)

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => (item.vehicleId || item.vehicle_id) === vehicleId)
  return vehicle?.plateNumber || vehicle?.plate_number || vehicleId || '-'
}

const events = computed(() => {
  return gateLogStore.gateLogs.map((log) => ({
    key: log.gateLogId || log.gate_log_id,
    time: log.entryTime || log.entry_time || log.exitTime || log.exit_time || '-',
    type: log.inOutType || log.in_out_type || '-',
    target: getPlateNumber(log.vehicleId || log.vehicle_id),
    message: `${log.gateName || log.gate_name || '-'} / ${log.processResult || log.process_result || '-'}`,
  }))
})

const exceptions = computed(() => notifications.value)

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
          <div
            v-for="item in exceptions"
            :key="`${item.exceptionType || item.exception_type}-${item.plateNumber || item.plate_number}-${item.occurredTime || item.occurred_time}`"
            class="timeline-row alert"
          >
            <time>{{ item.processStatus || item.process_status || '-' }}</time>
            <div>
              <b>{{ item.exceptionType || item.exception_type }} / {{ item.plateNumber || item.plate_number }}</b>
              <span>{{ item.exceptionMessage || item.exception_message }}</span>
            </div>
          </div>
          <div v-if="exceptions.length === 0" class="timeline-row alert">
            <time>-</time>
            <div>
              <b>예외 처리 기록이 없습니다.</b>
              <span>-</span>
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
  grid-template-columns: 160px 1fr;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.timeline-row.alert {
  background: #fff8f5;
  border-color: #f0cec5;
}

.timeline-row time {
  color: var(--blue-700);
  font-weight: 900;
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
</style>
