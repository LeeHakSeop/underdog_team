<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)
let refreshTimer = null

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

const statusText = (order) => {
  if (!order.isApproved) {
    return '작업 승인 대기'
  }

  if (order.workStatus === 'COMPLETED') {
    return '작업 완료'
  }

  if (order.workStatus === 'GATE_IN' || order.workStatus === 'IN_PROGRESS') {
    return '작업 진행 중'
  }

  if (order.workStatus === 'GATE_OUT') {
    return '출차 완료'
  }

  return order.workStatus || '작업 대기'
}

const statusClass = (order) => {
  if (!order.isApproved) {
    return 'amber'
  }

  if (order.workStatus === 'COMPLETED' || order.workStatus === 'GATE_OUT') {
    return 'green'
  }

  return ''
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
            {{ order.guideMessage || '게이트 입차 전 번호판 인식 결과와 작업 승인 상태를 확인하세요.' }}
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
