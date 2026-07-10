<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'

const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()

const { drivers } = storeToRefs(driverStore)
const { myVehicle, loading, error } = storeToRefs(vehicleStore)

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

const myDriver = computed(() => {
  return drivers.value.find((driver) => driver.userId === loginUser.value?.userId) || null
})

const approvalText = computed(() => {
  if (!myVehicle.value) {
    return '차량 정보 없음'
  }

  return myVehicle.value.isRegistered ? '관리자 승인 완료' : '관리자 승인 대기'
})

const approvalClass = computed(() => {
  if (!myVehicle.value) {
    return 'amber'
  }

  return myVehicle.value.isRegistered ? 'green' : 'amber'
})

const canEnterText = computed(() => {
  if (!myDriver.value) {
    return '기사 정보 없음'
  }

  return myDriver.value.canEnter ? '출입 가능' : '출입 불가'
})

const canEnterClass = computed(() => {
  if (!myDriver.value) {
    return 'amber'
  }

  return myDriver.value.canEnter ? 'green' : 'red'
})

const loadData = async () => {
  await driverStore.loadDrivers()

  if (myDriver.value?.driverId) {
    await vehicleStore.loadVehicleByDriver(myDriver.value.driverId)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>내 차량</h2>
        <span class="status-pill" :class="approvalClass">
          {{ approvalText }}
        </span>
      </div>

      <div class="notice-box">
        차량 등록은 회원가입 단계에서 신청합니다. 로그인 후에는 배정된 차량과 승인 상태만 확인합니다.
      </div>

      <div v-if="loading" class="empty-box">
        차량 정보를 불러오는 중입니다.
      </div>

      <div v-else-if="error" class="empty-box warning">
        {{ error }}
      </div>

      <div v-else-if="!myDriver" class="empty-box">
        로그인한 기사 정보를 찾을 수 없습니다.
      </div>

      <div v-else-if="!myVehicle" class="empty-box">
        등록된 차량 정보가 없습니다. 회원가입 시 차량 정보를 등록했는지 확인하세요.
      </div>

      <div v-else class="vehicle-grid">
        <article class="info-card">
          <h3>차량 정보</h3>

          <table class="data-table">
            <tbody>
              <tr>
                <th>차량 번호</th>
                <td>{{ myVehicle.plateNumber }}</td>
              </tr>
              <tr>
                <th>차량 유형</th>
                <td>{{ myVehicle.vehicleType || '-' }}</td>
              </tr>
              <tr>
                <th>톤수</th>
                <td>{{ myVehicle.tonnage || '-' }}</td>
              </tr>
              <tr>
                <th>트랙터 번호</th>
                <td>{{ myVehicle.tractorNo || '-' }}</td>
              </tr>
              <tr>
                <th>샤시 번호</th>
                <td>{{ myVehicle.chassisNo || '-' }}</td>
              </tr>
              <tr>
                <th>차량 상태</th>
                <td>{{ myVehicle.vehicleStatus || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="info-card">
          <h3>출입 상태</h3>

          <div class="status-grid">
            <div>
              <span>차량 승인</span>
              <strong>{{ approvalText }}</strong>
            </div>
            <div>
              <span>기사 출입</span>
              <strong>
                <span class="status-pill" :class="canEnterClass">
                  {{ canEnterText }}
                </span>
              </strong>
            </div>
            <div>
              <span>소속 운송사 ID</span>
              <strong>{{ myDriver.carrierId || '-' }}</strong>
            </div>
            <div>
              <span>기사 연락처</span>
              <strong>{{ myDriver.driverContact || '-' }}</strong>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.notice-box,
.empty-box {
  padding: 12px;
  color: var(--ink-500);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
}

.empty-box.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

.info-card {
  display: grid;
  min-width: 0;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.info-card h3 {
  margin: 0;
  font-size: 16px;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 10px;
}

.info-card .data-table {
  min-width: 0;
  table-layout: fixed;
}

.info-card .data-table th {
  width: 120px;
}

.status-grid {
  display: grid;
  gap: 10px;
}

.status-grid div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.status-grid span {
  color: var(--ink-500);
  font-weight: 800;
}

.status-grid strong {
  text-align: right;
}

@media (max-width: 900px) {
  .vehicle-grid {
    grid-template-columns: 1fr;
  }

  .status-grid div {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-grid strong {
    text-align: left;
  }
}
</style>
