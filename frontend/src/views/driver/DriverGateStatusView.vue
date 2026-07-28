<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { completeWorkOrder, startWorkOrder } from '@/api/adminApi/workOrderApi'
import { useDriverStore } from '@/stores/driverStore'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)
let refreshTimer = null
const processingId = ref(null)
const actionMessage = ref('')
const actionError = ref('')

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

const statusText = (order) => {
  if (order.workStatus === 'GATE_OUT') return '출차 완료'
  if (order.workStatus === 'COMPLETED') return order.canExit ? '출차 가능' : '출차 대기'
  if (order.workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (order.workStatus === 'GATE_IN') return '입차 완료'
  if (!order.isApproved) return '작업 승인 대기'
  return order.workStatus || '작업 대기'
}

const statusClass = (order) => {
  if (!order.isApproved) return 'amber'
  if (order.workStatus === 'COMPLETED' || order.workStatus === 'GATE_OUT') return 'green'
  return ''
}

const guideText = (order) => {
  if (order.workStatus === 'GATE_IN') {
    return '입차 되었습니다. 해당 야드 섹터로 이동하여 작업을 실시하세요.'
  }

  return order.guideMessage || '게이트 입차 후 번호판 인식 결과와 작업 승인 상태를 확인하세요.'
}

const processWork = async (order, action) => {
  processingId.value = order.workOrderId
  actionMessage.value = ''
  actionError.value = ''

  try {
    const result = action === 'start'
      ? await startWorkOrder(order.workOrderId)
      : await completeWorkOrder(order.workOrderId)

    if (result?.success === false) {
      throw new Error(result.message || '작업 상태를 변경하지 못했습니다.')
    }

    actionMessage.value = action === 'start'
      ? '작업을 시작했습니다.'
      : '작업을 완료했습니다.'

    await driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId)
  } catch (error) {
    actionError.value = error.message || '작업 처리에 실패했습니다.'
  } finally {
    processingId.value = null
  }
}

onMounted(() => {
  if (loginUser.value?.userId) {
    driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId)

    refreshTimer = setInterval(() => {
      if (!driverStore.loading) {
        driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId).catch(() => {})
      }
    }, 5000)
  }
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>작업 현황</h2>
        <span class="status-pill">{{ myWorkOrders.length }}건</span>
      </div>

      <div v-if="loading" class="empty-box">
        작업 현황을 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div v-else-if="myWorkOrders.length === 0" class="empty-box">
        현재 배정된 작업이 없습니다.
      </div>

      <div v-else class="work-list">
        <div v-if="actionMessage" class="action-feedback success" role="status">
          {{ actionMessage }}
        </div>
        <div v-if="actionError" class="action-feedback warning" role="alert">
          {{ actionError }}
        </div>

        <article
          v-for="order in myWorkOrders"
          :key="order.workOrderId"
          class="work-card"
        >
          <div class="work-card-head">
            <div>
              <strong>{{ order.workType || '작업 유형 미정' }}</strong>
              <span>작업 ID {{ order.workOrderId }}</span>
            </div>

            <span class="status-pill" :class="statusClass(order)">
              {{ statusText(order) }}
            </span>
          </div>

          <div class="work-info-grid">
            <div>
              <span>차량 번호</span>
              <b>{{ order.plateNumber || '-' }}</b>
            </div>
            <div>
              <span>트레일러 번호</span>
              <b>{{ order.trailerPlateNumber || '-' }}</b>
            </div>
            <div>
              <span>컨테이너</span>
              <b>{{ order.containerNumber || '-' }}</b>
            </div>
            <div>
              <span>야드 섹터</span>
              <b>{{ order.sectorName || '-' }}</b>
            </div>
            <div>
              <span>예약 시간</span>
              <b>{{ order.reservedTime || '-' }}</b>
            </div>
          </div>

          <div class="guide-line">
            {{ guideText(order) }}
          </div>

          <div
            v-if="order.workStatus === 'GATE_IN' || order.workStatus === 'IN_PROGRESS'"
            class="driver-work-actions"
          >
            <button
              v-if="order.workStatus === 'GATE_IN'"
              class="primary-button"
              type="button"
              :disabled="processingId === order.workOrderId"
              @click="processWork(order, 'start')"
            >
              {{ processingId === order.workOrderId ? '작업 시작 처리 중...' : '작업 시작' }}
            </button>
            <button
              v-else
              class="primary-button complete"
              type="button"
              :disabled="processingId === order.workOrderId"
              @click="processWork(order, 'complete')"
            >
              {{ processingId === order.workOrderId ? '작업 완료 처리 중...' : '작업 완료' }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.work-list {
  display: grid;
  gap: 10px;
}

.work-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.work-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.work-card-head strong,
.work-card-head span {
  display: block;
}

.work-card-head strong {
  font-size: 17px;
}

.work-card-head div span {
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.work-info-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.work-info-grid div,
.guide-line,
.empty-box {
  padding: 10px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.work-info-grid span {
  display: block;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.work-info-grid b {
  display: block;
  margin-top: 3px;
}

.guide-line {
  color: #244766;
  font-weight: 800;
}

.driver-work-actions {
  display: flex;
  justify-content: flex-end;
}

.driver-work-actions .primary-button {
  min-width: 180px;
  min-height: 38px;
}

.driver-work-actions .primary-button.complete {
  background: #23734f;
  border-color: #23734f;
}

.action-feedback {
  padding: 11px 12px;
  border: 1px solid var(--line);
  font-weight: 800;
}

.action-feedback.success {
  color: #12643a;
  background: #eaf7ef;
  border-color: #78c69a;
}

.action-feedback.warning {
  color: #a42626;
  background: #fff1f1;
  border-color: #e19a9a;
}

.empty-box {
  color: var(--ink-500);
  font-weight: 800;
}

.empty-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

@media (max-width: 900px) {
  .work-card-head {
    align-items: stretch;
    flex-direction: column;
  }

  .work-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
