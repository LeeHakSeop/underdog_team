<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)

let refreshTimer = null

// 간단 검색 조건
const searchKeyword = ref('')
const selectedStatus = ref('')

// 현재 페이지
const currentPage = ref(1)

// 한 페이지에 표시할 작업 수
const pageSize = ref(3)

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

// 화면에 표시할 작업 상태 이름
const workStatusLabel = (status) => {
  const labels = {
    WAITING: '작업 대기',
    GATE_IN: '입차 완료',
    IN_PROGRESS: '작업 진행 중',
    COMPLETED: '작업 완료',
    GATE_OUT: '출차 완료',
    CANCELED: '취소',
    CANCELLED: '취소',
  }

  return labels[status] || status
}

// 현재 조회된 작업에서 실제 상태만 선택지로 생성
// CANCELED와 CANCELLED는 화면에서 '취소' 하나로 묶음
const workStatusOptions = computed(() => {
  const normalizedStatuses = myWorkOrders.value
    .map((order) => order.workStatus)
    .filter(Boolean)
    .map((status) => {
      if (status === 'CANCELED' || status === 'CANCELLED') {
        return 'CANCELED_GROUP'
      }

      return status
    })

  return [...new Set(normalizedStatuses)]
    .map((value) => ({
      value,
      label: value === 'CANCELED_GROUP'
        ? '취소'
        : workStatusLabel(value),
    }))
    .sort((a, b) => a.label.localeCompare(b.label, 'ko'))
})

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

// 검색 비교용 문자열 변환
const normalize = (value) => {
  return String(value ?? '').trim().toLowerCase()
}

// 특정 값에 검색어가 포함되는지 확인
const includesValue = (value, keyword) => {
  if (!keyword) {
    return true
  }

  return normalize(value).includes(normalize(keyword))
}

// 검색 조건이 적용된 작업 목록
const filteredWorkOrders = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  return myWorkOrders.value.filter((order) => {
    const keywordMatched =
      !keyword
      || [
        order.workOrderId,
        order.workType,
        order.plateNumber,
        order.trailerPlateNumber,
        order.containerNumber,
        order.sectorName,
        order.reservedTime,
        order.workStatus,
        workStatusLabel(order.workStatus),
        order.driverName,
        order.carrierName,
        order.guideMessage,
        order.isApproved ? '승인' : '미승인',
      ].some((value) =>
        String(value ?? '').toLowerCase().includes(keyword),
      )

    const statusMatched =
      !selectedStatus.value
      || (
        selectedStatus.value === 'CANCELED_GROUP'
          ? order.workStatus === 'CANCELED'
            || order.workStatus === 'CANCELLED'
          : order.workStatus === selectedStatus.value
      )

    return keywordMatched && statusMatched
  })
})

// 검색 결과 기준 전체 페이지 수
const totalPages = computed(() => {
  return Math.ceil(filteredWorkOrders.value.length / pageSize.value)
})

// 현재 페이지에 표시할 검색 결과
const paginatedWorkOrders = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value

  return filteredWorkOrders.value.slice(startIndex, endIndex)
})

// 현재 페이지의 첫 작업 번호
const pageStartNumber = computed(() => {
  if (filteredWorkOrders.value.length === 0) {
    return 0
  }

  return (currentPage.value - 1) * pageSize.value + 1
})

// 현재 페이지의 마지막 작업 번호
const pageEndNumber = computed(() => {
  return Math.min(
    currentPage.value * pageSize.value,
    filteredWorkOrders.value.length,
  )
})

// 상세 검색창 열기/닫기
const resetSearch = () => {
  searchKeyword.value = ''
  selectedStatus.value = ''
  pageSize.value = 3
  currentPage.value = 1
}

// 페이지 이동
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) {
    return
  }

  currentPage.value = page
}

watch(
  [searchKeyword, selectedStatus, pageSize],
  () => {
    currentPage.value = 1
  },
)

// 검색 결과가 변경되면 현재 페이지를 보정
watch(
  () => filteredWorkOrders.value.length,
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
          <p class="page-summary">
            {{ pageStartNumber }} - {{ pageEndNumber }} / {{ filteredWorkOrders.length }}건
          </p>
        </div>

        <span class="status-pill">
          {{ myWorkOrders.length }}건
        </span>
      </div>

      <div class="simple-search-bar">
        <label class="simple-search-field">
          <span>조회</span>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="작업 번호, 차량, 트레일러, 기사, 컨테이너, 야드 섹터"
          >
        </label>

        <label class="simple-select-field">
          <span>상태</span>
          <select v-model="selectedStatus">
            <option value="">전체 상태</option>
            <option
              v-for="status in workStatusOptions"
              :key="status.value"
              :value="status.value"
            >
              {{ status.label }}
            </option>
          </select>
        </label>

        <label class="simple-select-field display-field">
          <span>표시</span>
          <select v-model.number="pageSize">
            <option :value="1">1건</option>
            <option :value="2">2건</option>
            <option :value="3">3건</option>
          </select>
        </label>

        <button
          type="button"
          class="simple-reset-button"
          @click="resetSearch"
        >
          초기화
        </button>
      </div>

      <div
        v-if="loading && myWorkOrders.length === 0"
        class="empty-box"
      >
        작업 현황을 불러오는 중입니다.
      </div>

      <div
        v-else-if="error"
        class="empty-box warning"
      >
        {{ error }}
      </div>

      <div
        v-else-if="myWorkOrders.length === 0"
        class="empty-box"
      >
        현재 배정된 작업이 없습니다.
      </div>

      <div
        v-else-if="filteredWorkOrders.length === 0"
        class="empty-box"
      >
        검색 조건에 맞는 작업이 없습니다.
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
            class="pagination-button"
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

.simple-search-bar {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 150px 90px auto;
  align-items: end;
  gap: 8px;
  margin-bottom: 12px;
}

.simple-search-field,
.simple-select-field {
  display: grid;
  gap: 5px;
}

.simple-search-field span,
.simple-select-field span {
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
}

.simple-search-field input,
.simple-select-field select {
  width: 100%;
  height: 32px;
  padding: 0 9px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  box-sizing: border-box;
  font-family: inherit;
}

.simple-search-field input:focus,
.simple-select-field select:focus {
  outline: none;
  border-color: var(--blue-700);
}

.simple-reset-button {
  height: 32px;
  padding: 0 12px;
  color: var(--blue-700);
  background: #ffffff;
  border: 1px solid #9dbddd;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.simple-reset-button:hover {
  background: #eef5fb;
}

@media (max-width: 900px) {
  .simple-search-bar {
    grid-template-columns: 1fr;
  }

  .work-card-head {
    align-items: stretch;
    flex-direction: column;
  }

  .work-info-grid {
    grid-template-columns: 1fr;
  }
}</style>
