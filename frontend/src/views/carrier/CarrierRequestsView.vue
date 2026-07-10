<script setup>




import { ref } from 'vue'
import { useLogisticsData } from '@/composables/useLogisticsData'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'

const { availableDrivers, containers } = useLogisticsData()
const workOrderStore = useWorkOrderStore()

const requestForm = ref({
  containerId: '',
  driverId: '',
  vehicleId: '',
  workType: 'LOAD_OUT',
  reservedTime: '2026-07-01T13:00',
  workStatus: 'DISPATCH_WAITING',
})

const submitRequest = async () => {
  await workOrderStore.addWorkOrder({
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
          <h2>요청 정보</h2>
        </div>
        <form class="form-grid" @submit.prevent="submitRequest">
          <div class="field">
            <label for="containerId">컨테이너</label>
            <select id="containerId" v-model="requestForm.containerId" required>
              <option value="">선택</option>
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
            <input id="vehicleId" v-model="requestForm.vehicleId" inputmode="numeric" />
          </div>
          <div class="field">
            <label for="workType">작업 유형</label>
            <select id="workType" v-model="requestForm.workType">
              <option value="LOAD_OUT">반출 상차</option>
              <option value="LOAD_IN">반입 하차</option>
            </select>
          </div>
          <div class="field">
            <label for="reservation">예약 방문 시간</label>
            <input id="reservation" v-model="requestForm.reservedTime" type="datetime-local" />
          </div>
          <div class="field">
            <label for="workStatus">작업 상태</label>
            <select id="workStatus" v-model="requestForm.workStatus">
              <option value="DISPATCH_WAITING">배차 대기</option>
              <option value="APPROVAL_WAITING">승인 대기</option>
            </select>
          </div>
          <button class="primary-button full request-button" type="submit">요청 등록</button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사 후보</h2>
          <span class="status-pill green">{{ availableDrivers.length }}명</span>
        </div>
        <div class="match-list">
          <article v-for="driver in availableDrivers" :key="driver.driver_id" class="match-card">
            <span>
              <b>{{ driver.driver_name }}</b>
              <small>연락처 {{ driver.driver_contact || '-' }}</small>
            </span>
            <strong>{{ driver.can_enter ? '출입 가능' : '출입 제한' }}</strong>
          </article>
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
