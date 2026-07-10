<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'

const driverStore = useDriverStore()
const { myWorkOrders, loading, error } = storeToRefs(driverStore)

const loginUser = computed(() => {
  return JSON.parse(
    localStorage.getItem('portGateUser') || 'null',
  )
})

const currentWorkOrder = computed(() => {
  return myWorkOrders.value[0] || null
})

const getBooleanText = (value) => {
  if (value === true) {
    return '승인'
  }

  if (value === false) {
    return '미승인'
  }

  return '-'
}

onMounted(() => {
  if (loginUser.value?.userId) {
    driverStore.loadMyWorkOrdersByUserId(
      loginUser.value.userId,
    )
  }
})
</script>

<template>
  <div class="page-stack driver-page">
    <section class="panel">
      <div class="section-title">
        <h2>내 작업 안내</h2>
        <span class="status-pill">
          {{ myWorkOrders.length }}건
        </span>
      </div>

      <div v-if="loading" class="empty-panel">
        작업정보를 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-panel warning">
        {{ error }}
      </div>

      <div
        v-else-if="currentWorkOrder"
        class="work-summary"
      >
        <div>
          <span>기사</span>
          <strong>
            {{ currentWorkOrder.driverName || '-' }}
          </strong>
        </div>

        <div>
          <span>운송사</span>
          <strong>
            {{ currentWorkOrder.carrierName || '-' }}
          </strong>
        </div>

        <div>
          <span>차량 번호</span>
          <strong>
            {{ currentWorkOrder.plateNumber || '-' }}
          </strong>
        </div>

        <div>
          <span>작업 유형</span>
          <strong>
            {{ currentWorkOrder.workType || '-' }}
          </strong>
        </div>

        <div>
          <span>작업 상태</span>
          <strong>
            {{ currentWorkOrder.workStatus || '-' }}
          </strong>
        </div>

        <div>
          <span>작업 승인</span>
          <strong>
            {{ getBooleanText(currentWorkOrder.isApproved) }}
          </strong>
        </div>
      </div>

      <div v-else class="empty-panel">
        로그인한 기사에게 배정된 작업정보가 없습니다.
      </div>
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
              <td>
                {{ currentWorkOrder.containerNumber || '-' }}
              </td>
            </tr>

            <tr>
              <th>컨테이너 크기</th>
              <td>
                {{ currentWorkOrder.containerSize || '-' }}
              </td>
            </tr>

            <tr>
              <th>컨테이너 위치</th>
              <td>
                {{
                  currentWorkOrder.containerLocation ||
                  '-'
                }}
              </td>
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
              <td>
                {{ currentWorkOrder.sectorName || '-' }}
              </td>
            </tr>

            <tr>
              <th>섹터 상태</th>
              <td>
                {{ currentWorkOrder.sectorStatus || '-' }}
              </td>
            </tr>

            <tr>
              <th>대체 대기장소</th>
              <td>
                {{
                  currentWorkOrder.altWaitingArea ||
                  '-'
                }}
              </td>
            </tr>

            <tr>
              <th>안내 메시지</th>
              <td>
                {{ currentWorkOrder.guideMessage || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>기사 / 차량 정보</h2>

          <span class="status-pill">
            {{ currentWorkOrder.vehicleType || '-' }}
          </span>
        </div>

        <table class="data-table">
          <tbody>
            <tr>
              <th>기사 연락처</th>
              <td>
                {{ currentWorkOrder.driverContact || '-' }}
              </td>
            </tr>

            <tr>
              <th>기사 출입 가능</th>
              <td>
                {{
                  currentWorkOrder.canEnter
                    ? '가능'
                    : '불가'
                }}
              </td>
            </tr>

            <tr>
              <th>운송사 연락처</th>
              <td>
                {{
                  currentWorkOrder.carrierContact ||
                  '-'
                }}
              </td>
            </tr>

            <tr>
              <th>차량 유형</th>
              <td>
                {{ currentWorkOrder.vehicleType || '-' }}
              </td>
            </tr>

            <tr>
              <th>차량 상태</th>
              <td>
                {{
                  currentWorkOrder.vehicleStatus ||
                  '-'
                }}
              </td>
            </tr>

            <tr>
              <th>예약 시간</th>
              <td>
                {{ currentWorkOrder.reservedTime || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>작업 목록</h2>

        <span class="status-pill">
          {{ myWorkOrders.length }}건
        </span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>작업 유형</th>
              <th>컨테이너</th>
              <th>야드 섹터</th>
              <th>예약 시간</th>
              <th>상태</th>
              <th>승인</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="order in myWorkOrders"
              :key="order.workOrderId"
            >
              <td>{{ order.workOrderId }}</td>
              <td>{{ order.workType || '-' }}</td>
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
              <td colspan="7">
                조회된 작업정보가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
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
  grid-template-columns:
    minmax(0, 1fr)
    minmax(320px, 0.8fr);
}

@media (max-width: 900px) {
  .work-summary,
  .driver-grid {
    grid-template-columns: 1fr;
  }
}
</style>
