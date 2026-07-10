<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { useLogisticsData } from '@/composables/useLogisticsData'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'

const currentUser = readCurrentUser()
const workOrderStore = useWorkOrderStore()
let refreshTimer = null
const { availableDrivers, carriers, getContainerNumber, getSectorByContainerId, workOrders } = useLogisticsData()

const myCarrier = computed(() => {
  return carriers.value.find((carrier) => carrier.user_id === currentUser?.userId)
})

const carrierOrders = computed(() => {
  if (!myCarrier.value) return []
  return workOrders.value.filter((order) => order.carrier_id === myCarrier.value.carrier_id)
})

const pendingOrders = computed(() => carrierOrders.value.filter((order) => !order.is_approved))
const approvedOrders = computed(() => carrierOrders.value.filter((order) => order.is_approved))

onMounted(() => {
  refreshTimer = setInterval(() => {
    if (!workOrderStore.loading) {
      workOrderStore.loadWorkOrders().catch(() => {})
    }
  }, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})

/* CODEX CARRIER DASHBOARD START */
const actionCards = computed(() => [
  {
    title: '기사 승인',
    count: availableDrivers.value.length,
    text: '출입 가능한 기사 현황을 확인합니다.',
    path: '/carrier/driver-approval',
  },
  {
    title: '트레일러 배정',
    count: pendingOrders.value.length,
    text: '승인 대기 작업의 트레일러 배정 상태를 확인합니다.',
    path: '/carrier/vehicle-register',
  },
  {
    title: '운송 요청',
    count: carrierOrders.value.length,
    text: '관리자에게 전달된 작업 요청 흐름을 확인합니다.',
    path: '/carrier/requests',
  },
])
/* CODEX CARRIER DASHBOARD END */

</script>

<template>
  <div class="page-stack">
    <section class="grid-4">
      <article class="metric-card">
        <span>승인 대기</span>
        <strong>{{ pendingOrders.length }}</strong>
        <small>관리자 검토 중</small>
      </article>
      <article class="metric-card">
        <span>출입 가능 기사</span>
        <strong>{{ availableDrivers.length }}</strong>
        <small>등록 기사 기준</small>
      </article>
      <article class="metric-card">
        <span>승인 작업</span>
        <strong>{{ approvedOrders.length }}</strong>
        <small>예약 기준</small>
      </article>
      <article class="metric-card">
        <span>전체 작업</span>
        <strong>{{ carrierOrders.length }}</strong>
        <small>운송사 기준</small>
      </article>
    </section>


    <!-- CODEX CARRIER DASHBOARD START -->
    <section class="grid-3 carrier-action-grid">
      <RouterLink v-for="card in actionCards" :key="card.title" class="carrier-action-card" :to="card.path">
        <span>{{ card.title }}</span>
        <strong>{{ card.count }}</strong>
        <p>{{ card.text }}</p>
      </RouterLink>
    </section>
    <!-- CODEX CARRIER DASHBOARD END -->

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>최근 운송 요청</h2>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>작업번호</th>
                <th>컨테이너</th>
                <th>섹터</th>
                <th>상태</th>
                <th>예약</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in carrierOrders" :key="order.work_order_id">
                <td>{{ order.work_order_id }}</td>
                <td>{{ getContainerNumber(order.container_id) }}</td>
                <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
                <td><span class="status-pill">{{ order.work_status }}</span></td>
                <td>{{ order.reserved_time }}</td>
              </tr>
              <tr v-if="carrierOrders.length === 0">
                <td colspan="5">최근 운송 요청이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사 후보</h2>
          <span class="status-pill green">can_enter=Y</span>
        </div>
        <div class="driver-list">
          <div v-for="driver in availableDrivers" :key="driver.driver_id" class="driver-row">
            <div>
              <b>{{ driver.driver_name }}</b>
              <span>{{ driver.driver_contact || '-' }}</span>
            </div>
            <span class="status-pill green">출입 가능</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>

/* CODEX CARRIER DASHBOARD START */
.carrier-action-card {
  display: grid;
  gap: 6px;
  min-height: 104px;
  padding: 13px;
  background: #f7f9fb;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue-700);
  border-radius: 2px;
}

.carrier-action-card span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.carrier-action-card strong {
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 900;
}

.carrier-action-card p {
  margin: 0;
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 800;
}
/* CODEX CARRIER DASHBOARD END */

.driver-list {
  display: grid;
  gap: 10px;
}

.driver-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.driver-row b,
.driver-row span {
  display: block;
}

.driver-row b {
  margin-bottom: 4px;
}

.driver-row div span {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}
</style>
