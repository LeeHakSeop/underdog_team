<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)

let refreshTimer = null

// 상세 검색창 표시 여부
const isSearchOpen = ref(false)

// 현재 페이지
const currentPage = ref(1)

// 한 페이지당 작업 수
const pageSize = 3

// 검색창에서 입력 중인 조건
const searchForm = reactive({
  keyword: '',
  workOrderId: '',
  workType: '',
  plateNumber: '',
  trailerPlateNumber: '',
  containerNumber: '',
  sectorName: '',
  reservedTime: '',
  workStatus: '',
  approvalStatus: '',
  guideMessage: '',
})

// 실제 검색에 적용되는 조건
const appliedSearch = reactive({
  keyword: '',
  workOrderId: '',
  workType: '',
  plateNumber: '',
  trailerPlateNumber: '',
  containerNumber: '',
  sectorName: '',
  reservedTime: '',
  workStatus: '',
  approvalStatus: '',
  guideMessage: '',
})

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

// 현재 조회된 작업에서 실제 작업 유형만 선택지로 생성
const workTypeOptions = computed(() => {
  return [
    ...new Set(
      myWorkOrders.value
        .map((order) => order.workType)
        .filter(Boolean),
    ),
  ].sort((a, b) => String(a).localeCompare(String(b), 'ko'))
})

// 현재 조회된 작업에서 실제 야드 섹터만 선택지로 생성
const sectorOptions = computed(() => {
  return [
    ...new Set(
      myWorkOrders.value
        .map((order) => order.sectorName)
        .filter(Boolean),
    ),
  ].sort((a, b) => String(a).localeCompare(String(b), 'ko'))
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
  return myWorkOrders.value.filter((order) => {
    // 전체 통합 검색 대상
    const allValues = [
      order.workOrderId,
      order.workType,
      order.plateNumber,
      order.trailerPlateNumber,
      order.containerNumber,
      order.sectorName,
      order.reservedTime,
      order.workStatus,
      statusText(order),
      order.isApproved ? '승인' : '미승인',
      order.canExit ? '출차 가능' : '출차 불가',
      order.guideMessage,
    ]

    const keywordMatched =
      !appliedSearch.keyword
      || allValues.some((value) =>
        includesValue(value, appliedSearch.keyword),
      )

    const workOrderIdMatched = includesValue(
      order.workOrderId,
      appliedSearch.workOrderId,
    )

    const workTypeMatched =
      !appliedSearch.workType
      || order.workType === appliedSearch.workType

    const plateNumberMatched = includesValue(
      order.plateNumber,
      appliedSearch.plateNumber,
    )

    const trailerPlateMatched = includesValue(
      order.trailerPlateNumber,
      appliedSearch.trailerPlateNumber,
    )

    const containerMatched = includesValue(
      order.containerNumber,
      appliedSearch.containerNumber,
    )

    const sectorMatched =
      !appliedSearch.sectorName
      || order.sectorName === appliedSearch.sectorName

    const reservedTimeMatched =
      !appliedSearch.reservedTime
      || String(order.reservedTime || '').startsWith(
        appliedSearch.reservedTime,
      )

    const workStatusMatched = (() => {
      if (!appliedSearch.workStatus) {
        return true
      }

      if (appliedSearch.workStatus === 'CANCELED_GROUP') {
        return (
          order.workStatus === 'CANCELED'
          || order.workStatus === 'CANCELLED'
        )
      }

      return order.workStatus === appliedSearch.workStatus
    })()

    const guideMessageMatched = includesValue(
      order.guideMessage,
      appliedSearch.guideMessage,
    )

    let approvalMatched = true

    if (appliedSearch.approvalStatus === 'APPROVED') {
      approvalMatched = order.isApproved === true
    }

    if (appliedSearch.approvalStatus === 'NOT_APPROVED') {
      approvalMatched = order.isApproved === false
    }

    return (
      keywordMatched
      && workOrderIdMatched
      && workTypeMatched
      && plateNumberMatched
      && trailerPlateMatched
      && containerMatched
      && sectorMatched
      && reservedTimeMatched
      && workStatusMatched
      && approvalMatched
      && guideMessageMatched
    )
  })
})

// 검색 결과 기준 전체 페이지 수
const totalPages = computed(() => {
  return Math.ceil(filteredWorkOrders.value.length / pageSize)
})

// 현재 페이지에 표시할 검색 결과
const paginatedWorkOrders = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize
  const endIndex = startIndex + pageSize

  return filteredWorkOrders.value.slice(startIndex, endIndex)
})

// 현재 페이지의 첫 작업 번호
const pageStartNumber = computed(() => {
  if (filteredWorkOrders.value.length === 0) {
    return 0
  }

  return (currentPage.value - 1) * pageSize + 1
})

// 현재 페이지의 마지막 작업 번호
const pageEndNumber = computed(() => {
  return Math.min(
    currentPage.value * pageSize,
    filteredWorkOrders.value.length,
  )
})

// 상세 검색창 열기/닫기
const toggleSearch = () => {
  isSearchOpen.value = !isSearchOpen.value
}

// 검색 조건 적용
const applySearch = () => {
  Object.assign(appliedSearch, searchForm)
  currentPage.value = 1
}

// 검색 조건 초기화
const resetSearch = () => {
  Object.keys(searchForm).forEach((key) => {
    searchForm[key] = ''
    appliedSearch[key] = ''
  })

  currentPage.value = 1
}

// 검색 적용 여부
const hasSearchCondition = computed(() => {
  return Object.values(appliedSearch).some((value) => {
    return String(value).trim() !== ''
  })
})

// 페이지 이동
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) {
    return
  }

  currentPage.value = page
}

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

          <p
            v-if="filteredWorkOrders.length > 0"
            class="page-summary"
          >
            전체 {{ filteredWorkOrders.length }}건 중
            {{ pageStartNumber }}~{{ pageEndNumber }}건 표시
          </p>
        </div>

        <div class="title-actions">
          <span
            v-if="hasSearchCondition"
            class="search-result-label"
          >
            검색 결과 {{ filteredWorkOrders.length }}건
          </span>

          <span class="status-pill">
            {{ myWorkOrders.length }}건
          </span>

          <button
            type="button"
            class="search-open-button"
            @click="toggleSearch"
          >
            {{ isSearchOpen ? '검색 닫기' : '검색' }}
          </button>
        </div>
      </div>

      <!-- 상세 검색 영역 -->
      <div
        v-if="isSearchOpen"
        class="search-panel"
      >
        <div class="search-panel-title">
          <div>
            <h3>상세 검색</h3>
            <p>여러 조건을 입력하면 모든 조건을 만족하는 작업만 검색됩니다.</p>
          </div>
        </div>

        <div class="search-grid">
          <label class="search-field search-field-wide">
            <span>전체 검색</span>
            <input
              v-model="searchForm.keyword"
              type="text"
              placeholder="모든 항목에서 검색"
              @keyup.enter="applySearch"
            >
          </label>

          <label class="search-field">
            <span>작업 ID</span>
            <input
              v-model="searchForm.workOrderId"
              type="text"
              placeholder="예: 1024"
              @keyup.enter="applySearch"
            >
          </label>

          <label class="search-field">
            <span>작업 유형</span>
            <select v-model="searchForm.workType">
              <option value="">전체 작업 유형</option>

              <option
                v-for="workType in workTypeOptions"
                :key="workType"
                :value="workType"
              >
                {{ workType }}
              </option>
            </select>
          </label>

          <label class="search-field">
            <span>차량 번호</span>
            <input
              v-model="searchForm.plateNumber"
              type="text"
              placeholder="차량 번호 입력"
              @keyup.enter="applySearch"
            >
          </label>

          <label class="search-field">
            <span>트레일러 번호</span>
            <input
              v-model="searchForm.trailerPlateNumber"
              type="text"
              placeholder="트레일러 번호 입력"
              @keyup.enter="applySearch"
            >
          </label>

          <label class="search-field">
            <span>컨테이너 번호</span>
            <input
              v-model="searchForm.containerNumber"
              type="text"
              placeholder="컨테이너 번호 입력"
              @keyup.enter="applySearch"
            >
          </label>

          <label class="search-field">
            <span>야드 섹터</span>
            <select v-model="searchForm.sectorName">
              <option value="">전체 야드 섹터</option>

              <option
                v-for="sector in sectorOptions"
                :key="sector"
                :value="sector"
              >
                {{ sector }}
              </option>
            </select>
          </label>

          <label class="search-field">
            <span>예약 날짜</span>
            <input
              v-model="searchForm.reservedTime"
              type="date"
            >
          </label>

          <label class="search-field">
            <span>작업 상태</span>
            <select v-model="searchForm.workStatus">
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

          <label class="search-field">
            <span>승인 여부</span>
            <select v-model="searchForm.approvalStatus">
              <option value="">전체</option>
              <option value="APPROVED">승인</option>
              <option value="NOT_APPROVED">미승인</option>
            </select>
          </label>

          <label class="search-field search-field-wide">
            <span>안내 메시지</span>
            <input
              v-model="searchForm.guideMessage"
              type="text"
              placeholder="안내 메시지 내용 검색"
              @keyup.enter="applySearch"
            >
          </label>
        </div>

        <div class="search-actions">
          <button
            type="button"
            class="reset-button"
            @click="resetSearch"
          >
            초기화
          </button>

          <button
            type="button"
            class="apply-search-button"
            @click="applySearch"
          >
            조건 검색
          </button>
        </div>
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

.title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-open-button,
.apply-search-button,
.reset-button,
.pagination-button {
  height: 36px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.search-open-button,
.apply-search-button {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.search-open-button:hover,
.apply-search-button:hover {
  opacity: 0.88;
}

.reset-button {
  color: var(--ink-700);
  background: #ffffff;
}

.search-panel {
  margin-bottom: 18px;
  padding: 18px;
  background: #f7f9fc;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.search-panel-title h3 {
  margin: 0;
  font-size: 17px;
}

.search-panel-title p {
  margin: 5px 0 0;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.search-field {
  display: grid;
  gap: 6px;
}

.search-field-wide {
  grid-column: span 3;
}

.search-field span {
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 900;
}

.search-field input,
.search-field select {
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
  box-sizing: border-box;
  font-family: inherit;
}

.search-field input:focus,
.search-field select:focus {
  outline: none;
  border-color: var(--blue-700);
  box-shadow: 0 0 0 2px rgb(31 111 185 / 12%);
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.search-result-label {
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 900;
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
  color: var(--ink-700);
  background: #ffffff;
}

.pagination-button:hover:not(:disabled),
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

  .section-title {
    align-items: stretch;
    flex-direction: column;
  }

  .title-actions {
    flex-wrap: wrap;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-field-wide {
    grid-column: span 1;
  }

  .search-actions {
    justify-content: stretch;
  }

  .search-actions button {
    flex: 1;
  }
}
</style>