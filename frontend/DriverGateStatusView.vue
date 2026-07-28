<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)

let refreshTimer = null

// 현재 페이지 번호
const currentPage = ref(1)

// 한 페이지에 표시할 작업 개수
const pageSize = 3

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

// 전체 페이지 수
const totalPages = computed(() => {
  return Math.ceil(myWorkOrders.value.length / pageSize)
})

// 현재 페이지에 표시할 작업 목록
const paginatedWorkOrders = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize
  const endIndex = startIndex + pageSize

  return myWorkOrders.value.slice(startIndex, endIndex)
})

// 현재 페이지에 표시되는 첫 번째 작업 번호
const pageStartNumber = computed(() => {
  if (myWorkOrders.value.length === 0) {
    return 0
  }

  return (currentPage.value - 1) * pageSize + 1
})

// 현재 페이지에 표시되는 마지막 작업 번호
const pageEndNumber = computed(() => {
  return Math.min(
    currentPage.value * pageSize,
    myWorkOrders.value.length,
  )
})

// 페이지 이동
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) {
    return
  }

  currentPage.value = page
}

// 작업 목록이 갱신되면서 페이지 수가 줄어든 경우 현재 페이지 보정
watch(
  () => myWorkOrders.value.length,
  () => {
    if (totalPages.value === 0) {
      currentPage.value = 1
      return
    }

    if (currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value
    }
  },
)

const statusText = (order) => {
  if (order.workStatus === 'GATE_OUT') {
    return '출차 완료'
  }

  if (order.workStatus === 'COMPLETED') {
    return order.canExit ? '출차 가능' : '출차 대기'
  }

  if (order.workStatus === 'IN_PROGRESS') {
    return '작업 진행 중'
  }

  if (order.workStatus === 'GATE_IN') {
    return '입차 완료'
  }

  if (!order.isApproved) {
    return '작업 승인 대기'
  }

  return order.workStatus || '작업 대기'
}

const statusClass = (order) => {
  if (!order.isApproved) {
    return 'amber'
  }

  if (
    order.workStatus === 'COMPLETED'
    || order.workStatus === 'GATE_OUT'
  ) {
    return 'green'
  }

  return ''
}

onMounted(() => {
  if (loginUser.value?.userId) {
    driverStore.loadMyWorkOrdersByUserId(loginUser.value.userId)

    refreshTimer = setInterval(() => {
      if (!driverStore.loading) {
        driverStore
          .loadMyWorkOrdersByUserId(loginUser.value.userId)
          .catch(() => {})
      }
    }, 5000)
  }
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>작업 현황</h2>

          <p
            v-if="myWorkOrders.length > 0"
            class="page-summary"
          >
            전체 {{ myWorkOrders.length }}건 중
            {{ pageStartNumber }}~{{ pageEndNumber }}건 표시
          </p>
        </div>

        <span class="status-pill">
          {{ myWorkOrders.length }}건
        </span>
      </div>

      <div v-if="loading && myWorkOrders.length === 0" class="empty-box">
        작업 현황을 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div v-else-if="myWorkOrders.length === 0" class="empty-box">
        현재 배정된 작업이 없습니다.
      </div>

      <template v-else>
        <div class="work-list">
          <article
            v-for="order in paginatedWorkOrders"
            :key="order.workOrderId"
            class="work-card"
          >
            <div class="work-card-head">
              <div>
                <strong>
                  {{ order.workType || '작업 유형 미정' }}
                </strong>

                <span>
                  작업 ID {{ order.workOrderId }}
                </span>
              </div>

              <span
                class="status-pill"
                :class="statusClass(order)"
              >
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
              {{
                order.guideMessage
                  || '게이트 입차 후 번호판 인식 결과와 작업 승인 상태를 확인하세요.'
              }}
            </div>
          </article>
        </div>

        <div
          v-if="totalPages > 1"
          class="pagination"
        >
          <button
            type="button"
            class="pagination-button"
            :disabled="currentPage === 1"
            @click="changePage(currentPage - 1)"
          >
            이전
          </button>

          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            class="pagination-button page-number"
            :class="{ active: currentPage === page }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>

          <button
            type="button"
            class="pagination-button"
            :disabled="currentPage === totalPages"
            @click="changePage(currentPage + 1)"
          >
            다음
          </button>
        </div>
      </template>
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

.empty-box {
  color: var(--ink-500);
  font-weight: 800;
}

.empty-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.page-summary {
  margin: 4px 0 0;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 18px;
}

.pagination-button {
  min-width: 38px;
  height: 36px;
  padding: 0 12px;
  color: var(--ink-700);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 3px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.pagination-button:hover:not(:disabled) {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.pagination-button.active {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.pagination-button:disabled {
  color: #9ba5af;
  background: #f2f4f6;
  cursor: not-allowed;
  opacity: 0.7;
}

@media (max-width: 900px) {
  .work-card-head {
    align-items: stretch;
    flex-direction: column;
  }

  .work-info-grid {
    grid-template-columns: 1fr;
  }

  .pagination-button {
    min-width: 34px;
    height: 34px;
    padding: 0 9px;
  }
}
</style>