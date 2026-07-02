<script setup>
import { computed } from 'vue'
import { useLogisticsData } from '@/composables/useLogisticsData'

const { gateLogs, getPlateNumber, workOrders } = useLogisticsData()

const workStatusHistory = computed(() =>
  workOrders.value.map((order) => ({
    history_id: order.work_order_id,
    changed_time: order.reserved_time,
    work_order_id: order.work_order_id,
    prev_status: '-',
    new_status: order.work_status,
    changed_by: 'SYSTEM',
    reason: 'DB 작업 상태',
  })),
)

const exceptionLogs = computed(() =>
  gateLogs.value
    .filter((log) => log.process_result && !['NORMAL', '정상'].includes(log.process_result))
    .map((log) => ({
      exception_id: log.gate_log_id,
      process_status: log.process_result,
      exception_type: log.in_out_type,
      plate_number: getPlateNumber(log.vehicle_id),
      exception_message: `${log.gate_name} 처리 확인 필요`,
    })),
)
</script>

<template>
  <div class="page-stack">
    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>작업 상태 이력</h2>
          <span class="status-pill">{{ workStatusHistory.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="history in workStatusHistory" :key="history.history_id" class="timeline-row">
            <time>{{ history.changed_time }}</time>
            <div>
              <b>작업 ID {{ history.work_order_id }} / {{ history.prev_status }} -> {{ history.new_status }}</b>
              <span>{{ history.changed_by }} / {{ history.reason }}</span>
            </div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>예외 처리 기록</h2>
          <span class="status-pill red">{{ exceptionLogs.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="item in exceptionLogs" :key="item.exception_id" class="timeline-row alert">
            <time>{{ item.process_status }}</time>
            <div>
              <b>{{ item.exception_type }} / {{ item.plate_number }}</b>
              <span>{{ item.exception_message }}</span>
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
