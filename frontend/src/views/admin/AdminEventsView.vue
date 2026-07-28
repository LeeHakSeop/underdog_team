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
const { notifications, loading, error } = storeToRefs(notificationStore)

const exceptionFilter = ref('ALL')

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
  const vehicle = vehicleStore.vehicles.find((item) => String(item.vehicleId || item.vehicle_id) === String(vehicleId))
  return vehicle?.plateNumber || vehicle?.plate_number || vehicleId || '-'
}

const getVehicleById = (vehicleId) => vehicleStore.vehicles.find((item) => (
  String(item.vehicleId || item.vehicle_id) === String(vehicleId)
))

const normalizeVehicleType = (vehicleType) => {
  if (vehicleType === 'TRACTOR' || vehicleType === '트랙터') return 'TRACTOR'
  if (vehicleType === 'TRAILER' || vehicleType === '트레일러') return 'TRAILER'
  return ''
}

const getLogVehiclePair = (log) => {
  const tractorVehicleId = getValue(log, 'tractorVehicleId', 'tractor_vehicle_id')
  const trailerVehicleId = getValue(log, 'trailerVehicleId', 'trailer_vehicle_id')
  const fallbackVehicleId = getValue(log, 'vehicleId', 'vehicle_id')

  let tractor = tractorVehicleId ? getPlateNumber(tractorVehicleId) : '-'
  let trailer = trailerVehicleId ? getPlateNumber(trailerVehicleId) : '-'

  // 번호판 인식 로그처럼 한쪽 차량 ID만 저장된 경우, 반대쪽에 같은 번호를 반복하지 않는다.
  // 구형 로그에서만 vehicle_id를 차량 종류로 확인해 해당 칸에 보완한다.
  if (!tractorVehicleId && !trailerVehicleId && fallbackVehicleId) {
    const fallbackVehicle = getVehicleById(fallbackVehicleId)
    const fallbackType = normalizeVehicleType(
      fallbackVehicle?.vehicleType || fallbackVehicle?.vehicle_type,
    )

    if (fallbackType === 'TRACTOR') tractor = getPlateNumber(fallbackVehicleId)
    if (fallbackType === 'TRAILER') trailer = getPlateNumber(fallbackVehicleId)
  }

  return {
    tractor,
    trailer,
  }
}

const events = computed(() => gateLogStore.gateLogs.map((log) => {
  const vehicles = getLogVehiclePair(log)

  return {
    key: getValue(log, 'gateLogId', 'gate_log_id'),
    time: formatDateTime(
      getValue(log, 'entryTime', 'entry_time') || getValue(log, 'exitTime', 'exit_time'),
    ),
    inOutType: getValue(log, 'inOutType', 'in_out_type') || '',
    tractorPlateNumber: vehicles.tractor,
    trailerPlateNumber: vehicles.trailer,
    gateName: getValue(log, 'gateName', 'gate_name') || '-',
    processResult: getValue(log, 'processResult', 'process_result') || '',
  }
}))

const getInOutClass = (value) => (value === 'OUT' ? 'red' : 'blue')

const getProcessClass = (value) => displayTone('process', value)

const buildTimeline = (item) => {
  const timeline = []
  const inOutType = getValue(item, 'inOutType', 'in_out_type')
  const entryTime = getValue(item, 'entryTime', 'entry_time')
  const exitTime = getValue(item, 'exitTime', 'exit_time')
  const gateTime = inOutType === 'OUT' ? exitTime || entryTime : entryTime || exitTime

  if (gateTime) {
    timeline.push({
      key: 'gate',
      type: 'GATE',
      label: inOutType === 'OUT' ? '출차' : '입차',
      time: formatDateTime(gateTime),
      sortTime: gateTime,
      description: getValue(item, 'gateName', 'gate_name') || '-',
    })
  }

  const occurredTime = getValue(item, 'occurredTime', 'occurred_time')
  if (occurredTime) {
    timeline.push({
      key: 'exception',
      type: 'EXCEPTION',
      label: eventTypeText.EXCEPTION,
      time: formatDateTime(occurredTime),
      sortTime: occurredTime,
      description: getValue(item, 'exceptionMessage', 'exception_message') || '-',
    })
  }

  const processedTime = getValue(item, 'processedTime', 'processed_time')
  if (processedTime) {
    timeline.push({
      key: 'processed',
      type: 'PROCESSED',
      label: eventTypeText.PROCESSED,
      time: formatDateTime(processedTime),
      sortTime: processedTime,
      description: getValue(item, 'managerAction', 'manager_action') || '관리자 조치 완료',
    })
  }

  return timeline.sort((left, right) => new Date(left.sortTime) - new Date(right.sortTime))
}

const normalizeTimeline = (item) => {
  const timeline = getValue(item, 'timeline')

  if (!Array.isArray(timeline) || timeline.length === 0) {
    return buildTimeline(item)
  }

  return timeline.map((event, index) => {
    const eventType = getValue(event, 'eventType', 'event_type') || 'EXCEPTION'
    const eventTime = getValue(event, 'eventTime', 'event_time')

    return {
      key: `${eventType}-${index}`,
      type: eventType,
      label: eventTypeText[eventType] || getValue(event, 'eventLabel', 'event_label') || '기록',
      time: formatDateTime(eventTime),
      sortTime: eventTime,
      description: getValue(event, 'description') || getValue(event, 'eventLabel', 'event_label') || '-',
    }
  })
}

const exceptionItems = computed(() => notifications.value.map((item, index) => {
  const key = getValue(item, 'exceptionLogId', 'exception_log_id', 'id')
    || `${getValue(item, 'exceptionType', 'exception_type')}-${getValue(item, 'occurredTime', 'occurred_time')}-${index}`

  return {
    key,
    status: getValue(item, 'processStatus', 'process_status') || 'UNPROCESSED',
    type: getValue(item, 'exceptionType', 'exception_type') || 'EXCEPTION',
    plateNumber: getValue(item, 'plateNumber', 'plate_number')
      || getPlateNumber(getValue(item, 'vehicleId', 'vehicle_id')),
    occurredTime: formatDateTime(getValue(item, 'occurredTime', 'occurred_time')),
    processedTime: formatDateTime(getValue(item, 'processedTime', 'processed_time')),
    message: getValue(item, 'exceptionMessage', 'exception_message') || '-',
    managerAction: getValue(item, 'managerAction', 'manager_action') || '조치 대기',
    timeline: normalizeTimeline(item),
  }
}))

const exceptions = computed(() => exceptionItems.value.filter((item) => (
  exceptionFilter.value === 'ALL' || item.status === exceptionFilter.value
)))

const openExceptionCount = computed(() => exceptionItems.value.filter((item) => item.status !== 'PROCESSED').length)

const exceptionTimelineRows = computed(() => exceptions.value.flatMap((item) => {
  if (item.timeline.length === 0) {
    return [{
      key: `${item.key}-exception`,
      time: item.occurredTime,
      title: `${item.type} / ${item.plateNumber}`,
      message: item.message,
      status: item.status,
      type: 'EXCEPTION',
    }]
  }

  return item.timeline.map((event) => ({
    key: `${item.key}-${event.key}`,
    time: event.time,
    title: `${event.label} / ${item.type} / ${item.plateNumber}`,
    message: event.description,
    status: item.status,
    type: event.type,
  }))
}))

const getExceptionStatusClass = (status) => (status === 'PROCESSED' ? 'green' : 'red')

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
                <div class="gate-vehicles" aria-label="트랙터·트레일러 차량번호">
                  <div class="vehicle-tag tractor">
                    <span>트랙터</span>
                    <strong>{{ event.tractorPlateNumber }}</strong>
                  </div>
                  <div class="vehicle-tag trailer">
                    <span>트레일러</span>
                    <strong>{{ event.trailerPlateNumber }}</strong>
                  </div>
                </div>
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
                <span class="status-pill" :class="getExceptionStatusClass(event.status)">{{ event.status }}</span>
              </div>
              <span>{{ event.message }}</span>
              <small v-if="event.type === 'PROCESSED'">관리자 조치 기록</small>
            </div>
          </div>

          <div v-if="!loading && exceptionTimelineRows.length === 0" class="timeline-row alert">
            <time>-</time>
            <div>
              <b>표시할 예외 처리 내역이 없습니다.</b>
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
  border-radius: 4px;
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
.timeline-row span,
.timeline-row small {
  display: block;
}

.timeline-row span {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.timeline-row small {
  margin-top: 8px;
  color: var(--ink-500);
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

.gate-vehicles {
  display: grid;
  grid-template-columns: repeat(2, minmax(150px, 1fr));
  gap: 8px;
  width: min(100%, 520px);
}

.vehicle-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 6px 9px;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.vehicle-tag span {
  flex: 0 0 auto;
  margin-top: 0;
  font-size: 12px;
  font-weight: 900;
}

.vehicle-tag strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 900;
}

.vehicle-tag.tractor {
  background: #eef6ff;
  border-color: #a8c8e6;
}

.vehicle-tag.tractor span {
  color: #1f5f91;
}

.vehicle-tag.trailer {
  background: #fff8eb;
  border-color: #e7c58f;
}

.vehicle-tag.trailer span {
  color: #9a641b;
}

.gate-event-head b {
  color: var(--ink-900);
  font-size: 15px;
  font-weight: 900;
}

.gate-event-head .status-pill {
  flex: 0 0 auto;
}

.section-hint {
  margin: 4px 0 0;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.status-filter {
  min-width: 110px;
  padding: 8px 10px;
  color: var(--ink-700);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-weight: 700;
}

@media (max-width: 760px) {
  .timeline-row {
    grid-template-columns: 1fr;
  }

  .timeline-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .gate-vehicles {
    width: 100%;
    grid-template-columns: 1fr;
  }
}
</style>
