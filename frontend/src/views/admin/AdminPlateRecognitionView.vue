<script setup>
import { computed, onMounted, ref } from 'vue'
import { usePlateRecognitionStore } from '@/stores/adminStore/plateRecognitionStore'
import { useGateLogStore } from '@/stores/adminStore/gateLogStore'
import { vehicleTypeLabel } from '@/config/vehicleType'

const plateRecognitionStore = usePlateRecognitionStore()
const gateLogStore = useGateLogStore()

const selectedOcrType = ref('paddle')
const selectedInOutType = ref('IN')
const tractorFile = ref(null)
const trailerFile = ref(null)
const tractorPreviewUrl = ref('')
const trailerPreviewUrl = ref('')

const tractorResult = computed(() => plateRecognitionStore.tractorResult)
const trailerResult = computed(() => plateRecognitionStore.trailerResult)

const normalizeVehicleType = (vehicleType) => {
  const normalizedType = String(vehicleType || '').trim().toUpperCase()

  if (normalizedType === 'TRACTOR' || vehicleType === '트랙터') return 'TRACTOR'
  if (normalizedType === 'TRAILER' || vehicleType === '트레일러') return 'TRAILER'

  return normalizedType
}

const getVehicleType = (result) => {
  return normalizeVehicleType(result?.vehicle?.vehicleType)
}

const getBooleanText = (value) => {
  if (value === true) {
    return '가능'
  }

  if (value === false) {
    return '불가'
  }

  return '-'
}

const getPassText = (result, expectedType) => {
  if (!result) {
    return '-'
  }

  if (!result.matched) {
    return '불가'
  }

  if (getVehicleType(result) !== normalizeVehicleType(expectedType)) {
    return '불가'
  }

  if (result.needReview) {
    return '불가'
  }

  return '가능'
}

const getAlertMessage = (result, expectedType, label) => {
  if (!result) {
    return `${label} 번호판 인식 전입니다.`
  }

  if (!result.aiResult?.detected) {
    return `${label} 번호판을 인식하지 못했습니다.`
  }

  if (!result.matched) {
    return `${label} 번호판이 등록 차량 정보에 없습니다.`
  }

  if (getVehicleType(result) !== normalizeVehicleType(expectedType)) {
    return `${label} 영역에서 ${vehicleTypeLabel(getVehicleType(result))} 차량이 조회되었습니다.`
  }

  if (result.needReview) {
    return `${label} 번호판 인식 결과 확인이 필요합니다.`
  }

  return `${label} 번호판 확인이 완료되었습니다.`
}

const tractorPassText = computed(() => {
  return getPassText(tractorResult.value, 'TRACTOR')
})

const trailerPassText = computed(() => {
  return getPassText(trailerResult.value, 'TRAILER')
})

const tractorAlertMessage = computed(() => {
  return getAlertMessage(tractorResult.value, 'TRACTOR', '트랙터')
})

const trailerAlertMessage = computed(() => {
  return getAlertMessage(trailerResult.value, 'TRAILER', '트레일러')
})

const getWorkOrder = (result) => {
  return result?.workOrder || result?.trailerWorkInfo || null
}

const tractorWorkOrder = computed(() => getWorkOrder(tractorResult.value))
const trailerWorkOrder = computed(() => getWorkOrder(trailerResult.value))

const workOrderMatch = computed(() => {
  const tractorWorkOrderId = tractorWorkOrder.value?.workOrderId
  const trailerWorkOrderId = trailerWorkOrder.value?.workOrderId

  return Boolean(
    tractorWorkOrderId &&
    trailerWorkOrderId &&
    tractorWorkOrderId === trailerWorkOrderId,
  )
})

const workOrderMismatch = computed(() => {
  const tractorWorkOrderId = tractorWorkOrder.value?.workOrderId
  const trailerWorkOrderId = trailerWorkOrder.value?.workOrderId

  return Boolean(
    tractorWorkOrderId &&
    trailerWorkOrderId &&
    tractorWorkOrderId !== trailerWorkOrderId,
  )
})

const workOrderMatchMessage = computed(() => {
  if (!tractorResult.value || !trailerResult.value) {
    return '트랙터와 트레일러 번호판을 모두 조회하세요.'
  }

  if (!tractorWorkOrder.value || !trailerWorkOrder.value) {
    return '두 차량에 연결된 WorkOrder를 확인할 수 없습니다.'
  }

  if (workOrderMismatch.value) {
    return `WorkOrder가 일치하지 않습니다. 트랙터 #${tractorWorkOrder.value.workOrderId} / 트레일러 #${trailerWorkOrder.value.workOrderId}`
  }

  return `WorkOrder #${tractorWorkOrder.value.workOrderId}가 일치합니다.`
})

const gateDecisionMessage = computed(() => {
  if (workOrderMatch.value && !isReadyForGateProcess.value) {
    const blockedItems = []

    if (tractorPassText.value !== '가능') {
      blockedItems.push(`트랙터: ${tractorAlertMessage.value}`)
    }

    if (trailerPassText.value !== '가능') {
      blockedItems.push(`트레일러: ${trailerAlertMessage.value}`)
    }

    return blockedItems.join(' / ') || '번호판 검증 결과를 확인하세요.'
  }

  return workOrderMatchMessage.value
})

const matchedWorkOrder = computed(() => {
  if (!workOrderMatch.value) return null

  return trailerWorkOrder.value
})

const isReadyForGateProcess = computed(() => {
  return (
    tractorPassText.value === '가능' &&
    trailerPassText.value === '가능' &&
    workOrderMatch.value
  )
})

const gateProcessPayload = computed(() => {
  return {
    tractorVehicleId: tractorResult.value?.vehicle?.vehicleId || null,
    trailerVehicleId: trailerResult.value?.vehicle?.vehicleId || null,
    workOrderId: matchedWorkOrder.value?.workOrderId || null,
    containerId: matchedWorkOrder.value?.containerId || null,
    sectorId: trailerResult.value?.container?.sectorId || trailerResult.value?.trailerWorkInfo?.sectorId || null,
    gateNumber: 'G01',
    gateName: 'AI_GATE',
    inOutType: selectedInOutType.value,
  }
})

const gateProcessMissingItems = computed(() => {
  const payload = gateProcessPayload.value
  const missingItems = []

  if (!payload.tractorVehicleId) {
    missingItems.push('트랙터 차량')
  }

  if (!payload.trailerVehicleId) {
    missingItems.push('트레일러 차량')
  }

  if (!payload.workOrderId) {
    missingItems.push('작업정보')
  }

  if (!workOrderMatch.value) {
    missingItems.push('트랙터·트레일러 공통 WorkOrder')
  }

  if (!payload.containerId) {
    missingItems.push('컨테이너')
  }

  if (!payload.sectorId) {
    missingItems.push('야드 섹터')
  }

  if (tractorPassText.value !== '가능') {
    missingItems.push('트랙터 번호판 검증')
  }

  if (trailerPassText.value !== '가능') {
    missingItems.push('트레일러 번호판 검증')
  }

  return missingItems
})

const canProcessGate = computed(() => {
  return isReadyForGateProcess.value && gateProcessMissingItems.value.length === 0
})

const tractorSummaryText = computed(() => {
  if (!tractorResult.value) {
    return '트랙터 번호판 인식 후 기사와 운송사 정보가 표시됩니다.'
  }

  if (tractorPassText.value !== '가능') {
    return tractorAlertMessage.value
  }

  const driverName = tractorResult.value.driver?.driverName || '기사 정보 없음'
  const carrierName = tractorResult.value.carrier?.carrierName || '운송사 정보 없음'

  return `${driverName} / ${carrierName}`
})

const trailerSummaryText = computed(() => {
  if (!trailerResult.value) {
    return '트레일러 번호판 인식 후 작업정보와 야드 안내가 표시됩니다.'
  }

  if (trailerPassText.value !== '가능') {
    return trailerAlertMessage.value
  }

  const workType = trailerWorkOrder.value?.workType || '작업 유형 없음'
  const containerNumber = trailerResult.value.container?.containerNumber || '컨테이너 정보 없음'
  const sectorName = trailerResult.value.yardSector?.sectorName || '야드 정보 없음'

  return `${workType} / ${containerNumber} / ${sectorName}`
})

onMounted(() => {
  plateRecognitionStore.reset()
})

const selectTractorFile = (event) => {
  const file = event.target.files?.[0]

  tractorFile.value = file || null
  tractorPreviewUrl.value = file ? URL.createObjectURL(file) : ''
}

const selectTrailerFile = (event) => {
  const file = event.target.files?.[0]

  trailerFile.value = file || null
  trailerPreviewUrl.value = file ? URL.createObjectURL(file) : ''
}

const submitTractorRecognize = async () => {
  if (!tractorFile.value) {
    return
  }

  await plateRecognitionStore.recognize(tractorFile.value, selectedOcrType.value, 'tractor')
}

const submitTrailerRecognize = async () => {
  if (!trailerFile.value) {
    return
  }

  await plateRecognitionStore.recognize(trailerFile.value, selectedOcrType.value, 'trailer')
}

const submitGateProcess = async () => {
  if (!canProcessGate.value) {
    return
  }

  try {
    await gateLogStore.processGate(gateProcessPayload.value)
  } catch {
    // store.error를 화면에 표시해 최종 처리 실패 원인을 안내합니다.
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>AI 번호판 인식</h2>
        <span class="status-pill">
          {{ isReadyForGateProcess ? '출입 처리 준비 완료' : '트랙터/트레일러 인식 대기' }}
        </span>
      </div>

      <div class="top-control-area">
        <label class="model-select">
          <span>OCR 모델</span>
          <select v-model="selectedOcrType">
            <option value="paddle">PaddleOCR</option>
            <option value="crnn">CRNN</option>
          </select>
        </label>

        <label class="model-select">
          <span>출입 구분</span>
          <select v-model="selectedInOutType">
            <option value="IN">입차</option>
            <option value="OUT">출차</option>
          </select>
        </label>

        <div :class="['gate-pass-box', isReadyForGateProcess ? 'success' : 'warning']">
          <span>최종 출입 판단</span>
          <strong>{{ isReadyForGateProcess ? '통과' : '불가' }}</strong>
          <p>
            {{
              isReadyForGateProcess
                ? '트랙터와 트레일러 번호판이 같은 WorkOrder에 연결되었습니다.'
                : gateDecisionMessage
            }}
          </p>
        </div>
      </div>

      <ol class="process-steps" aria-label="번호판 출입 처리 단계">
        <li :class="{ complete: tractorResult }"><b>1</b><span>트랙터 인식</span></li>
        <li :class="{ complete: trailerResult }"><b>2</b><span>트레일러 인식</span></li>
        <li :class="{ complete: canProcessGate }"><b>3</b><span>정보 검증</span></li>
        <li :class="{ complete: gateLogStore.processResult?.success }"><b>4</b><span>출입 처리</span></li>
      </ol>

      <div class="gate-process-panel">
        <div>
          <span>최종 출입 처리 요청 데이터</span>
          <strong>{{ canProcessGate ? '처리 가능' : '처리 대기' }}</strong>
          <p>
            {{
              gateProcessMissingItems.length === 0
                ? '트랙터, 트레일러, 작업정보, 컨테이너, 야드 섹터 정보가 준비되었습니다.'
                : `부족한 정보: ${gateProcessMissingItems.join(', ')}`
            }}
          </p>
        </div>

        <div class="gate-process-data">
          <span>트랙터 ID: {{ gateProcessPayload.tractorVehicleId || '-' }}</span>
          <span>트레일러 ID: {{ gateProcessPayload.trailerVehicleId || '-' }}</span>
          <span>작업 ID: {{ gateProcessPayload.workOrderId || '-' }}</span>
          <span>컨테이너 ID: {{ gateProcessPayload.containerId || '-' }}</span>
          <span>섹터 ID: {{ gateProcessPayload.sectorId || '-' }}</span>
          <span>출입 구분: {{ selectedInOutType === 'OUT' ? '출차' : '입차' }}</span>
        </div>

        <div
          v-if="matchedWorkOrder"
          class="matched-work-order"
        >
          <div class="matched-work-order-head">
            <span>일치 WorkOrder 종합 정보</span>
            <strong>#{{ matchedWorkOrder.workOrderId }} · {{ matchedWorkOrder.workType || '-' }}</strong>
          </div>
          <div class="matched-work-order-grid">
            <div><span>작업 상태</span><strong>{{ matchedWorkOrder.workStatus || '-' }}</strong></div>
            <div><span>승인 여부</span><strong>{{ getBooleanText(matchedWorkOrder.isApproved) }}</strong></div>
            <div><span>트랙터</span><strong>{{ tractorResult?.vehicle?.plateNumber || '-' }}</strong></div>
            <div><span>트레일러</span><strong>{{ trailerResult?.vehicle?.plateNumber || '-' }}</strong></div>
            <div><span>기사</span><strong>{{ tractorResult?.driver?.driverName || trailerResult?.driver?.driverName || '-' }}</strong></div>
            <div><span>예약 시간</span><strong>{{ matchedWorkOrder.reservedTime || '-' }}</strong></div>
            <div><span>컨테이너</span><strong>{{ trailerResult?.container?.containerNumber || matchedWorkOrder.containerId || '-' }}</strong></div>
            <div><span>야드 섹터</span><strong>{{ trailerResult?.yardSector?.sectorName || trailerResult?.trailerWorkInfo?.sectorName || '-' }}</strong></div>
          </div>
        </div>

        <div
          v-else-if="workOrderMismatch"
          class="matched-work-order warning"
        >
          <div class="matched-work-order-head">
            <span>WorkOrder 일치 확인</span>
            <strong>통과 불가</strong>
          </div>
          <p>{{ workOrderMatchMessage }}</p>
        </div>

        <button
          class="primary-button gate-process-button"
          type="button"
          :disabled="!canProcessGate || gateLogStore.loading"
          @click="submitGateProcess"
        >
          {{ gateLogStore.loading ? '처리 중' : '최종 출입 처리' }}
        </button>

        <p
          v-if="gateLogStore.processResult"
          :class="['gate-process-result', gateLogStore.processResult.success ? 'success' : 'warning']"
        >
          {{ gateLogStore.processResult.message }}
        </p>

        <p v-if="gateLogStore.error" class="gate-process-result warning">
          {{ gateLogStore.error }}
        </p>
      </div>

      <div class="recognition-layout">
        <div class="recognition-card">
          <div class="section-title sub-title">
            <h3>트랙터 번호판</h3>
            <span class="status-pill">{{ tractorPassText }}</span>
          </div>

          <div :class="['alert-message', tractorPassText === '가능' ? 'success' : 'warning']">
            {{ tractorAlertMessage }}
          </div>

          <label class="file-box" for="tractorImage">
            <b>트랙터 이미지 업로드</b>
            <span>{{ tractorFile?.name || 'JPG, PNG 파일을 선택하세요' }}</span>
            <input id="tractorImage" accept="image/*" type="file" @change="selectTractorFile" />
          </label>

          <img v-if="tractorPreviewUrl" :src="tractorPreviewUrl" alt="선택한 트랙터 이미지" class="preview-image" />

          <button
            class="primary-button"
            type="button"
            :disabled="!tractorFile || plateRecognitionStore.tractorLoading"
            @click="submitTractorRecognize"
          >
            {{ plateRecognitionStore.tractorLoading ? '인식 중' : '트랙터 번호판 인식' }}
          </button>

          <div class="result-grid">
            <div>
              <span>인식 번호판</span>
              <strong>{{ tractorResult?.aiResult?.plateNumber || '-' }}</strong>
            </div>
            <div>
              <span>OCR 모델</span>
              <strong>{{ tractorResult?.aiResult?.ocrType || selectedOcrType }}</strong>
            </div>
            <div>
              <span>인식 성공</span>
              <strong>{{ tractorResult?.aiResult?.detected ? '성공' : '-' }}</strong>
            </div>
            <div>
              <span>통과 여부</span>
              <strong>{{ tractorPassText }}</strong>
            </div>
          </div>

          <div class="summary-panel">
            <span>기사/운송사 조회 요약</span>
            <strong>{{ tractorSummaryText }}</strong>
            <p>
              기사 출입 가능: {{ getBooleanText(tractorResult?.driver?.canEnter) }}
              / 운송사 상태: {{ tractorResult?.carrier?.carrierStatus || '-' }}
            </p>
          </div>

          <table class="data-table">
            <tbody>
              <tr>
                <th>차량 번호</th>
                <td>{{ tractorResult?.vehicle?.plateNumber || '-' }}</td>
              </tr>
              <tr>
                <th>차량 유형</th>
                <td>{{ vehicleTypeLabel(tractorResult?.vehicle?.vehicleType) }}</td>
              </tr>
              <tr>
                <th>등록 여부</th>
                <td>{{ getBooleanText(tractorResult?.vehicle?.isRegistered) }}</td>
              </tr>
              <tr>
                <th>차량 상태</th>
                <td>{{ tractorResult?.vehicle?.vehicleStatus || '-' }}</td>
              </tr>
              <tr>
                <th>기사 이름</th>
                <td>{{ tractorResult?.driver?.driverName || '-' }}</td>
              </tr>
              <tr>
                <th>기사 연락처</th>
                <td>{{ tractorResult?.driver?.driverContact || '-' }}</td>
              </tr>
              <tr>
                <th>기사 출입 가능</th>
                <td>{{ getBooleanText(tractorResult?.driver?.canEnter) }}</td>
              </tr>
              <tr>
                <th>기사 등록 여부</th>
                <td>{{ getBooleanText(tractorResult?.driver?.isRegistered) }}</td>
              </tr>
              <tr>
                <th>운송사</th>
                <td>{{ tractorResult?.carrier?.carrierName || '-' }}</td>
              </tr>
              <tr>
                <th>운송사 연락처</th>
                <td>{{ tractorResult?.carrier?.carrierContact || '-' }}</td>
              </tr>
              <tr>
                <th>운송사 담당자</th>
                <td>{{ tractorResult?.carrier?.managerName || '-' }}</td>
              </tr>
              <tr>
                <th>운송사 상태</th>
                <td>{{ tractorResult?.carrier?.carrierStatus || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="recognition-card">
          <div class="section-title sub-title">
            <h3>트레일러 번호판</h3>
            <span class="status-pill">{{ trailerPassText }}</span>
          </div>

          <div :class="['alert-message', trailerPassText === '가능' ? 'success' : 'warning']">
            {{ trailerAlertMessage }}
          </div>

          <label class="file-box" for="trailerImage">
            <b>트레일러 이미지 업로드</b>
            <span>{{ trailerFile?.name || 'JPG, PNG 파일을 선택하세요' }}</span>
            <input id="trailerImage" accept="image/*" type="file" @change="selectTrailerFile" />
          </label>

          <img v-if="trailerPreviewUrl" :src="trailerPreviewUrl" alt="선택한 트레일러 이미지" class="preview-image" />

          <button
            class="primary-button"
            type="button"
            :disabled="!trailerFile || plateRecognitionStore.trailerLoading"
            @click="submitTrailerRecognize"
          >
            {{ plateRecognitionStore.trailerLoading ? '인식 중' : '트레일러 번호판 인식' }}
          </button>

          <div class="result-grid">
            <div>
              <span>인식 번호판</span>
              <strong>{{ trailerResult?.aiResult?.plateNumber || '-' }}</strong>
            </div>
            <div>
              <span>OCR 모델</span>
              <strong>{{ trailerResult?.aiResult?.ocrType || selectedOcrType }}</strong>
            </div>
            <div>
              <span>인식 성공</span>
              <strong>{{ trailerResult?.aiResult?.detected ? '성공' : '-' }}</strong>
            </div>
            <div>
              <span>통과 여부</span>
              <strong>{{ trailerPassText }}</strong>
            </div>
          </div>

          <div class="summary-panel">
            <span>작업정보 조회 요약</span>
            <strong>{{ trailerSummaryText }}</strong>
            <p>
              작업 승인: {{ getBooleanText(trailerWorkOrder?.isApproved) }}
              / 안내: {{ trailerResult?.yardSector?.guideMessage || '-' }}
            </p>
          </div>

          <table class="data-table">
            <tbody>
              <tr>
                <th>차량 번호</th>
                <td>{{ trailerResult?.vehicle?.plateNumber || '-' }}</td>
              </tr>
              <tr>
                <th>차량 유형</th>
                <td>{{ vehicleTypeLabel(trailerResult?.vehicle?.vehicleType) }}</td>
              </tr>
              <tr>
                <th>등록 여부</th>
                <td>{{ getBooleanText(trailerResult?.vehicle?.isRegistered) }}</td>
              </tr>
              <tr>
                <th>차량 상태</th>
                <td>{{ trailerResult?.vehicle?.vehicleStatus || '-' }}</td>
              </tr>
              <tr>
                <th>작업 유형</th>
                <td>{{ trailerWorkOrder?.workType || '-' }}</td>
              </tr>
              <tr>
                <th>작업 상태</th>
                <td>{{ trailerWorkOrder?.workStatus || '-' }}</td>
              </tr>
              <tr>
                <th>작업 승인</th>
                <td>{{ getBooleanText(trailerWorkOrder?.isApproved) }}</td>
              </tr>
              <tr>
                <th>작업 예약 시간</th>
                <td>{{ trailerWorkOrder?.reservedTime || '-' }}</td>
              </tr>
              <tr>
                <th>컨테이너 번호</th>
                <td>{{ trailerResult?.container?.containerNumber || '-' }}</td>
              </tr>
              <tr>
                <th>컨테이너 크기</th>
                <td>{{ trailerResult?.container?.containerSize || '-' }}</td>
              </tr>
              <tr>
                <th>컨테이너 위치</th>
                <td>{{ trailerResult?.container?.containerLocation || '-' }}</td>
              </tr>
              <tr>
                <th>블록 / 베이 / 로우</th>
                <td>
                  {{ trailerResult?.container?.block || '-' }}
                  / {{ trailerResult?.container?.bay || '-' }}
                  / {{ trailerResult?.container?.rowNo || '-' }}
                </td>
              </tr>
              <tr>
                <th>야드 섹터</th>
                <td>{{ trailerResult?.yardSector?.sectorName || '-' }}</td>
              </tr>
              <tr>
                <th>섹터 상태</th>
                <td>{{ trailerResult?.yardSector?.sectorStatus || '-' }}</td>
              </tr>
              <tr>
                <th>대체 대기장소</th>
                <td>{{ trailerResult?.yardSector?.altWaitingArea || '-' }}</td>
              </tr>
              <tr>
                <th>안내 메시지</th>
                <td>{{ trailerResult?.yardSector?.guideMessage || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <p v-if="plateRecognitionStore.error" class="error-message">
        {{ plateRecognitionStore.error }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.top-control-area {
  display: grid;
  grid-template-columns: 220px 160px minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
  margin-bottom: 14px;
}

.model-select {
  display: grid;
  gap: 6px;
}

.model-select span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.model-select select {
  height: 36px;
  padding: 0 10px;
  color: var(--ink-900);
  background: #fff;
  border: 1px solid var(--line);
  font-weight: 800;
}

.gate-pass-box {
  display: grid;
  gap: 4px;
  padding: 10px 14px;
  border: 1px solid var(--line);
}

.gate-pass-box span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.gate-pass-box strong {
  font-size: 26px;
  font-weight: 900;
}

.gate-pass-box p {
  margin: 0;
  font-size: 13px;
  font-weight: 800;
}

.gate-pass-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.gate-pass-box.success {
  color: #155e38;
  background: #eefaf3;
  border-color: #9dd8b8;
}

.gate-process-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 420px) 160px;
  gap: 12px;
  align-items: stretch;
  margin-bottom: 14px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.gate-process-panel > div:first-child {
  display: grid;
  gap: 4px;
}

.gate-process-panel span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.gate-process-panel strong {
  color: var(--ink-900);
  font-size: 20px;
  font-weight: 900;
}

.gate-process-panel p {
  margin: 0;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.gate-process-data {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 10px;
  align-content: center;
}

.gate-process-button {
  align-self: center;
  min-height: 42px;
}

.gate-process-result {
  grid-column: 1 / -1;
  padding: 10px 12px;
  border: 1px solid var(--line);
  font-size: 15px;
}

.gate-process-result.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.gate-process-result.success {
  color: #155e38;
  background: #eefaf3;
  border-color: #9dd8b8;
}

.matched-work-order {
  grid-column: 1 / -1;
  display: grid;
  gap: 10px;
  padding: 12px;
  background: #eefaf3;
  border: 1px solid #9dd8b8;
}

.matched-work-order.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.matched-work-order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.matched-work-order-head span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.matched-work-order-head strong {
  color: var(--ink-900);
  font-size: 17px;
  font-weight: 900;
}

.matched-work-order-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.matched-work-order-grid div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 9px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.matched-work-order-grid span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.matched-work-order-grid strong {
  overflow-wrap: anywhere;
  color: var(--ink-900);
  font-size: 13px;
}

.matched-work-order p {
  margin: 0;
  font-weight: 800;
  line-height: 1.45;
}

.recognition-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.recognition-card {
  display: grid;
  align-content: start;
  gap: 12px;
}

.file-box {
  display: grid;
  min-height: 90px;
  place-items: center;
  color: var(--blue-700);
  gap: 5px;
  background: #f4f9ff;
  border: 1px dashed #8db5dc;
  border-radius: 4px;
  font-weight: 800;
}

.file-box b {
  color: var(--blue-700);
  font-size: 14px;
}

.file-box span {
  color: var(--ink-500);
  font-size: 12px;
}

.file-box input {
  display: none;
}

.alert-message {
  min-height: 48px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  font-size: 16px;
  font-weight: 900;
  line-height: 1.45;
}

.alert-message.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.alert-message.success {
  color: #155e38;
  background: #eefaf3;
  border-color: #9dd8b8;
}

.preview-image {
  width: 100%;
  max-height: 240px;
  object-fit: contain;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.result-grid div {
  display: grid;
  gap: 4px;
  padding: 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.summary-panel {
  display: grid;
  gap: 6px;
  padding: 12px;
  background: #eef6ff;
  border: 1px solid #b6cfe8;
}

.summary-panel span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.summary-panel strong {
  color: var(--ink-900);
  font-size: 18px;
  font-weight: 900;
}

.summary-panel p {
  margin: 0;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.result-grid span,
.data-table th {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.result-grid strong {
  font-size: 18px;
  font-weight: 900;
}

.sub-title {
  margin-top: 4px;
}

.sub-title h3 {
  margin: 0;
  font-size: 16px;
}

.error-message {
  margin: 0;
  color: #9f1d1d;
  font-weight: 800;
}

.process-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 0 0 14px;
  padding: 0;
  list-style: none;
}

.process-steps li {
  display: flex;
  min-height: 40px;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  color: var(--ink-500);
  background: #f7f9fb;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.process-steps b {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  background: #e3eaf2;
  border-radius: 999px;
}

.process-steps li.complete {
  color: #155e38;
  background: #eefaf3;
  border-color: #9dd8b8;
}

.process-steps li.complete b {
  color: #ffffff;
  background: var(--green-600);
}

@media (max-width: 900px) {
  .top-control-area,
  .gate-process-panel,
  .matched-work-order-grid,
  .recognition-layout,
  .result-grid,
  .process-steps {
    grid-template-columns: 1fr;
  }

  .matched-work-order-head {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
