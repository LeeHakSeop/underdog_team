<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'
import { vehicleTypeLabel } from '@/config/vehicleType'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)

let refreshTimer = null

// 현재 작업 목록 페이지
const currentPage = ref(1)

// 한 페이지에 표시할 작업 수
const pageSize = ref(3)


// 간단 검색 조건
const searchKeyword = ref('')
const selectedStatus = ref('')

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

// 가장 첫 번째 작업을 현재 작업으로 표시
const currentWorkOrder = computed(() => {
  return myWorkOrders.value[0] || null
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

// 전체 페이지 수
const totalPages = computed(() => {
  return Math.ceil(filteredWorkOrders.value.length / pageSize.value)
})

// 현재 페이지에 표시할 작업 목록
const paginatedWorkOrders = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value

  return filteredWorkOrders.value.slice(startIndex, endIndex)
})

// 현재 페이지에서 표시되는 작업 번호 범위
const pageStartNumber = computed(() => {
  if (filteredWorkOrders.value.length === 0) {
    return 0
  }

  return (currentPage.value - 1) * pageSize.value + 1
})

const pageEndNumber = computed(() => {
  return Math.min(
    currentPage.value * pageSize.value,
    filteredWorkOrders.value.length,
  )
})

const resetSearch = () => {
  searchKeyword.value = ''
  selectedStatus.value = ''
  pageSize.value = 3
  currentPage.value = 1
}

// 페이지 이동 함수
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

// 작업 목록 개수가 변했을 때 현재 페이지 보정
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

const passStatus = computed(() => {
  const order = currentWorkOrder.value

  if (!order) return '대기'
  if (order.workStatus === 'GATE_OUT') return '출차 완료'

  if (order.workStatus === 'COMPLETED') {
    return order.canExit ? '출차 가능' : '출차 대기'
  }

  if (order.workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (order.workStatus === 'GATE_IN') return '입차 완료'
  if (order.isApproved && order.canEnter) return '입차 가능'

  return '승인 대기'
})

const nextGuide = computed(() => {
  const order = currentWorkOrder.value

  if (!order) return '배정된 작업이 없습니다.'

  if (order.workStatus === 'GATE_OUT') {
    return '출차 처리가 완료되었습니다.'
  }

  if (order.workStatus === 'COMPLETED') {
    return order.canExit
      ? '작업이 완료되었습니다. 출차 게이트로 이동하세요.'
      : '작업이 완료되었습니다. 관리자 확인 후 출차할 수 있습니다.'
  }

  if (order.workStatus === 'IN_PROGRESS') {
    return order.guideMessage || '작업을 진행하세요.'
  }

  if (order.workStatus === 'GATE_IN') {
    return '입차가 완료되었습니다. 지정된 야드 섹터로 이동하세요.'
  }

  if (!order.isApproved) {
    return '관리자 승인 후 게이트 입차가 가능합니다.'
  }

  if (!order.canEnter) {
    return '기사 출입 가능 상태를 운송사 또는 관리자에게 확인하세요.'
  }

  return `${order.sectorName || '지정 섹터'}로 이동 후 안내 메시지를 확인하세요.`
})

const getBooleanText = (value) => {
  if (value === true) return '승인'
  if (value === false) return '미승인'

  return '-'
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
  <div class="page-stack driver-page">
    <section class="panel">
      <div class="section-title">
        <h2>내 작업 안내</h2>
        <span class="status-pill">{{ myWorkOrders.length }}건</span>
      </div>

      <div v-if="loading" class="empty-panel">
        작업 정보를 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-panel warning">
        {{ error }}
      </div>

      <div v-else-if="currentWorkOrder" class="work-summary">
        <div>
          <span>기사</span>
          <strong>{{ currentWorkOrder.driverName || '-' }}</strong>
        </div>

        <div>
          <span>운송사</span>
          <strong>{{ currentWorkOrder.carrierName || '-' }}</strong>
        </div>

        <div>
          <span>차량 번호</span>
          <strong>{{ currentWorkOrder.plateNumber || '-' }}</strong>
        </div>

        <div>
          <span>트레일러 번호</span>
          <strong>
            {{ currentWorkOrder.trailerPlateNumber || '-' }}
          </strong>
        </div>

        <div>
          <span>작업 유형</span>
          <strong>{{ currentWorkOrder.workType || '-' }}</strong>
        </div>

        <div>
          <span>작업 상태</span>
          <strong>{{ currentWorkOrder.workStatus || '-' }}</strong>
        </div>

        <div>
          <span>작업 승인</span>
          <strong>
            {{ getBooleanText(currentWorkOrder.isApproved) }}
          </strong>
        </div>
      </div>

      <div v-else class="empty-panel">
        로그인한 기사에게 배정된 작업 정보가 없습니다.
      </div>
    </section>

    <section
      v-if="currentWorkOrder"
      class="driver-operation-panel"
    >
      <article class="driver-pass-card">
        <span>최종 출입 상태</span>
        <strong>{{ passStatus }}</strong>
        <p>{{ nextGuide }}</p>
      </article>

      <article class="driver-pass-card">
        <span>목적지</span>
        <strong>{{ currentWorkOrder.sectorName || '-' }}</strong>
        <p>
          {{
            currentWorkOrder.guideMessage
              || '야드 안내 메시지가 없습니다.'
          }}
        </p>
      </article>

      <article class="driver-pass-card">
        <span>예약 시간</span>
        <strong>{{ currentWorkOrder.reservedTime || '-' }}</strong>
        <p>
          {{ currentWorkOrder.containerNumber || '-' }}
          /
          {{ currentWorkOrder.workType || '-' }}
        </p>
      </article>
    </section>

    <section
      v-if="currentWorkOrder"
      class="grid-2 driver-grid"
    >
      <article class="panel">
        <div class="section-title">
          <h2>컨테이너 / 야드 안내</h2>

          <span class="status-pill green">
            {{ currentWorkOrder.sectorName || '-' }}
          </span>
        </div>

        <table class="data-table">
          <tbody>
            <tr>
              <th>컨테이너 번호</th>
              <td>{{ currentWorkOrder.containerNumber || '-' }}</td>
            </tr>

            <tr>
              <th>컨테이너 크기</th>
              <td>{{ currentWorkOrder.containerSize || '-' }}</td>
            </tr>

            <tr>
              <th>컨테이너 위치</th>
              <td>{{ currentWorkOrder.containerLocation || '-' }}</td>
            </tr>

            <tr>
              <th>블록 / 베이 / 로우</th>
              <td>
                {{ currentWorkOrder.block || '-' }}
                /
                {{ currentWorkOrder.bay || '-' }}
                /
                {{ currentWorkOrder.rowNo || '-' }}
              </td>
            </tr>

            <tr>
              <th>야드 섹터</th>
              <td>{{ currentWorkOrder.sectorName || '-' }}</td>
            </tr>

            <tr>
              <th>섹터 상태</th>
              <td>{{ currentWorkOrder.sectorStatus || '-' }}</td>
            </tr>

            <tr>
              <th>대체 대기장소</th>
              <td>{{ currentWorkOrder.altWaitingArea || '-' }}</td>
            </tr>

            <tr>
              <th>안내 메시지</th>
              <td>{{ currentWorkOrder.guideMessage || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>기사 / 차량 정보</h2>

          <span class="status-pill">
            {{ vehicleTypeLabel(currentWorkOrder.vehicleType) }}
          </span>
        </div>

        <table class="data-table">
          <tbody>
            <tr>
              <th>기사 연락처</th>
              <td>{{ currentWorkOrder.driverContact || '-' }}</td>
            </tr>

            <tr>
              <th>기사 출입 가능</th>
              <td>
                {{ currentWorkOrder.canEnter ? '가능' : '불가' }}
              </td>
            </tr>

            <tr>
              <th>운송사 연락처</th>
              <td>{{ currentWorkOrder.carrierContact || '-' }}</td>
            </tr>

            <tr>
              <th>차량 유형</th>
              <td>
                {{ vehicleTypeLabel(currentWorkOrder.vehicleType) }}
              </td>
            </tr>

            <tr>
              <th>트레일러 번호</th>
              <td>
                {{ currentWorkOrder.trailerPlateNumber || '-' }}
              </td>
            </tr>

            <tr>
              <th>차량 상태</th>
              <td>{{ currentWorkOrder.vehicleStatus || '-' }}</td>
            </tr>

            <tr>
              <th>예약 시간</th>
              <td>{{ currentWorkOrder.reservedTime || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>작업 목록</h2>
          <p class="pagination-summary">
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
            placeholder="작업 번호, 작업 유형, 트레일러, 컨테이너, 야드 섹터"
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

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>작업 유형</th>
              <th>트레일러</th>
              <th>컨테이너</th>
              <th>야드 섹터</th>
              <th>예약 시간</th>
              <th>상태</th>
              <th>승인</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="order in paginatedWorkOrders"
              :key="order.workOrderId"
            >
              <td>{{ order.workOrderId }}</td>
              <td>{{ order.workType || '-' }}</td>
              <td>{{ order.trailerPlateNumber || '-' }}</td>
              <td>{{ order.containerNumber || '-' }}</td>
              <td>{{ order.sectorName || '-' }}</td>
              <td>{{ order.reservedTime || '-' }}</td>

              <td>
                <span class="status-pill">
                  {{ order.workStatus || '-' }}
                </span>
              </td>

              <td>
                {{ getBooleanText(order.isApproved) }}
              </td>
            </tr>

            <tr v-if="myWorkOrders.length === 0">
              <td colspan="8">
                조회된 작업 정보가 없습니다.
              </td>
            </tr>

            <tr v-else-if="filteredWorkOrders.length === 0">
              <td colspan="8">
                검색 조건에 맞는 작업이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이지네이션 -->
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
    </section>
  </div>
</template>

<style scoped>
.driver-page {
  max-width: 1180px;
}

.work-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.work-summary div,
.empty-panel {
  display: grid;
  gap: 4px;
  padding: 14px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.work-summary span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.work-summary strong {
  font-size: 17px;
  font-weight: 900;
}

.empty-panel {
  color: var(--ink-500);
  font-weight: 800;
}

.empty-panel.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.driver-grid {
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.8fr);
}

.driver-operation-panel {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1fr;
  gap: 10px;
}

.driver-pass-card {
  display: grid;
  gap: 7px;
  padding: 14px;
  background: #f7f9fb;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue-700);
  border-radius: 2px;
}

.driver-pass-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.driver-pass-card strong {
  color: var(--ink-900);
  font-size: 22px;
  font-weight: 900;
}

.driver-pass-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
}

/* 작업 목록 표시 범위 문구 */
.pagination-summary {
  margin: 5px 0 0;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

/* 페이지네이션 전체 영역 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 20px;
}

/* 페이지네이션 버튼 */
.pagination-button {
  min-width: 38px;
  height: 38px;
  padding: 0 12px;
  color: var(--ink-700);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color 0.2s,
    border-color 0.2s,
    color 0.2s;
}

.pagination-button:hover:not(:disabled) {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

/* 현재 선택된 페이지 */
.pagination-button.active {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

/* 이동할 수 없는 이전/다음 버튼 */
.pagination-button:disabled {
  color: #a8b0bb;
  background: #f3f5f7;
  cursor: not-allowed;
  opacity: 0.7;
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

  .work-summary,
  .driver-operation-panel,
  .driver-grid {
    grid-template-columns: 1fr;
  }

  .pagination-button {
    min-width: 34px;
    height: 34px;
    padding: 0 9px;
  }
}</style>