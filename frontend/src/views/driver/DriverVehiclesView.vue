<script setup>
import { computed, ref } from 'vue'
import { vehicles } from '../../data/dbData'

const readRows = () => JSON.parse(localStorage.getItem('portGateVehicles') || '[]')
const writeRows = (rows) => localStorage.setItem('portGateVehicles', JSON.stringify(rows))

const localVehicles = ref(readRows())
const editingId = ref(null)

const vehicleForm = ref({
  plate_number: '',
  vehicle_type: 'TRACTOR',
  tonnage: '25T',
  is_registered: false,
  vehicle_status: 'PENDING',
  tractor_no: '',
  chassis_no: '',
  carrier_id: 1,
})

const vehicleRows = computed(() => [...vehicles, ...localVehicles.value])

const nextId = computed(() => {
  return vehicleRows.value.reduce((max, row) => Math.max(max, Number(row.vehicle_id) || 0), 0) + 1
})

const resetForm = () => {
  editingId.value = null
  vehicleForm.value = {
    plate_number: '',
    vehicle_type: 'TRACTOR',
    tonnage: '25T',
    is_registered: false,
    vehicle_status: 'PENDING',
    tractor_no: '',
    chassis_no: '',
    carrier_id: 1,
  }
}

const saveVehicle = () => {
  if (editingId.value) {
    localVehicles.value = localVehicles.value.map((vehicle) =>
      vehicle.vehicle_id === editingId.value
        ? { ...vehicleForm.value, vehicle_id: editingId.value, carrier_id: Number(vehicleForm.value.carrier_id) }
        : vehicle,
    )
  } else {
    localVehicles.value = [
      ...localVehicles.value,
      {
        ...vehicleForm.value,
        vehicle_id: nextId.value,
        carrier_id: Number(vehicleForm.value.carrier_id),
        is_registered: false,
        vehicle_status: 'PENDING',
      },
    ]
  }

  writeRows(localVehicles.value)
  resetForm()
}

const editVehicle = (vehicle) => {
  if (!localVehicles.value.some((item) => item.vehicle_id === vehicle.vehicle_id)) return
  editingId.value = vehicle.vehicle_id
  vehicleForm.value = { ...vehicle }
}

const deleteVehicle = (vehicleId) => {
  localVehicles.value = localVehicles.value.filter((vehicle) => vehicle.vehicle_id !== vehicleId)
  writeRows(localVehicles.value)
  if (editingId.value === vehicleId) resetForm()
}

const isLocalVehicle = (vehicleId) => localVehicles.value.some((vehicle) => vehicle.vehicle_id === vehicleId)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>차량 등록</h2>
        <span class="status-pill">관리자 승인 필요</span>
      </div>

      <form class="form-grid" @submit.prevent="saveVehicle">
        <div class="field">
          <label for="plateNumber">차량 번호</label>
          <input id="plateNumber" v-model="vehicleForm.plate_number" required />
        </div>
        <div class="field">
          <label for="vehicleType">차량 유형</label>
          <select id="vehicleType" v-model="vehicleForm.vehicle_type">
            <option value="TRACTOR">TRACTOR</option>
            <option value="CARGO">CARGO</option>
            <option value="TRAILER">TRAILER</option>
          </select>
        </div>
        <div class="field">
          <label for="tonnage">톤수</label>
          <input id="tonnage" v-model="vehicleForm.tonnage" />
        </div>
        <div class="field">
          <label for="tractorNo">트랙터 번호</label>
          <input id="tractorNo" v-model="vehicleForm.tractor_no" />
        </div>
        <div class="field">
          <label for="chassisNo">샤시 번호</label>
          <input id="chassisNo" v-model="vehicleForm.chassis_no" />
        </div>
        <div class="field">
          <label for="carrierId">운송사 ID</label>
          <input id="carrierId" v-model.number="vehicleForm.carrier_id" min="1" type="number" />
        </div>
        <div class="field">
          <label for="isRegistered">등록 승인 여부</label>
          <input id="isRegistered" :value="vehicleForm.is_registered" disabled />
        </div>
        <div class="field">
          <label for="vehicleStatus">차량 상태</label>
          <input id="vehicleStatus" v-model="vehicleForm.vehicle_status" disabled />
        </div>
        <div class="form-actions">
          <button class="primary-button" type="submit">{{ editingId ? '수정' : '등록' }}</button>
          <button class="ghost-button" type="button" @click="resetForm">초기화</button>
        </div>
      </form>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>차량 목록</h2>
        <span class="status-pill">{{ vehicleRows.length }}건</span>
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
                <span class="status-pill" :class="vehicle.is_registered ? 'green' : 'amber'">
                  {{ vehicle.is_registered }}
                </span>
              </td>
              <td>{{ vehicle.vehicle_status }}</td>
              <td>{{ vehicle.tractor_no || '-' }}</td>
              <td>{{ vehicle.chassis_no || '-' }}</td>
              <td>{{ vehicle.carrier_id }}</td>
              <td>
                <div v-if="isLocalVehicle(vehicle.vehicle_id)" class="action-row">
                  <button class="ghost-button" type="button" @click="editVehicle(vehicle)">수정</button>
                  <button class="ghost-button" type="button" @click="deleteVehicle(vehicle.vehicle_id)">삭제</button>
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
.form-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 6px;
}
</style>
