<script setup>

import { availableDrivers } from '../../data/mockData'

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
        <form class="form-grid">
          <div class="field">
            <label for="containerNo">컨테이너 번호</label>
            <input id="containerNo" value="KDTU1234567" />
          </div>
          <div class="field">
            <label for="workType">작업 유형</label>
            <select id="workType">
              <option>반출 상차</option>
              <option>반입 하차</option>
            </select>
          </div>
          <div class="field">
            <label for="cargoSize">화물 규격</label>
            <select id="cargoSize">
              <option>20FT</option>
              <option selected>40FT</option>
            </select>
          </div>
          <div class="field">
            <label for="cargoType">화물 종류</label>
            <select id="cargoType">
              <option selected>DRY</option>
              <option>REEFER</option>
              <option>TANK</option>
            </select>
          </div>
          <div class="field">
            <label for="reservation">희망 방문 시간</label>
            <input id="reservation" type="datetime-local" value="2026-06-30T13:00" />
          </div>
          <div class="field">
            <label for="destination">도착지</label>
            <input id="destination" value="부산 물류센터 2Hub" />
          </div>
          <div class="field full">
            <label for="memo">요청 메모</label>
            <textarea id="memo">냉동 컨테이너 우선 배차 요청</textarea>
          </div>
          <button class="primary-button full request-button" type="button">요청 등록</button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>출입 가능 기사 후보</h2>
          <span class="status-pill green">3명</span>
        </div>
        <div class="match-list">
          <button v-for="driver in availableDrivers" :key="driver.vehicleNo" class="match-card" type="button">
            <span>
              <b>{{ driver.name }}</b>
              <small>{{ driver.vehicleNo }} · 현재 위치 {{ driver.distance }}</small>
            </span>
            <strong>{{ driver.status }}</strong>
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
