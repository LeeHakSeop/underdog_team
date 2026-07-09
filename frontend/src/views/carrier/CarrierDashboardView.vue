<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { containers, getContainerNumber, getSectorByContainerId, workOrders } from '../../data/dbData'

const loading = ref(false)
const errorMessage = ref('')

const carriers = ref([])
const drivers = ref([])

const currentUser = readCurrentUser()

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const myDrivers = computed(() => {
  if (!myCarrier.value) return []
  return drivers.value.filter((driver) => driver.carrierId === myCarrier.value.carrierId)
})

const availableMyDrivers = computed(() =>
  myDrivers.value.filter((driver) => driver.canEnter === true),
)

const carrierOrders = computed(() => workOrders.slice(0, 2))

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [carrierData, driverData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
  } catch (error) {
    errorMessage.value = error.message || '운송사 대시보드 데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section v-if="errorMessage" class="panel error-panel">
      {{ errorMessage }}
    </section>

    <section class="grid-4">
      <article class="metric-card">
        <span>내 운송사</span>
        <strong>{{ myCarrier?.carrierName || '-' }}</strong>
        <small>{{ myCarrier?.carrierStatus || '조회 중' }}</small>
      </article>

      <article class="metric-card">
        <span>소속 기사</span>
        <strong>{{ myDrivers.length }}</strong>
        <small>DB 기준</small>
      </article>

      <article class="metric-card">
        <span>출입 가능 기사</span>
        <strong>{{ availableMyDrivers.length }}</strong>
        <small>승인된 기사</small>
      </article>

      <article class="metric-card">
        <span>컨테이너</span>
        <strong>{{ containers.length }}</strong>
        <small>컨테이너 기준</small>
      </article>
    </section>

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>최근 운송 요청</h2>
        </div>

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>작업 ID</th>
                <th>컨테이너</th>
                <th>섹터</th>
                <th>작업 상태</th>
                <th>예약 시간</th>
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
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>소속 기사</h2>
          <span class="status-pill green">DB 조회</span>
        </div>

        <div v-if="loading" class="empty-box">
          불러오는 중...
        </div>

        <div v-else-if="myDrivers.length === 0" class="empty-box">
          소속 기사가 없습니다.
        </div>

        <div v-else class="driver-list">
          <div v-for="driver in myDrivers" :key="driver.driverId" class="driver-row">
            <div>
              <b>{{ driver.driverName }}</b>
              <span>
                연락처 {{ driver.driverContact || '-' }} /
                운송사 ID {{ driver.carrierId }}
              </span>
            </div>

            <span class="status-pill" :class="driver.canEnter ? 'green' : 'red'">
              {{ driver.canEnter ? '출입 가능' : '출입 불가' }}
            </span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
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

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.error-panel {
  color: #991b1b;
  background: #fff1f1;
  border-color: #fecaca;
}
</style>