<script setup>
import { computed, ref } from 'vue'
import { carriers, drivers, vehicles } from '@/data/dbData'

const activeTab = ref('carrier')

const readRows = (key) => JSON.parse(localStorage.getItem(key) || '[]')
const writeRows = (key, rows) => localStorage.setItem(key, JSON.stringify(rows))

const localCarriers = ref(readRows('portGateCarrierSignups'))
const localDrivers = ref(readRows('portGateDriverSignups'))
const localVehicles = ref(readRows('portGateVehicles'))

const carrierRows = computed(() => [...carriers, ...localCarriers.value])
const driverRows = computed(() => [...drivers, ...localDrivers.value])
const vehicleRows = computed(() => [...vehicles, ...localVehicles.value])

const tabs = computed(() => [
  { key: 'carrier', label: '운송사', count: carrierRows.value.length },
  { key: 'driver', label: '기사', count: driverRows.value.length },
  { key: 'vehicle', label: '차량', count: vehicleRows.value.length },
])

const updateCarrierStatus = (carrierId, carrier_status) => {
  localCarriers.value = localCarriers.value.map((carrier) =>
    carrier.carrier_id === carrierId ? { ...carrier, carrier_status } : carrier,
  )
  writeRows('portGateCarrierSignups', localCarriers.value)
}

const updateDriverPermission = (driverId, changes) => {
  localDrivers.value = localDrivers.value.map((driver) =>
    driver.driver_id === driverId ? { ...driver, ...changes } : driver,
  )
  writeRows('portGateDriverSignups', localDrivers.value)
}

const updateVehicleApproval = (vehicleId, changes) => {
  localVehicles.value = localVehicles.value.map((vehicle) =>
    vehicle.vehicle_id === vehicleId ? { ...vehicle, ...changes } : vehicle,
  )
  writeRows('portGateVehicles', localVehicles.value)
}

const isLocalCarrier = (carrierId) => localCarriers.value.some((carrier) => carrier.carrier_id === carrierId)
const isLocalDriver = (driverId) => localDrivers.value.some((driver) => driver.driver_id === driverId)
const isLocalVehicle = (vehicleId) => localVehicles.value.some((vehicle) => vehicle.vehicle_id === vehicleId)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>가입 및 권한 승인 관리</h2>
        <span class="status-pill">
          운송사 {{ carrierRows.length }} / 기사 {{ driverRows.length }} / 차량 {{ vehicleRows.length }}
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
            <tr v-for="carrier in carrierRows" :key="carrier.carrier_id">
              <td>{{ carrier.carrier_id }}</td>
              <td>{{ carrier.carrier_name }}</td>
              <td>{{ carrier.carrier_contact || '-' }}</td>
              <td>{{ carrier.manager_name || '-' }}</td>
              <td>
                <span class="status-pill" :class="{ green: carrier.carrier_status === 'APPROVED', amber: carrier.carrier_status === 'PENDING' }">
                  {{ carrier.carrier_status }}
                </span>
              </td>
              <td>
                <div v-if="isLocalCarrier(carrier.carrier_id)" class="action-row">
                  <button class="ghost-button" type="button" @click="updateCarrierStatus(carrier.carrier_id, 'APPROVED')">
                    승인
                  </button>
                  <button class="ghost-button" type="button" @click="updateCarrierStatus(carrier.carrier_id, 'REJECTED')">
                    반려
                  </button>
                </div>
                <span v-else>기본 데이터</span>
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
            <tr v-for="driver in driverRows" :key="driver.driver_id">
              <td>{{ driver.driver_id }}</td>
              <td>{{ driver.driver_name }}</td>
              <td>{{ driver.driver_contact || '-' }}</td>
              <td>
                <span class="status-pill" :class="driver.is_registered ? 'green' : 'red'">
                  {{ driver.is_registered }}
                </span>
              </td>
              <td>{{ driver.carrier_id || '-' }}</td>
              <td>
                <span class="status-pill" :class="driver.can_enter ? 'green' : 'red'">
                  {{ driver.can_enter }}
                </span>
              </td>
              <td>
                <div v-if="isLocalDriver(driver.driver_id)" class="action-row">
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateDriverPermission(driver.driver_id, { is_registered: true, can_enter: true })"
                  >
                    승인
                  </button>
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateDriverPermission(driver.driver_id, { is_registered: false, can_enter: false })"
                  >
                    차단
                  </button>
                </div>
                <span v-else>기본 데이터</span>
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
            <tr v-for="vehicle in vehicleRows" :key="vehicle.vehicle_id">
              <td>{{ vehicle.vehicle_id }}</td>
              <td>{{ vehicle.plate_number }}</td>
              <td>{{ vehicle.vehicle_type }}</td>
              <td>{{ vehicle.tonnage }}</td>
              <td>
                <span class="status-pill" :class="vehicle.is_registered ? 'green' : 'red'">
                  {{ vehicle.is_registered }}
                </span>
              </td>
              <td>{{ vehicle.vehicle_status }}</td>
              <td>{{ vehicle.tractor_no || '-' }}</td>
              <td>{{ vehicle.chassis_no || '-' }}</td>
              <td>{{ vehicle.carrier_id }}</td>
              <td>
                <div v-if="isLocalVehicle(vehicle.vehicle_id)" class="action-row">
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateVehicleApproval(vehicle.vehicle_id, { is_registered: true, vehicle_status: 'NORMAL' })"
                  >
                    승인
                  </button>
                  <button
                    class="ghost-button"
                    type="button"
                    @click="updateVehicleApproval(vehicle.vehicle_id, { is_registered: false, vehicle_status: 'RESTRICTED' })"
                  >
                    제한
                  </button>
                </div>
                <span v-else>기본 데이터</span>
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
