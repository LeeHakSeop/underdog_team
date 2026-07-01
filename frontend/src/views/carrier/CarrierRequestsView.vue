<script setup>
import { ref } from 'vue'
import { useLogisticsData } from '@/composables/useLogisticsData'
import { useTaskStore } from '@/stores/adminStore/taskStore'

const { availableDrivers, containers } = useLogisticsData()
const taskStore = useTaskStore()

const requestForm = ref({
  containerId: '',
  driverId: '',
  vehicleId: '',
  workType: 'LOAD_OUT',
  reservedTime: '2026-07-01T13:00',
  workStatus: 'DISPATCH_WAITING',
})

const submitRequest = async () => {
  await taskStore.addTask({
    ...requestForm.value,
    containerId: Number(requestForm.value.containerId),
    driverId: Number(requestForm.value.driverId) || null,
    vehicleId: Number(requestForm.value.vehicleId) || null,
    reservedTime: requestForm.value.reservedTime,
    isApproved: false,
  })
}
</script>

<template>
  <div class="page-stack">
    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>운송 요청 등록</h2>
        </div>
        <form class="form-grid" @submit.prevent="submitRequest">
          <div class="field">
            <label for="containerId">컨테이너</label>
            <select id="containerId" v-model="requestForm.containerId" required>
              <option value="" disabled>컨테이너 선택</option>
              <option v-for="container in containers" :key="container.container_id" :value="container.container_id">
                {{ container.container_number }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="driverId">기사</label>
            <select id="driverId" v-model="requestForm.driverId">
              <option value="">미배정</option>
              <option v-for="driver in availableDrivers" :key="driver.driver_id" :value="driver.driver_id">
                {{ driver.driver_name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label for="vehicleId">차량 ID</label>
            <input id="vehicleId" v-model="requestForm.vehicleId" min="1" type="number" />
          </div>
          <div class="field">
            <label for="workType">작업 유형</label>
            <select id="workType" v-model="requestForm.workType">
              <option value="LOAD_OUT">반출 적재</option>
              <option value="UNLOAD_IN">반입 하차</option>
            </select>
          </div>
          <div class="field">
            <label for="reservedTime">예약 시간</label>
            <input id="reservedTime" v-model="requestForm.reservedTime" type="datetime-local" />
          </div>
          <div class="field">
            <label for="workStatus">작업 상태</label>
            <input id="workStatus" v-model="requestForm.workStatus" />
          </div>
          <button class="primary-button full request-button" type="submit">운송 요청 등록</button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사</h2>
          <span class="status-pill green">{{ availableDrivers.length }}명</span>
        </div>
        <div class="match-list">
          <button
            v-for="driver in availableDrivers"
            :key="driver.driver_id"
            class="match-card"
            type="button"
            @click="requestForm.driverId = driver.driver_id"
          >
            <span>
              <b>{{ driver.driver_name }}</b>
              <small>기사 ID {{ driver.driver_id }} / 운송사 ID {{ driver.carrier_id }}</small>
            </span>
            <strong>출입 가능 {{ driver.can_enter }}</strong>
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
</style>
