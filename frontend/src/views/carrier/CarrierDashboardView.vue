<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { readCurrentUser } from '@/stores/authStore'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'

const currentUser = readCurrentUser()
const carrierStore = useCarrierStore()
const driverStore = useDriverStore()
const { carriers } = storeToRefs(carrierStore)
const { drivers } = storeToRefs(driverStore)

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]

const myCarrier = computed(() => {
  return carriers.value.find((carrier) => getId(carrier, 'userId') === currentUser?.userId)
})

const myDrivers = computed(() => {
  if (!myCarrier.value) return []
  const carrierId = getId(myCarrier.value, 'carrierId')
  return drivers.value.filter((driver) => getId(driver, 'carrierId') === carrierId)
})

const availableMyDrivers = computed(() => {
  return myDrivers.value.filter((driver) => driver.canEnter === true || driver.can_enter === true)
})

const waitingDrivers = computed(() => {
  return myDrivers.value.filter((driver) => !(driver.canEnter === true || driver.can_enter === true))
})

const actionCards = computed(() => [
  {
    title: '기사 가입 승인',
    count: waitingDrivers.value.length,
    text: '운송사 승인이 필요한 소속 기사 목록을 확인합니다.',
    path: '/carrier/driver-approval',
  },
  {
    title: '승인 현황',
    count: myDrivers.value.length,
    text: '소속 기사와 차량 승인 상태를 확인합니다.',
    path: '/carrier/approvals',
  },
])

const loadData = () => {
  carrierStore.loadCarriers().catch(() => {})
  driverStore.loadDrivers().catch(() => {})
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section v-if="carrierStore.error || driverStore.error" class="panel error-panel">
      {{ carrierStore.error || driverStore.error }}
    </section>

    <section class="grid-4">
      <article class="metric-card">
        <span>소속 기사</span>
        <strong>{{ myDrivers.length }}</strong>
        <small>DB 기준</small>
      </article>

      <article class="metric-card">
        <span>출입 가능 기사</span>
        <strong>{{ availableMyDrivers.length }}</strong>
        <small>승인 완료</small>
      </article>

      <article class="metric-card">
        <span>승인 대기</span>
        <strong>{{ waitingDrivers.length }}</strong>
        <small>확인 필요</small>
      </article>

      <article class="metric-card">
        <span>운송사</span>
        <strong>{{ myCarrier ? 1 : 0 }}</strong>
        <small>로그인 계정 기준</small>
      </article>
    </section>

    <section class="grid-3 carrier-action-grid">
      <RouterLink v-for="card in actionCards" :key="card.title" class="carrier-action-card" :to="card.path">
        <span>{{ card.title }}</span>
        <strong>{{ card.count }}</strong>
        <p>{{ card.text }}</p>
      </RouterLink>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>소속 기사</h2>
        <span class="status-pill green">DB 조회</span>
      </div>

      <div v-if="driverStore.loading" class="empty-box">
        불러오는 중입니다.
      </div>

      <div v-else-if="myDrivers.length === 0" class="empty-box">
        소속 기사가 없습니다.
      </div>

      <div v-else class="driver-list">
        <div v-for="driver in myDrivers" :key="driver.driverId || driver.driver_id" class="driver-row">
          <div>
            <b>{{ driver.driverName || driver.driver_name }}</b>
            <span>{{ driver.driverContact || driver.driver_contact || '-' }}</span>
          </div>
          <span class="status-pill" :class="driver.canEnter || driver.can_enter ? 'green' : 'red'">
            {{ driver.canEnter || driver.can_enter ? '출입 가능' : '승인 대기' }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
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
