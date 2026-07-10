<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useNotificationStore } from '@/stores/adminStore/notificationStore'
import { useLogisticsData } from '@/composables/useLogisticsData'

const gateLogStore = useGateLogStore()
const notificationStore = useNotificationStore()
const { notifications } = storeToRefs(notificationStore)
const { getPlateNumber } = useLogisticsData()

const events = computed(() => {
  return gateLogStore.gateLogs.map((log) => ({
    time: log.entryTime || log.exitTime || '-',
    type: log.inOutType || '-',
    target: getPlateNumber(log.vehicleId),
    message: `${log.gateName || '-'} / ${log.processResult || '-'}`,
  }))
})

const exceptions = computed(() => notifications.value)

onMounted(() => {
  gateLogStore.loadGateLogs()
  notificationStore.loadNotifications()
})
</script>

<template>
  <div class="page-stack">

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>작업 이벤트</h2>
          <span class="status-pill">{{ events.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="event in events" :key="event.time + event.type" class="timeline-row">
            <time>{{ event.time }}</time>
            <div>
              <b>{{ event.type }} · {{ event.target }}</b>
              <span>{{ event.message }}</span>
            </div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>예외 알림</h2>
          <span class="status-pill red">{{ exceptions.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="item in exceptions" :key="item.exceptionType + item.plateNumber + item.occurredTime" class="timeline-row alert">
            <time>{{ item.processStatus }}</time>
            <div>
              <b>{{ item.exceptionType }} · {{ item.plateNumber }}</b>
              <span>{{ item.exceptionMessage }}</span>
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
  grid-template-columns: 76px 1fr;
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
