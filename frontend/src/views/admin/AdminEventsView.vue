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
const { notifications, loading, error, processingId } = storeToRefs(notificationStore)

const exceptionFilter = ref('UNPROCESSED')

const eventTypeText = {
  GATE: '게이트 처리',
  EXCEPTION: '예외 발생',
  PROCESSED: '처리 완료',
}

const exceptionMetaMap = {
  DRIVER_CANNOT_ENTER: { label: '기사 출입 제한', tone: 'danger', action: '기사 상태와 승인 여부를 우선 확인하세요.' },
  DRIVER_ALREADY_ASSIGNED: { label: '기사 중복 배정', tone: 'warning', action: '같은 기사에 활성 작업이 중복 배정되었는지 확인하세요.' },
  DRIVER_UNAVAILABLE: { label: '기사 배정 불가', tone: 'warning', action: '기사 승인 상태와 출입 가능 여부를 먼저 확인하세요.' },
  CARRIER_INACTIVE: { label: '운송사 비활성', tone: 'danger', action: '운송사 계정 상태와 승인 이력을 확인하세요.' },
  WORK_ORDER_NOT_APPROVED: { label: '작업 승인 대기', tone: 'warning', action: '배차 승인 누락 여부를 먼저 확인하세요.' },
  WORK_ORDER_NOT_FOUND: { label: '작업지시 누락', tone: 'danger', action: '최근 배차 변경과 작업지시 생성 여부를 재확인하세요.' },
  VEHICLE_NOT_REGISTERED: { label: '차량 미등록', tone: 'danger', action: '차량 등록과 번호판 정보를 즉시 확인하세요.' },
  VEHICLE_UNAVAILABLE: { label: '차량 배정 불가', tone: 'warning', action: '차량 승인 상태와 정비 여부를 먼저 확인하세요.' },
  VEHICLE_DUPLICATE_ASSIGNMENT: { label: '차량 중복 배정', tone: 'warning', action: '동일 차량이 다른 활성 작업에 배정됐는지 확인하세요.' },
  VEHICLE_TYPE_MISMATCH: { label: '차량 유형 불일치', tone: 'warning', action: '트랙터와 트레일러 연결 상태를 점검하세요.' },
  PLATE_NOT_DETECTED: { label: '번호판 인식 실패', tone: 'warning', action: '촬영 환경과 인식 장비 상태를 확인하세요.' },
  CONTAINER_NOT_FOUND: { label: '컨테이너 정보 누락', tone: 'danger', action: '컨테이너와 작업지시 연결 상태를 확인하세요.' },
  YARD_SECTOR_NOT_FOUND: { label: '야드 섹터 누락', tone: 'warning', action: '야드 배치 정보와 목적지를 점검하세요.' },
  YARD_SECTOR_UNAVAILABLE: { label: '야드 섹터 사용 불가', tone: 'warning', action: '점검 또는 비활성 상태 섹터인지 먼저 확인하세요.' },
  YARD_SECTOR_CAPACITY_EXCEEDED: { label: '야드 수용량 초과', tone: 'danger', action: '혼잡 섹터 대신 다른 배정 가능 위치를 확인하세요.' },
  TRACTOR_INFO_NOT_FOUND: { label: '트랙터 정보 누락', tone: 'warning', action: '트랙터 등록 정보와 기사 연결 상태를 확인하세요.' },
  AI_SERVER_ERROR: { label: '인식 서버 오류', tone: 'danger', action: 'AI 인식 서버 상태와 재시도 여부를 확인하세요.' },
  UNKNOWN_VEHICLE_TYPE: { label: '차량 유형 미확인', tone: 'warning', action: '차량 기본 정보와 운영 규칙을 확인하세요.' },
  EXCEPTION: { label: '기타 예외', tone: 'warning', action: '상세 로그를 확인해 원인과 조치를 정리하세요.' },
}

const getValue = (item, ...keys) => {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== undefined && value !== null && value !== '') return value
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

  if (!tractorVehicleId && !trailerVehicleId && fallbackVehicleId) {
    const fallbackVehicle = getVehicleById(fallbackVehicleId)
    const fallbackType = normalizeVehicleType(
      fallbackVehicle?.vehicleType || fallbackVehicle?.vehicle_type,
    )

    if (fallbackType === 'TRACTOR') tractor = getPlateNumber(fallbackVehicleId)
    if (fallbackType === 'TRAILER') trailer = getPlateNumber(fallbackVehicleId)
  }

  return { tractor, trailer }
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
  const type = getValue(item, 'exceptionType', 'exception_type') || 'EXCEPTION'
  const meta = exceptionMetaMap[type] || exceptionMetaMap.EXCEPTION

  return {
    key,
    exceptionLogId: getValue(item, 'exceptionLogId', 'exception_log_id', 'id'),
    status: getValue(item, 'processStatus', 'process_status') || 'UNPROCESSED',
    type,
    typeLabel: meta.label,
    tone: meta.tone,
    recommendedAction: meta.action,
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
const criticalExceptionCount = computed(() => exceptionItems.value.filter((item) => item.status !== 'PROCESSED' && item.tone === 'danger').length)

const topExceptionSummary = computed(() => {
  const grouped = new Map()

  exceptionItems.value
    .filter((item) => item.status !== 'PROCESSED')
    .forEach((item) => {
      const current = grouped.get(item.type) || {
        type: item.type,
        label: item.typeLabel,
        tone: item.tone,
        count: 0,
        action: item.recommendedAction,
      }
      current.count += 1
      grouped.set(item.type, current)
    })

  return Array.from(grouped.values())
    .sort((left, right) => {
      if (left.tone !== right.tone) {
        return left.tone === 'danger' ? -1 : 1
      }
      return right.count - left.count
    })
    .slice(0, 3)
})

const exceptionTimelineRows = computed(() => exceptions.value.flatMap((item) => {
  if (item.timeline.length === 0) {
    return [{
      key: `${item.key}-exception`,
      exceptionLogId: item.exceptionLogId,
      time: item.occurredTime,
      title: `${item.typeLabel} / ${item.plateNumber}`,
      message: item.message,
      status: item.status,
      type: 'EXCEPTION',
      tone: item.tone,
      recommendedAction: item.recommendedAction,
    }]
  }

  return item.timeline.map((event) => ({
    key: `${item.key}-${event.key}`,
    exceptionLogId: item.exceptionLogId,
    time: event.time,
    title: `${event.label} / ${item.typeLabel} / ${item.plateNumber}`,
    message: event.description,
    status: item.status,
    type: event.type,
    tone: item.tone,
    recommendedAction: item.recommendedAction,
  }))
}))

const getExceptionStatusClass = (status) => (status === 'PROCESSED' ? 'green' : 'red')
const getToneClass = (tone) => {
  if (tone === 'danger') return 'danger'
  if (tone === 'warning') return 'warning'
  return 'info'
}

const handleProcessException = async (event) => {
  if (!event?.exceptionLogId || event.status === 'PROCESSED') return
  await notificationStore.processNotification(event.exceptionLogId, event.recommendedAction).catch(() => {})
}

onMounted(() => {
  gateLogStore.loadGateLogs().catch(() => {})
  notificationStore.loadNotifications().catch(() => {})
  vehicleStore.loadVehicles().catch(() => {})
})
</script>

<template>
  <div class="page-stack">
    <section class="grid-2">
      <article class="panel gate-history-panel">
        <div class="section-title">
          <h2>게이트 처리 이력</h2>
          <span class="status-pill">{{ events.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="event in events" :key="event.key" class="timeline-row">
            <time>{{ event.time }}</time>
            <div>
              <div class="gate-event-head">
                <span class="status-pill gate-direction-pill" :class="getInOutClass(event.inOutType)">
                  {{ inOutTypeLabel(event.inOutType) }} · {{ event.gateName }}
                </span>
                <div class="gate-vehicles" aria-label="트랙터와 트레일러 차량번호">
                  <div class="vehicle-tag tractor">
                    <span>트랙터</span>
                    <strong>{{ event.tractorPlateNumber }}</strong>
                  </div>
                  <div class="vehicle-tag trailer">
                    <span>트레일러</span>
                    <strong>{{ event.trailerPlateNumber }}</strong>
                  </div>
                </div>
                <span class="status-pill gate-result-pill" :class="getProcessClass(event.processResult)">
                  {{ processResultLabel(event.processResult) }}
                </span>
              </div>
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

        <div class="exception-overview">
          <article class="overview-card danger">
            <span>긴급 예외</span>
            <strong>{{ criticalExceptionCount }}건</strong>
            <small>즉시 확인이 필요한 위험 예외</small>
          </article>
          <article class="overview-card warning">
            <span>미처리 예외</span>
            <strong>{{ openExceptionCount }}건</strong>
            <small>아직 조치 전인 예외 건수</small>
          </article>
          <article class="overview-card info">
            <span>가장 많은 유형</span>
            <strong>{{ topExceptionSummary[0]?.label || '없음' }}</strong>
            <small>{{ topExceptionSummary[0] ? `${topExceptionSummary[0].count}건` : '현재 없음' }}</small>
          </article>
        </div>

        <div v-if="topExceptionSummary.length > 0" class="exception-summary-list">
          <article
            v-for="item in topExceptionSummary"
            :key="item.type"
            class="exception-summary-item"
            :class="getToneClass(item.tone)"
          >
            <div>
              <b>{{ item.label }}</b>
              <span>{{ item.count }}건</span>
            </div>
            <p>{{ item.action }}</p>
          </article>
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
            :class="['timeline-row', 'alert', event.type?.toLowerCase(), getToneClass(event.tone)]"
          >
            <time>{{ event.time }}</time>
            <div>
              <div class="timeline-heading">
                <b>{{ event.title }}</b>
                <div class="timeline-actions">
                  <button
                    v-if="event.type === 'EXCEPTION' && event.status !== 'PROCESSED'"
                    class="ghost-button"
                    type="button"
                    :disabled="processingId === event.exceptionLogId"
                    @click="handleProcessException(event)"
                  >
                    {{ processingId === event.exceptionLogId ? '처리 중...' : '확인 처리' }}
                  </button>
                  <span class="status-pill" :class="getExceptionStatusClass(event.status)">{{ event.status }}</span>
                </div>
              </div>
              <span>{{ event.message }}</span>
              <small v-if="event.type === 'EXCEPTION'">{{ event.recommendedAction }}</small>
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
.grid-2 {
  grid-template-columns: minmax(0, calc(50% + 45px)) minmax(0, calc(50% - 55px));
}

.exception-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.overview-card {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--line);
  background: #f7f9fb;
}

.overview-card span,
.overview-card small {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.overview-card strong {
  color: var(--ink-900);
  font-size: 22px;
  font-weight: 900;
}

.overview-card.danger {
  background: #fff4f4;
  border-color: #e4a6a6;
}

.overview-card.warning {
  background: #fff8ea;
  border-color: #ebcf8b;
}

.exception-summary-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.exception-summary-item {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--line);
  background: #f8fafc;
}

.exception-summary-item > div {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.exception-summary-item b {
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 900;
}

.exception-summary-item span,
.exception-summary-item p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.5;
}

.exception-summary-item.danger {
  background: #fff4f4;
  border-color: #e4a6a6;
  border-left: 4px solid #b63a3a;
}

.exception-summary-item.warning {
  background: #fff8ea;
  border-color: #ebcf8b;
  border-left: 4px solid #cc8a12;
}

.exception-summary-item.info {
  border-left: 4px solid #2f6fad;
}

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

.timeline-row.alert.gate,
.timeline-row.alert.info {
  background: #f6f9fd;
  border-color: #c7d6e5;
}

.timeline-row.alert.processed {
  background: #f5fbf7;
  border-color: #bddfc8;
}

.timeline-row.alert.warning {
  background: #fff8ea;
  border-color: #ebcf8b;
}

.timeline-row.alert.danger {
  background: #fff4f4;
  border-color: #e4a6a6;
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
  min-width: 0;
}

.timeline-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.timeline-actions .ghost-button {
  min-width: 72px;
  white-space: nowrap;
}

.timeline-heading .status-pill {
  flex: 0 0 auto;
}

.gate-event-head {
  display: grid;
  min-width: 0;
  grid-template-columns: max-content 350px max-content;
  grid-template-areas: 'direction vehicles result';
  align-items: center;
  gap: 8px;
  overflow-x: auto;
}

.gate-direction-pill {
  grid-area: direction;
}

.gate-vehicles {
  display: grid;
  grid-area: vehicles;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  width: 100%;
}

.vehicle-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  padding: 5px 6px;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.vehicle-tag span {
  flex: 0 0 auto;
  margin-top: 0;
  font-size: 11px;
  font-weight: 900;
}

.vehicle-tag strong {
  min-width: 0;
  white-space: nowrap;
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

.gate-event-head .status-pill {
  display: inline-flex;
  flex: 0 0 auto;
  width: max-content;
  margin-top: 0;
}

.gate-result-pill {
  grid-area: result;
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

@media (max-width: 1100px) {
  .grid-2,
  .exception-overview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .timeline-row {
    grid-template-columns: 1fr;
  }

  .timeline-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .timeline-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
