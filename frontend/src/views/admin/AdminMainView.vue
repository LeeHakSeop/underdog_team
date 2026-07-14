<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useContainerStore } from '@/stores/adminStore/containerStore'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { useCarrierStore } from '@/stores/carrierStore'
import { usePlateRecognitionStore } from '@/stores/adminStore/plateRecognitionStore'
import { vehicleTypeLabel } from '@/config/vehicleType'

const gateLogStore = useGateLogStore()
const workOrderStore = useWorkOrderStore()
const containerStore = useContainerStore()
const vehicleStore = useVehicleStore()
const driverStore = useDriverStore()
const carrierStore = useCarrierStore()
const plateRecognitionStore = usePlateRecognitionStore()

const selectedGateId = ref('G-01')
const processType = ref('IN')
const selectedOcrType = ref('crnn')
const gatePreviewUrls = reactive({})
const gateRecognitionResults = reactive({})
let refreshTimer = null

const gateSlots = [
  { id: 'G-01', gateNumber: 'G01', gateName: '입차 게이트 1', inOutType: 'IN' },
  { id: 'G-02', gateNumber: 'G02', gateName: '입차 게이트 2', inOutType: 'IN' },
  { id: 'G-03', gateNumber: 'G03', gateName: '출차 게이트 1', inOutType: 'OUT' },
  { id: 'G-04', gateNumber: 'G04', gateName: '출차 게이트 2', inOutType: 'OUT' },
]

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]
const getValue = (row, camelKey, snakeKey) => row?.[camelKey] ?? row?.[snakeKey] ?? ''

const getPlateNumber = (vehicleId) => {
  const vehicle = vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId)
  return getValue(vehicle, 'plateNumber', 'plate_number') || vehicleId || '-'
}

const getWorkOrderPlateText = (order) => {
  if (!order) return '-'

  const tractorId = getId(order, 'tractorVehicleId')
  const trailerId = getId(order, 'trailerVehicleId') || getId(order, 'vehicleId')

  if (tractorId && trailerId && tractorId !== trailerId) {
    return `${getPlateNumber(tractorId)} / ${getPlateNumber(trailerId)}`
  }

  return getPlateNumber(trailerId || tractorId || getId(order, 'vehicleId'))
}

const getGateLogPlateText = (log) => {
  if (!log) return '-'

  const tractorId = getId(log, 'tractorVehicleId')
  const trailerId = getId(log, 'trailerVehicleId') || getId(log, 'vehicleId')

  if (tractorId && trailerId && tractorId !== trailerId) {
    return `${getPlateNumber(tractorId)} / ${getPlateNumber(trailerId)}`
  }

  return getPlateNumber(trailerId || tractorId || getId(log, 'vehicleId'))
}

const getVehicle = (vehicleId) => {
  return vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId) || null
}

const getDriverName = (driverId) => {
  const driver = driverStore.drivers.find((item) => getId(item, 'driverId') === driverId)
  return getValue(driver, 'driverName', 'driver_name') || '-'
}

const getCarrierName = (carrierId) => {
  const carrier = carrierStore.carriers.find((item) => getId(item, 'carrierId') === carrierId)
  return getValue(carrier, 'carrierName', 'carrier_name') || '-'
}

const getContainer = (containerId) => {
  return containerStore.containers.find((item) => getId(item, 'containerId') === containerId) || null
}

const getContainerNumber = (containerId) => {
  const container = getContainer(containerId)
  return getValue(container, 'containerNumber', 'container_number') || '-'
}

const getWorkStatus = (order) => getValue(order, 'workStatus', 'work_status')

const statusText = (status) => {
  if (status === 'DISPATCH_WAITING') return '배차 대기'
  if (status === 'APPROVED') return '입차 대기'
  if (status === 'GATE_IN') return '입차 완료'
  if (status === 'IN_PROGRESS') return '작업 중'
  if (status === 'COMPLETED') return '출차 대기'
  if (status === 'GATE_OUT') return '출차 완료'
  return status || '-'
}

const gateText = (type) => (type === 'OUT' ? '출차' : '입차')

const isGateProcessLog = (log) => {
  const processResult = getValue(log, 'processResult', 'process_result')
  return processResult === 'GATE_SUCCESS' || processResult === 'GATE_FAIL'
}

const operationalGateLogs = computed(() => gateLogStore.gateLogs.filter(isGateProcessLog))

const latestGateLogs = computed(() => gateLogStore.gateLogs.slice(0, 8))

const todayGateIn = computed(() => operationalGateLogs.value.filter((log) => getValue(log, 'inOutType', 'in_out_type') === 'IN').length)
const todayGateOut = computed(() => operationalGateLogs.value.filter((log) => getValue(log, 'inOutType', 'in_out_type') === 'OUT').length)
const activeWorkCount = computed(() =>
  workOrderStore.workOrders.filter((order) => ['GATE_IN', 'IN_PROGRESS'].includes(getWorkStatus(order))).length,
)
const readyOutCount = computed(() =>
  workOrderStore.workOrders.filter((order) => getWorkStatus(order) === 'COMPLETED').length,
)

const statusCards = computed(() => [
  { label: '출차 대기', value: readyOutCount.value, detail: '작업 완료 차량', tone: 'amber' },
  { label: '작업 진행', value: activeWorkCount.value, detail: '입차 완료/작업 중', tone: 'green' },
  { label: '현재 출차', value: todayGateOut.value, detail: '게이트 OUT 로그', tone: 'red' },
  { label: '현재 입차', value: todayGateIn.value, detail: '게이트 IN 로그', tone: 'blue' },
])

const gateCells = computed(() => {
  const logsByType = {
    IN: [],
    OUT: [],
  }

  operationalGateLogs.value.forEach((log) => {
    const inOutType = getValue(log, 'inOutType', 'in_out_type') === 'OUT' ? 'OUT' : 'IN'
    logsByType[inOutType].push(log)
  })

  const usedLogs = new Set()

  return gateSlots.map((slot) => {
    const log =
      operationalGateLogs.value.find((item) => {
        const gateNumber = getValue(item, 'gateNumber', 'gate_number')
        return gateNumber === slot.gateNumber && !usedLogs.has(item)
      }) ||
      logsByType[slot.inOutType].find((item) => !usedLogs.has(item)) ||
      null

    if (log) {
      usedLogs.add(log)
    }

    const vehicleId = log ? getId(log, 'vehicleId') : null

    return {
      ...slot,
      processResult: log ? getValue(log, 'processResult', 'process_result') || '대기' : '대기',
      vehicleId,
      recognizedVehicleNo: log ? getGateLogPlateText(log) : '',
      entryTime: log ? getValue(log, 'entryTime', 'entry_time') : '',
      exitTime: log ? getValue(log, 'exitTime', 'exit_time') : '',
    }
  })
})

const selectedGate = computed(() => gateCells.value.find((gate) => gate.id === selectedGateId.value) || gateCells.value[0])

const matchedOrder = computed(() => {
  const vehicleId = selectedGate.value?.vehicleId
  if (!vehicleId) return null

  return (
    workOrderStore.workOrders.find((order) =>
      [getId(order, 'vehicleId'), getId(order, 'tractorVehicleId'), getId(order, 'trailerVehicleId')].includes(vehicleId),
    ) || null
  )
})

const matchedVehicle = computed(() => getVehicle(selectedGate.value?.vehicleId))
const matchedContainer = computed(() => (matchedOrder.value ? getContainer(getId(matchedOrder.value, 'containerId')) : null))

const selectedDriverName = computed(() => {
  if (matchedOrder.value) return getDriverName(getId(matchedOrder.value, 'driverId'))
  const driverId = getId(matchedVehicle.value, 'driverId')
  return driverId ? getDriverName(driverId) : '-'
})

const selectedCarrierName = computed(() => {
  const orderCarrierId = getId(matchedOrder.value, 'carrierId')
  if (orderCarrierId) return getCarrierName(orderCarrierId)

  const vehicleCarrierId = getId(matchedVehicle.value, 'carrierId')
  return vehicleCarrierId ? getCarrierName(vehicleCarrierId) : '-'
})

const activeOrders = computed(() =>
  workOrderStore.workOrders
    .filter((order) => ['APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED'].includes(getWorkStatus(order)))
    .slice(0, 6),
)

const yardSectors = computed(() => {
  const sectorMap = new Map()

  containerStore.containers.forEach((container) => {
    const sectorId = getId(container, 'sectorId') || `${container.block || '미지정'}-${container.bay || '-'}`
    const current = sectorMap.get(sectorId) || {
      id: sectorId,
      name: getValue(container, 'sectorName', 'sector_name') || container.block || '미지정',
      total: 0,
      canExit: 0,
      hold: 0,
    }

    current.total += 1
    if (container.canExit ?? container.can_exit) {
      current.canExit += 1
    } else {
      current.hold += 1
    }

    sectorMap.set(sectorId, current)
  })

  return Array.from(sectorMap.values()).slice(0, 8)
})

const tractorResult = computed(() => gateRecognitionResults[selectedGateId.value]?.tractor || null)
const trailerResult = computed(() => gateRecognitionResults[selectedGateId.value]?.trailer || null)

const getVehicleType = (result) => {
  const vehicleType = String(result?.vehicle?.vehicleType || '').trim().toUpperCase()

  if (vehicleType === '트랙터') return 'TRACTOR'
  if (vehicleType === '트레일러') return 'TRAILER'

  return vehicleType
}
const getBooleanText = (value) => (value === true ? '가능' : value === false ? '불가' : '-')

const getPassText = (result, expectedType) => {
  if (!result?.matched || getVehicleType(result) !== expectedType || result.needReview) return '불가'
  return '가능'
}

const tractorPassText = computed(() => getPassText(tractorResult.value, 'TRACTOR'))
const trailerPassText = computed(() => getPassText(trailerResult.value, 'TRAILER'))
const isReadyForGateProcess = computed(() => tractorPassText.value === '가능' && trailerPassText.value === '가능')

const gateProcessPayload = computed(() => ({
  tractorVehicleId: tractorResult.value?.vehicle?.vehicleId || null,
  trailerVehicleId: trailerResult.value?.vehicle?.vehicleId || null,
  workOrderId: trailerResult.value?.workOrder?.workOrderId || trailerResult.value?.trailerWorkInfo?.workOrderId || null,
  containerId: trailerResult.value?.workOrder?.containerId || trailerResult.value?.trailerWorkInfo?.containerId || null,
  sectorId: trailerResult.value?.container?.sectorId || trailerResult.value?.trailerWorkInfo?.sectorId || null,
  gateNumber: selectedGate.value?.gateNumber || 'G01',
  gateName: selectedGate.value?.gateName || 'AI_GATE',
  inOutType: processType.value,
}))

const gateProcessMissingItems = computed(() => {
  const payload = gateProcessPayload.value
  return [
    [payload.tractorVehicleId, '트랙터 차량'],
    [payload.trailerVehicleId, '트레일러 차량'],
    [payload.workOrderId, '작업정보'],
    [payload.containerId, '컨테이너'],
    [payload.sectorId, '야드 섹터'],
  ].filter(([value]) => !value).map(([, label]) => label)
})

const canProcessGate = computed(() => isReadyForGateProcess.value && gateProcessMissingItems.value.length === 0)

const recognitionStatus = (result, expectedType) => {
  if (!result) return '인식 대기'
  if (result.needReview) return 'RECOGNITION_NEED_REVIEW'
  return getPassText(result, expectedType) === '가능' ? '정상' : '인식 불가'
}

const getGateRecognition = (gate, targetType) => gateRecognitionResults[gate.id]?.[targetType] || null

const getGateOcrStatus = (gate) => {
  const tractor = getGateRecognition(gate, 'tractor')
  const trailer = getGateRecognition(gate, 'trailer')

  if (!tractor && !trailer) return { text: 'OCR 대기', tone: 'idle' }
  if (tractor?.needReview || trailer?.needReview) return { text: '확인 필요', tone: 'warning' }
  if (tractor?.aiResult?.detected && trailer?.aiResult?.detected) return { text: 'OCR 성공', tone: 'success' }
  return { text: 'OCR 실패', tone: 'danger' }
}

const getGateDbStatus = (gate) => {
  const tractor = getGateRecognition(gate, 'tractor')
  const trailer = getGateRecognition(gate, 'trailer')

  if (!tractor && !trailer) return { text: 'DB 대기', tone: 'idle' }
  if (tractor?.matched && trailer?.matched) return { text: 'DB 매칭', tone: 'success' }
  return { text: '미등록/불일치', tone: 'danger' }
}

const selectGateImage = async (event, gate, targetType) => {
  const file = event.target.files?.[0] || null
  if (!file) return

  selectedGateId.value = gate.id
  const key = `${gate.id}-${targetType}`
  const oldPreviewUrl = gatePreviewUrls[key]
  if (oldPreviewUrl) URL.revokeObjectURL(oldPreviewUrl)
  gatePreviewUrls[key] = URL.createObjectURL(file)

  await plateRecognitionStore.recognize(file, selectedOcrType.value, targetType)
  gateRecognitionResults[gate.id] = {
    ...(gateRecognitionResults[gate.id] || {}),
    [targetType]: targetType === 'tractor'
      ? plateRecognitionStore.tractorResult
      : plateRecognitionStore.trailerResult,
  }
}

const submitGateProcess = async () => {
  if (canProcessGate.value) await gateLogStore.processGate(gateProcessPayload.value)
}

const loadData = () => {
  gateLogStore.loadGateLogs().catch(() => {})
  workOrderStore.loadWorkOrders().catch(() => {})
  containerStore.loadContainers().catch(() => {})
  vehicleStore.loadVehicles().catch(() => {})
  driverStore.loadDrivers().catch(() => {})
  carrierStore.loadCarriers().catch(() => {})
}

watch(selectedGate, (gate) => {
  processType.value = gate?.inOutType || 'IN'
})

onMounted(() => {
  plateRecognitionStore.reset()
  loadData()
  refreshTimer = setInterval(loadData, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  Object.values(gatePreviewUrls).forEach((url) => URL.revokeObjectURL(url))
})
</script>

<template>
  <div class="control-room">
    <section class="ops-strip">
      <article v-for="card in statusCards" :key="card.label" class="ops-card" :class="card.tone">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}건</strong>
        <small>{{ card.detail }}</small>
      </article>
    </section>

    <section class="control-layout">
      <article class="cctv-wall" aria-label="4개 게이트 관제 현황">
        <article
          v-for="gate in gateCells"
          :key="gate.id"
          class="cctv-cell"
          :class="{ active: gate.id === selectedGateId, empty: !gate.vehicleId, out: gate.inOutType === 'OUT' }"
          @click="selectedGateId = gate.id"
        >
          <span class="gate-head">
            <b>{{ gate.gateName }}</b>
            <span class="gate-meta">
              <i>{{ gateText(gate.inOutType) }}</i>
              <em>LIVE</em>
            </span>
          </span>
          <span class="gate-body">
            <label class="camera-upload" :for="`tractorImage-${gate.id}`" @click.stop>
              <span>트랙터 인식</span>
              <img v-if="gatePreviewUrls[`${gate.id}-tractor`]" :src="gatePreviewUrls[`${gate.id}-tractor`]" alt="선택한 트랙터 이미지" />
              <b v-else>트랙터 이미지 업로드</b>
              <small :class="{ review: recognitionStatus(gateRecognitionResults[gate.id]?.tractor, 'TRACTOR') === 'RECOGNITION_NEED_REVIEW' }">
                {{ recognitionStatus(gateRecognitionResults[gate.id]?.tractor, 'TRACTOR') }}
              </small>
              <input :id="`tractorImage-${gate.id}`" accept="image/*" type="file" @change="selectGateImage($event, gate, 'tractor')" />
            </label>
            <label class="camera-upload" :for="`trailerImage-${gate.id}`" @click.stop>
              <span>트레일러 인식</span>
              <img v-if="gatePreviewUrls[`${gate.id}-trailer`]" :src="gatePreviewUrls[`${gate.id}-trailer`]" alt="선택한 트레일러 이미지" />
              <b v-else>트레일러 이미지 업로드</b>
              <small :class="{ review: recognitionStatus(gateRecognitionResults[gate.id]?.trailer, 'TRAILER') === 'RECOGNITION_NEED_REVIEW' }">
                {{ recognitionStatus(gateRecognitionResults[gate.id]?.trailer, 'TRAILER') }}
              </small>
              <input :id="`trailerImage-${gate.id}`" accept="image/*" type="file" @change="selectGateImage($event, gate, 'trailer')" />
            </label>
          </span>
          <span class="gate-summary">
            <small :class="getGateOcrStatus(gate).tone">{{ getGateOcrStatus(gate).text }}</small>
            <small :class="getGateDbStatus(gate).tone">{{ getGateDbStatus(gate).text }}</small>
            <small :class="gate.vehicleId ? 'success' : 'idle'">{{ gate.vehicleId ? '작업 로그 있음' : '게이트 대기' }}</small>
          </span>
        </article>
      </article>

      <aside class="recognition-panel">
        <div class="info-stack">
          <section>
            <h3>트랙터 조회 정보</h3>
            <dl>
              <div><dt>차량 번호 / 유형</dt><dd>{{ tractorResult?.vehicle?.plateNumber || '-' }} / {{ vehicleTypeLabel(tractorResult?.vehicle?.vehicleType) }}</dd></div>
              <div><dt>차량 등록 / 상태</dt><dd>{{ getBooleanText(tractorResult?.vehicle?.isRegistered) }} / {{ tractorResult?.vehicle?.vehicleStatus || '-' }}</dd></div>
              <div><dt>기사</dt><dd>{{ tractorResult?.driver?.driverName || '-' }} / {{ tractorResult?.driver?.driverContact || '-' }}</dd></div>
              <div><dt>기사 등록 / 출입</dt><dd>{{ getBooleanText(tractorResult?.driver?.isRegistered) }} / {{ getBooleanText(tractorResult?.driver?.canEnter) }}</dd></div>
              <div><dt>운송사 / 연락처</dt><dd>{{ tractorResult?.carrier?.carrierName || '-' }} / {{ tractorResult?.carrier?.carrierContact || '-' }}</dd></div>
              <div><dt>담당자 / 상태</dt><dd>{{ tractorResult?.carrier?.managerName || '-' }} / {{ tractorResult?.carrier?.carrierStatus || '-' }}</dd></div>
            </dl>
          </section>

          <section>
            <h3>트레일러 조회 정보</h3>
            <dl>
              <div><dt>차량 번호 / 유형</dt><dd>{{ trailerResult?.vehicle?.plateNumber || '-' }} / {{ vehicleTypeLabel(trailerResult?.vehicle?.vehicleType) }}</dd></div>
              <div><dt>차량 등록 / 상태</dt><dd>{{ getBooleanText(trailerResult?.vehicle?.isRegistered) }} / {{ trailerResult?.vehicle?.vehicleStatus || '-' }}</dd></div>
              <div><dt>작업 유형 / 상태</dt><dd>{{ trailerResult?.workOrder?.workType || '-' }} / {{ trailerResult?.workOrder?.workStatus || '-' }}</dd></div>
              <div><dt>작업 승인 / 예약</dt><dd>{{ getBooleanText(trailerResult?.workOrder?.isApproved) }} / {{ trailerResult?.workOrder?.reservedTime || '-' }}</dd></div>
              <div><dt>컨테이너 번호 / 크기</dt><dd>{{ trailerResult?.container?.containerNumber || '-' }} / {{ trailerResult?.container?.containerSize || '-' }}</dd></div>
              <div><dt>위치 / 블록-베이-로우</dt><dd>{{ trailerResult?.container?.containerLocation || '-' }} / {{ trailerResult?.container?.block || '-' }}-{{ trailerResult?.container?.bay || '-' }}-{{ trailerResult?.container?.rowNo || '-' }}</dd></div>
              <div><dt>야드 섹터 / 상태</dt><dd>{{ trailerResult?.yardSector?.sectorName || '-' }} / {{ trailerResult?.yardSector?.sectorStatus || '-' }}</dd></div>
              <div><dt>대체 대기 / 안내</dt><dd>{{ trailerResult?.yardSector?.altWaitingArea || '-' }} / {{ trailerResult?.yardSector?.guideMessage || '-' }}</dd></div>

            </dl>
          </section>

          <section>
            <h3>선택 게이트</h3>
            <dl>
              <div><dt>게이트</dt><dd>{{ selectedGate?.gateName || '-' }} / {{ gateText(selectedGate?.inOutType) }}</dd></div>
              <div><dt>트랙터 인식</dt><dd>{{ tractorResult?.aiResult?.plateNumber || '-' }} / {{ tractorPassText }}</dd></div>
              <div><dt>트레일러 인식</dt><dd>{{ trailerResult?.aiResult?.plateNumber || '-' }} / {{ trailerPassText }}</dd></div>
            </dl>
          </section>
        </div>
        <div class="process-actions side-process-actions">
          <button class="primary-button process-button" type="button" :disabled="!canProcessGate || gateLogStore.loading" @click="processType = 'IN'; submitGateProcess()">{{ gateLogStore.loading && processType === 'IN' ? '처리 중' : '입차 처리' }}</button>
          <button class="primary-button process-button out" type="button" :disabled="!canProcessGate || gateLogStore.loading" @click="processType = 'OUT'; submitGateProcess()">{{ gateLogStore.loading && processType === 'OUT' ? '처리 중' : '출차 처리' }}</button>
        </div>
      </aside>
    </section>

    <section class="ai-process-zone">
      <div class="decision-stack">
        <div :class="['final-decision', canProcessGate ? 'success' : 'warning']">
          <span>최종 출입 판단</span>
          <strong>{{ canProcessGate ? '통과' : '불가' }}</strong>
          <p>{{ canProcessGate ? '트랙터·트레일러·작업정보가 모두 확인되었습니다.' : `확인 필요: ${gateProcessMissingItems.join(', ') || '번호판 인식 결과'}` }}</p>
        </div>
      </div>
      <ol class="process-steps" aria-label="번호판 출입 처리 단계">
        <li :class="{ complete: tractorResult }"><b>1</b><span>트랙터 인식</span></li>
        <li :class="{ complete: trailerResult }"><b>2</b><span>트레일러 인식</span></li>
        <li :class="{ complete: canProcessGate }"><b>3</b><span>정보 검증</span></li>
        <li :class="{ complete: gateLogStore.processResult?.success }"><b>4</b><span>출입 처리</span></li>
      </ol>
      <p v-if="gateLogStore.processResult" :class="['process-result', gateLogStore.processResult.success ? 'success' : 'warning']">{{ gateLogStore.processResult.message }}</p>
      <p v-if="plateRecognitionStore.error || gateLogStore.error" class="process-result warning">{{ plateRecognitionStore.error || gateLogStore.error }}</p>
    </section>

    <section class="monitor-grid">
      <article class="panel dark-panel">
        <div class="section-title dark-title">
          <h2>작업 진행 요약</h2>
          <span class="status-pill">{{ activeOrders.length }}건</span>
        </div>
        <div class="work-lane">
          <div v-for="order in activeOrders" :key="getId(order, 'workOrderId')" class="work-row">
            <b>{{ getWorkOrderPlateText(order) }}</b>
            <span>{{ getContainerNumber(getId(order, 'containerId')) }}</span>
            <small>{{ statusText(getWorkStatus(order)) }}</small>
          </div>
          <div v-if="activeOrders.length === 0" class="empty-dark">진행 중인 작업이 없습니다.</div>
        </div>
      </article>

      <article class="panel dark-panel">
        <div class="section-title dark-title">
          <h2>야드 섹터 요약</h2>
          <span class="status-pill">{{ yardSectors.length }}개</span>
        </div>
        <div class="yard-grid">
          <div v-for="sector in yardSectors" :key="sector.id" class="yard-node">
            <b>{{ sector.name }}</b>
            <strong>{{ sector.total }}</strong>
            <span>반출 가능 {{ sector.canExit }} / 보류 {{ sector.hold }}</span>
          </div>
          <div v-if="yardSectors.length === 0" class="empty-dark">야드 섹터 데이터가 없습니다.</div>
        </div>
      </article>
    </section>

    <section class="panel log-panel">
      <div class="section-title dark-title">
        <h2>최근 게이트 입출차 기록</h2>
        <span class="status-pill">5초 갱신</span>
      </div>
      <div class="compact-table">
        <div class="compact-row head">
          <span>시간</span>
          <span>차량</span>
          <span>게이트</span>
          <span>구분</span>
          <span>처리 결과</span>
        </div>
        <div v-for="log in latestGateLogs" :key="log.gateLogId || log.gate_log_id" class="compact-row">
          <span>{{ log.entryTime || log.entry_time || log.exitTime || log.exit_time || '-' }}</span>
          <span>{{ getGateLogPlateText(log) }}</span>
          <span>{{ log.gateName || log.gate_name || '-' }}</span>
          <span>{{ gateText(log.inOutType || log.in_out_type) }}</span>
          <span>{{ log.processResult || log.process_result || '-' }}</span>
        </div>
        <div v-if="latestGateLogs.length === 0" class="compact-row">
          <span>-</span>
          <span>게이트 로그 데이터가 없습니다.</span>
          <span>-</span>
          <span>-</span>
          <span>-</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.control-room {
  display: grid;
  gap: 10px;
  color: #dceaff;
}

.ops-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.ops-card,
.cctv-wall,
.recognition-panel,
.log-panel,
.dark-panel {
  background: #101624;
  border: 1px solid #263353;
  border-radius: 2px;
  box-shadow: none;
}

.ops-card {
  display: grid;
  min-height: 76px;
  gap: 2px;
  padding: 10px 12px;
}

.ops-card span,
.ops-card small {
  color: #91a0c0;
  font-weight: 700;
}

.ops-card strong {
  color: #ffffff;
  font-size: 24px;
}

.ops-card.blue {
  border-left: 4px solid #2d75ae;
}

.ops-card.red {
  border-left: 4px solid #b8403a;
}

.ops-card.green {
  border-left: 4px solid #2f7d57;
}

.ops-card.amber {
  border-left: 4px solid #b47c1c;
}

.control-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 0.65fr);
  gap: 10px;
}

.cctv-wall {
  display: grid;
  min-height: 686px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(308px, 1fr));
  gap: 6px;
  padding: 8px;
}

.cctv-cell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  padding: 8px;
  color: #dceaff;
  background: #020407;
  border: 1px solid #303b5c;
  border-radius: 1px;
  text-align: left;
}

.cctv-cell.active {
  border-color: #f6c34a;
  box-shadow: inset 0 0 0 2px #f6c34a;
}

.cctv-cell.empty {
  background: #070b12;
}

.cctv-cell.out .gate-type {
  background: #b8403a;
}

.gate-head {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.gate-head b,
.gate-head i,
.gate-head em {
  display: inline-flex;
  min-height: 22px;
  align-items: center;
  padding: 3px 7px;
  color: #ffffff;
  border-radius: 1px;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 1.1;
  white-space: nowrap;
}

.gate-meta {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 4px;
}

.gate-head b {
  min-width: 0;
  overflow: hidden;
  background: #23639c;
  border: 1px solid #6e94b7;
  text-overflow: ellipsis;
}

.gate-head i {
  flex: 0 0 auto;
  background: #2f7d57;
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.gate-head em {
  color: #a6e6c3;
  background: #123b2a;
  border: 1px solid #2f7d57;
  font-style: normal;
}

.gate-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 0;
  gap: 6px;
  padding: 2px;
}

.camera-upload {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 4px;
  overflow: hidden;
  padding: 6px;
  color: #91a0c0;
  background: #070b12;
  border: 1px dashed #4d638b;
  cursor: pointer;
  text-align: left;
}

.camera-upload > span {
  color: #dceaff;
  font-size: 11px;
  font-weight: 800;
}

.camera-upload > b {
  display: grid;
  min-height: 0;
  place-items: center;
  color: #57627b;
  font-size: 11px;
  font-weight: 700;
  text-align: center;
}

.camera-upload img {
  width: 100%;
  min-height: 0;
  height: 100%;
  object-fit: cover;
  background: #020407;
}

.camera-upload > small {
  overflow: hidden;
  padding: 3px 5px;
  color: #a6e6c3;
  background: #123b2a;
  border: 1px solid #2f7d57;
  font-size: 9px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.camera-upload > small.review {
  color: #ffd0ce;
  background: #491e22;
  border-color: #b8403a;
}

.camera-upload input { display: none; }

.gate-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
}

.gate-summary small {
  overflow: hidden;
  padding: 4px 6px;
  color: #91a0c0;
  background: #0c1220;
  border: 1px solid #263353;
  font-size: 10px;
  font-weight: 800;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gate-summary small.success {
  color: #a6e6c3;
  background: #123b2a;
  border-color: #2f7d57;
}

.gate-summary small.warning {
  color: #ffe2a3;
  background: #453312;
  border-color: #b47c1c;
}

.gate-summary small.danger {
  color: #ffd0ce;
  background: #491e22;
  border-color: #b8403a;
}

.gate-summary small.idle {
  color: #91a0c0;
}

.recognition-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 10px;
}

.ai-process-zone {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 250px;
  gap: 10px;
  align-items: stretch;
  padding: 8px;
  background: #101624;
  border: 1px solid #263353;
}

.final-decision {
  display: grid;
  gap: 3px;
  padding: 10px;
  border: 1px solid #263353;
}

.final-decision span { font-size: 12px; font-weight: 700; }
.final-decision strong { font-size: 24px; }
.final-decision p { margin: 0; font-size: 12px; font-weight: 700; line-height: 1.35; }
.final-decision.success { color: #a6e6c3; background: #123b2a; border-color: #2f7d57; }
.final-decision.warning { color: #ffd0ce; background: #491e22; border-color: #b8403a; }

.decision-stack {
  display: grid;
  gap: 6px;
}

.process-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.process-steps li {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  color: #91a0c0;
  background: #151d31;
  border: 1px solid #263353;
  font-size: 12px;
  font-weight: 700;
}

.process-steps b {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  place-items: center;
  color: #172033;
  background: #91a0c0;
  border-radius: 999px;
}

.process-steps li.complete { color: #a6e6c3; border-color: #2f7d57; }
.process-steps li.complete b { color: #ffffff; background: #2f7d57; }

.process-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.process-button { min-height: 32px; padding-block: 5px; }
.side-process-actions { margin-top: auto; }
.process-result { grid-column: 1 / -1; margin: 0; padding: 8px 10px; font-size: 13px; font-weight: 700; }
.process-result.success { color: #a6e6c3; background: #123b2a; border: 1px solid #2f7d57; }
.process-result.warning { color: #ffd0ce; background: #491e22; border: 1px solid #b8403a; }

.dark-title {
  background: #0c1220;
  border-color: #263353;
}

.dark-title h2 {
  color: #ffffff;
}

.result-card {
  display: grid;
  gap: 2px;
  padding: 10px;
  background: #e9eef4;
  border: 1px solid #9eb0c1;
  border-radius: 2px;
}

.result-card small {
  color: var(--ink-500);
  font-weight: 700;
}

.result-card strong {
  color: #172033;
  font-size: 28px;
  font-weight: 700;
}

.result-card span {
  color: #1f64b7;
  font-weight: 700;
}

.decision-box,
.process-footer {
  display: grid;
  gap: 6px;
}

.decision-box {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.decision-button {
  min-height: 34px;
  color: #dceaff;
  background: #1a233a;
  border: 1px solid #303b5c;
  border-radius: 2px;
  font-weight: 700;
}

.decision-button.in.selected {
  color: #ffffff;
  background: #1f64b7;
  border-color: #3f8beb;
}

.decision-button.out.selected,
.process-button.out {
  color: #ffffff;
  background: #b8403a;
  border-color: #b8403a;
}

.info-stack {
  display: grid;
  grid-template-rows: minmax(0, 0.85fr) minmax(0, 1.15fr) auto;
  gap: 10px;
  min-height: 0;
}

.info-stack section {
  min-height: 0;
  overflow: auto;
  padding: 8px;
  background: #151d31;
  border: 1px solid #263353;
  border-radius: 2px;
}

.info-stack h3 {
  margin: 0 0 6px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.info-stack dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.info-stack dl div {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 10px;
}

.info-stack dt {
  color: #91a0c0;
  font-size: 12px;
  font-weight: 700;
}

.info-stack dd {
  margin: 0;
  color: #dceaff;
  font-weight: 700;
}

.monitor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.work-lane,
.yard-grid {
  display: grid;
  gap: 6px;
}

.work-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(110px, 1fr) 96px;
  gap: 8px;
  padding: 8px;
  background: #151d31;
  border: 1px solid #263353;
}

.work-row b {
  color: #f6c34a;
}

.work-row span,
.work-row small {
  color: #dceaff;
  font-weight: 700;
}

.yard-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.yard-node {
  display: grid;
  gap: 3px;
  min-height: 74px;
  padding: 8px;
  background: #151d31;
  border: 1px solid #263353;
}

.yard-node b,
.yard-node span {
  color: #91a0c0;
}

.yard-node strong {
  color: #ffffff;
  font-size: 22px;
}

.empty-dark {
  padding: 12px;
  color: #91a0c0;
  background: #151d31;
  border: 1px solid #263353;
  font-weight: 700;
}

.log-panel {
  padding: 8px;
}

.compact-table {
  display: grid;
  gap: 3px;
  overflow-x: auto;
}

.compact-row {
  display: grid;
  min-width: 760px;
  grid-template-columns: 160px 1fr 100px 100px 130px;
  gap: 6px;
  padding: 6px 8px;
  color: #dceaff;
  background: #151d31;
  border: 1px solid #263353;
  border-radius: 1px;
  font-size: 12px;
  font-weight: 700;
}

.compact-row.head {
  color: #91a0c0;
  background: #0c1220;
}

@media (max-width: 1180px) {
  .control-layout,
  .monitor-grid,
  .ai-process-zone {
    grid-template-columns: 1fr;
  }

  .cctv-wall {
    min-height: 520px;
  }
}

@media (max-width: 900px) {
  .ops-strip,
  .yard-grid,
  .process-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 760px) {
  .ops-strip,
  .yard-grid,
  .process-steps,
  .process-actions {
    grid-template-columns: 1fr;
  }

  .cctv-wall {
    min-height: 920px;
    grid-template-columns: 1fr;
    grid-template-rows: repeat(4, 230px);
  }
}

.recognition-info-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  color: #dceaff;
  font-size: 11px;
}

.recognition-info-table th,
.recognition-info-table td {
  padding: 5px 6px;
  border: 1px solid #334364;
  line-height: 1.2;
  text-align: left;
  vertical-align: middle;
  word-break: break-word;
}

.recognition-info-table th {
  width: 42%;
  color: #bdd1e6;
  background: #23324d;
  font-weight: 800;
}

.recognition-info-table td {
  color: #ffffff;
  background: #101624;
  font-weight: 700;
}
</style>
