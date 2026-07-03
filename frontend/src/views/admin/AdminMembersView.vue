<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'

const activeTab = ref('carrier')
const carrierStore = useCarrierStore()
const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()
const { carriers } = storeToRefs(carrierStore)
const { drivers } = storeToRefs(driverStore)
const { vehicles } = storeToRefs(vehicleStore)

onMounted(() => {
  carrierStore.loadCarriers()
  driverStore.loadDrivers()
  vehicleStore.loadVehicles()
})

const tabs = computed(() => [
  { key: 'carrier', label: '운송사', count: carriers.value.length },
  { key: 'driver', label: '기사', count: drivers.value.length },
  { key: 'vehicle', label: '차량', count: vehicles.value.length },
])

const updateCarrierStatus = (carrier, carrierStatus) => {
  return carrierStore.editCarrier(carrier.carrierId, { ...carrier, carrierStatus })
}

const updateDriverPermission = (driver, changes) => {
  return driverStore.editDriver(driver.driverId, { ...driver, ...changes })
}

const updateVehicleApproval = (vehicle, changes) => {
  return vehicleStore.editVehicle(vehicle.vehicleId, { ...vehicle, ...changes })
}
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>가입 및 권한 승인 관리</h2>
        <span class="status-pill">
          운송사 {{ carriers.length }} / 기사 {{ drivers.length }} / 차량 {{ vehicles.length }}
        </span>
      </div>

      <div class="member-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <b>{{ tab.count }}</b>
        </button>
      </div>
    </section>

    <section v-if="activeTab === 'carrier'" class="panel">
      <div class="section-title">
        <h2>운송사 승인</h2>
        <span class="status-pill green">관리자가 가입 상태를 결정</span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>운송사 ID</th>
              <th>운송사명</th>
              <th>연락처</th>
              <th>담당자명</th>
              <th>가입 상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="carrier in carriers" :key="carrier.carrierId">
              <td>{{ carrier.carrierId }}</td>
              <td>{{ carrier.carrierName }}</td>
              <td>{{ carrier.carrierContact || '-' }}</td>
              <td>{{ carrier.managerName || '-' }}</td>
              <td>
                <span class="status-pill" :class="{ green: carrier.carrierStatus === 'APPROVED', amber: carrier.carrierStatus === 'PENDING' }">
                  {{ carrier.carrierStatus }}
                </span>
              </td>
              <td>
                <div class="action-row">
                  <button class="ghost-button" type="button" @click="updateCarrierStatus(carrier, 'APPROVED')">
                    승인
                  </button>
                  <button class="ghost-button" type="button" @click="updateCarrierStatus(carrier, 'REJECTED')">
                    반려
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'driver'" class="panel">
      <div class="section-title">
        <h2>기사 권한 승인</h2>
        <span class="status-pill amber">관리자가 등록 여부와 출입 권한을 결정</span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>기사 ID</th>
              <th>기사명</th>
              <th>연락처</th>
              <th>등록 승인</th>
              <th>운송사 ID</th>
              <th>출입 가능</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="driver in drivers" :key="driver.driverId">
              <td>{{ driver.driverId }}</td>
              <td>{{ driver.driverName }}</td>
              <td>{{ driver.driverContact || '-' }}</td>
              <td>
                <span class="status-pill" :class="driver.isRegistered ? 'green' : 'red'">
                  {{ driver.isRegistered }}
                </span>
              </td>
              <td>{{ driver.carrierId || '-' }}</td>
              <td>
                <span class="status-pill" :class="driver.canEnter ? 'green' : 'red'">
                  {{ driver.canEnter }}
                </span>
              </td>
              <td>
                <div class="action-row">
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateDriverPermission(driver, { isRegistered: true, canEnter: true })"
                  >
                    승인
                  </button>
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateDriverPermission(driver, { isRegistered: false, canEnter: false })"
                  >
                    차단
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else class="panel">
      <div class="section-title">
        <h2>차량 등록 승인</h2>
        <span class="status-pill amber">관리자가 차량 등록 여부와 상태를 결정</span>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>차량 ID</th>
              <th>차량 번호</th>
              <th>차량 유형</th>
              <th>톤수</th>
              <th>등록 승인</th>
              <th>차량 상태</th>
              <th>트랙터 번호</th>
              <th>샤시 번호</th>
              <th>운송사 ID</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="vehicle in vehicles" :key="vehicle.vehicleId">
              <td>{{ vehicle.vehicleId }}</td>
              <td>{{ vehicle.plateNumber }}</td>
              <td>{{ vehicle.vehicleType }}</td>
              <td>{{ vehicle.tonnage }}</td>
              <td>
                <span class="status-pill" :class="vehicle.isRegistered ? 'green' : 'red'">
                  {{ vehicle.isRegistered }}
                </span>
              </td>
              <td>{{ vehicle.vehicleStatus }}</td>
              <td>{{ vehicle.tractorNo || '-' }}</td>
              <td>{{ vehicle.chassisNo || '-' }}</td>
              <td>{{ vehicle.carrierId }}</td>
              <td>
                <div class="action-row">
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateVehicleApproval(vehicle, { isRegistered: true, vehicleStatus: 'NORMAL' })"
                  >
                    승인
                  </button>
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateVehicleApproval(vehicle, { isRegistered: false, vehicleStatus: 'RESTRICTED' })"
                  >
                    제한
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.member-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.member-tabs button {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: var(--ink-700);
  background: #eef3f8;
  border: 1px solid var(--line);
  border-radius: 1px;
  font-weight: 700;
}

.member-tabs button.active {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.member-tabs b {
  font-weight: 700;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
