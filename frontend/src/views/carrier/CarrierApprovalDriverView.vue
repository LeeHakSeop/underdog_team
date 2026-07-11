<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers, approveDriverByCarrier } from '@/api/driverApi'

const loading = ref(false)
const approvingId = ref(null)
const message = ref('')
const errorMessage = ref('')

const carriers = ref([])
const drivers = ref([])

const currentUser = readCurrentUser()

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const pendingDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.isRegistered === false,
  )
})

const approvedDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.isRegistered === true,
  )
})

const clearMessage = () => {
  message.value = ''
  errorMessage.value = ''
}

const loadData = async () => {
  loading.value = true
  clearMessage()

  try {
    const [carrierData, driverData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
  } catch (error) {
    errorMessage.value = error.message || '기사 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const approve = async (driver) => {
  clearMessage()
  approvingId.value = driver.driverId

  try {
    await approveDriverByCarrier(driver.userId)
    message.value = `${driver.driverName} 기사 가입을 승인했습니다.`
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 승인에 실패했습니다.'
  } finally {
    approvingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>기사 가입 승인</h2>
        <span class="status-pill">
          {{ myCarrier?.carrierName || '운송사 조회 중' }} / 승인대기 {{ pendingDrivers.length }}명
        </span>
      </div>

      <div v-if="message" class="form-message success">
        {{ message }}
      </div>

      <div v-if="errorMessage" class="form-message error">
        {{ errorMessage }}
      </div>

      <div v-if="loading" class="empty-box">
        불러오는 중...
      </div>

      <div v-else-if="pendingDrivers.length === 0" class="empty-box">
        승인 대기 중인 기사가 없습니다.
      </div>

      <div v-else class="driver-list">
        <div
          v-for="driver in pendingDrivers"
          :key="driver.driverId"
          class="driver-row"
        >
          <div>
            <b>{{ driver.driverName }}</b>
            <span>연락처 {{ driver.driverContact || '-' }}</span>
            <small>기사 ID {{ driver.driverId }} / 사용자 ID {{ driver.userId }}</small>
          </div>

          <button
            class="approve-btn"
            type="button"
            :disabled="approvingId === driver.driverId"
            @click="approve(driver)"
          >
            {{ approvingId === driver.driverId ? '승인 중...' : '기사 승인' }}
          </button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>승인 완료 기사</h2>
        <span class="status-pill green">{{ approvedDrivers.length }}명</span>
      </div>

      <div v-if="approvedDrivers.length === 0" class="empty-box">
        승인 완료된 기사가 없습니다.
      </div>

      <div v-else class="driver-list">
        <div
          v-for="driver in approvedDrivers"
          :key="driver.driverId"
          class="driver-row"
        >
          <div>
            <b>{{ driver.driverName }}</b>
            <span>연락처 {{ driver.driverContact || '-' }}</span>
            <small>관리자 최종 승인 전까지 기사 로그인은 제한됩니다.</small>
          </div>

          <span class="status-pill" :class="driver.canEnter ? 'green' : 'amber'">
            {{ driver.canEnter ? '최종 승인 완료' : '관리자 최종 승인 대기' }}
          </span>
        </div>
      </div>
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
  background: #f8fbfe;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.driver-row b,
.driver-row span,
.driver-row small {
  display: block;
}

.driver-row b {
  margin-bottom: 4px;
}

.driver-row span,
.driver-row small {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.approve-btn {
  min-width: 96px;
  min-height: 34px;
  padding: 0 12px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 4px;
  font-weight: 800;
  cursor: pointer;
}

.approve-btn:disabled {
  cursor: wait;
  opacity: 0.65;
}

.form-message {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

.form-message.success {
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}
</style>
