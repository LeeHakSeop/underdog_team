<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { fetchVehicles } from '@/api/vehicleApi'
import { vehicleTypeLabel } from '@/config/vehicleType'

const loading = ref(false)
const errorMessage = ref('')

const carriers = ref([])
const drivers = ref([])
const vehicles = ref([])

const currentUser = readCurrentUser()

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const myDrivers = computed(() => {
  if (!myCarrier.value) return []
  return drivers.value.filter((driver) => driver.carrierId === myCarrier.value.carrierId)
})

const myVehicles = computed(() => {
  if (!myCarrier.value) return []
  return vehicles.value.filter((vehicle) => vehicle.carrierId === myCarrier.value.carrierId)
})

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [carrierData, driverData, vehicleData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
      fetchVehicles(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
    vehicles.value = vehicleData || []
  } catch (error) {
    errorMessage.value = error.message || '승인 현황 데이터를 불러오지 못했습니다.'
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

    <section class="panel">
      <div class="section-title">
        <h2>승인 현황</h2>
        <span class="status-pill">
          기사 {{ myDrivers.length }}명 / 차량 {{ myVehicles.length }}대
        </span>
      </div>

      <div v-if="loading" class="empty-box">
        불러오는 중...
      </div>

      <div v-else class="approval-grid">
        <article class="approval-card">
          <h3>내 운송사</h3>
          <dl>
            <div>
              <dt>운송사명</dt>
              <dd>{{ myCarrier?.carrierName || '-' }}</dd>
            </div>
            <div>
              <dt>상태</dt>
              <dd>
                <span
                  class="status-pill"
                  :class="{
                    green: myCarrier?.carrierStatus === 'APPROVED',
                    amber: myCarrier?.carrierStatus === 'PENDING',
                    red: myCarrier?.carrierStatus === 'REJECTED',
                  }"
                >
                  {{ myCarrier?.carrierStatus || '-' }}
                </span>
              </dd>
            </div>
          </dl>
        </article>

        <article class="approval-card">
          <h3>소속 기사 승인 현황</h3>

          <div v-if="myDrivers.length === 0" class="empty-mini">
            소속 기사가 없습니다.
          </div>

          <div v-else class="mini-list">
            <div v-for="driver in myDrivers" :key="driver.driverId" class="mini-row">
              <div>
                <b>{{ driver.driverName }}</b>
                <small>{{ driver.driverContact || '-' }}</small>
              </div>
              <span class="status-pill" :class="driver.canEnter ? 'green' : 'red'">
                {{ driver.canEnter ? '출입 가능' : '출입 불가' }}
              </span>
            </div>
          </div>
        </article>

        <article class="approval-card full">
          <h3>소속 차량 승인 현황</h3>

          <div v-if="myVehicles.length === 0" class="empty-mini">
            소속 차량이 없습니다.
          </div>

          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>차량 ID</th>
                  <th>차량번호</th>
                  <th>차량종류</th>
                  <th>톤수</th>
                  <th>등록 승인</th>
                  <th>상태</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="vehicle in myVehicles" :key="vehicle.vehicleId">
                  <td>{{ vehicle.vehicleId }}</td>
                  <td>{{ vehicle.plateNumber }}</td>
                  <td>{{ vehicleTypeLabel(vehicle.vehicleType) }}</td>
                  <td>{{ vehicle.tonnage }}</td>
                  <td>
                    <span class="status-pill" :class="vehicle.isRegistered ? 'green' : 'red'">
                      {{ vehicle.isRegistered ? '승인' : '미승인' }}
                    </span>
                  </td>
                  <td>{{ vehicle.vehicleStatus }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.approval-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.approval-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.approval-card.full {
  grid-column: 1 / -1;
}

.approval-card h3 {
  margin: 0;
  color: var(--ink-900);
  font-size: 16px;
}

.approval-card dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.approval-card dl div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.approval-card dt {
  color: var(--ink-500);
  font-weight: 700;
}

.approval-card dd {
  margin: 0;
  font-weight: 800;
}

.mini-list {
  display: grid;
  gap: 8px;
}

.mini-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.mini-row b,
.mini-row small {
  display: block;
}

.mini-row small {
  margin-top: 3px;
  color: var(--ink-500);
  font-weight: 700;
}

.empty-box,
.empty-mini {
  padding: 20px;
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

@media (max-width: 760px) {
  .approval-grid {
    grid-template-columns: 1fr;
  }

  .approval-card.full {
    grid-column: auto;
  }
}
</style>
