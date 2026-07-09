<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { containers } from '../../data/dbData'

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
    errorMessage.value = error.message || '운송 요청 데이터를 불러오지 못했습니다.'
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

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>운송 요청 등록</h2>
          <span class="status-pill">
            {{ myCarrier?.carrierName || '운송사 조회 중' }}
          </span>
        </div>

        <form class="form-grid">
          <div class="field">
            <label for="containerId">컨테이너</label>
            <select id="containerId">
              <option v-for="container in containers" :key="container.container_id" :value="container.container_id">
                {{ container.container_number }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="workType">작업 유형</label>
            <select id="workType">
              <option value="LOAD_OUT">반출 상차</option>
              <option value="UNLOAD_IN">반입 하차</option>
            </select>
          </div>

          <div class="field">
            <label for="reservedTime">예약 시간</label>
            <input id="reservedTime" type="datetime-local" value="2026-07-01T13:00" />
          </div>

          <div class="field">
            <label for="workStatus">작업 상태</label>
            <input id="workStatus" value="DISPATCH_WAITING" />
          </div>

          <button class="primary-button full request-button" type="button">
            운송 요청 등록
          </button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사</h2>
          <span class="status-pill green">{{ availableMyDrivers.length }}명</span>
        </div>

        <div v-if="loading" class="empty-box">
          불러오는 중...
        </div>

        <div v-else-if="availableMyDrivers.length === 0" class="empty-box">
          출입 가능한 소속 기사가 없습니다.
        </div>

        <div v-else class="match-list">
          <button
            v-for="driver in availableMyDrivers"
            :key="driver.driverId"
            class="match-card"
            type="button"
          >
            <span>
              <b>{{ driver.driverName }}</b>
              <small>
                연락처 {{ driver.driverContact || '-' }} /
                기사 ID {{ driver.driverId }} /
                운송사 ID {{ driver.carrierId }}
              </small>
            </span>

            <strong>출입 가능</strong>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.full {
  grid-column: 1 / -1;
}

.request-button {
  min-height: 44px;
}

.match-list {
  display: grid;
  gap: 10px;
}

.match-card {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  text-align: left;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.match-card:hover {
  border-color: var(--blue-700);
}

.match-card span,
.match-card small {
  display: block;
}

.match-card b {
  font-size: 16px;
}

.match-card small {
  margin-top: 4px;
  color: var(--ink-500);
  font-weight: 700;
}

.match-card strong {
  color: var(--green-600);
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