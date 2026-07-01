<script setup>
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useVehicleStore } from '@/stores/vehicleStore'

const vehicleStore = useVehicleStore()
const { vehicles } = storeToRefs(vehicleStore)
const editingId = ref(null)

const emptyForm = () => ({
  plateNumber: '',
  vehicleType: 'TRACTOR',
  tonnage: '25T',
  isRegistered: false,
  vehicleStatus: 'PENDING',
  tractorNo: '',
  chassisNo: '',
  carrierId: 1,
})

const vehicleForm = ref(emptyForm())

onMounted(() => {
  vehicleStore.loadVehicles()
})

const resetForm = () => {
  editingId.value = null
  vehicleForm.value = emptyForm()
}

const saveVehicle = async () => {
  const payload = {
    ...vehicleForm.value,
    carrierId: Number(vehicleForm.value.carrierId),
  }

  if (editingId.value) {
    await vehicleStore.editVehicle(editingId.value, payload)
  } else {
    await vehicleStore.addVehicle({
      ...payload,
      isRegistered: false,
      vehicleStatus: 'PENDING',
    })
  }

  resetForm()
}

const editVehicle = (vehicle) => {
  editingId.value = vehicle.vehicleId
  vehicleForm.value = { ...vehicle }
}

const deleteVehicle = async (vehicleId) => {
  await vehicleStore.removeVehicle(vehicleId)
  if (editingId.value === vehicleId) resetForm()
}
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
          <input id="plateNumber" v-model="vehicleForm.plateNumber" required />
        </div>
        <div class="field">
          <label for="vehicleType">차량 유형</label>
          <select id="vehicleType" v-model="vehicleForm.vehicleType">
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
          <input id="tractorNo" v-model="vehicleForm.tractorNo" />
        </div>
        <div class="field">
          <label for="chassisNo">샤시 번호</label>
          <input id="chassisNo" v-model="vehicleForm.chassisNo" />
        </div>
        <div class="field">
          <label for="carrierId">운송사 ID</label>
          <input id="carrierId" v-model.number="vehicleForm.carrierId" min="1" type="number" />
        </div>
        <div class="field">
          <label for="isRegistered">등록 승인 여부</label>
          <input id="isRegistered" :value="vehicleForm.isRegistered" disabled />
        </div>
        <div class="field">
          <label for="vehicleStatus">차량 상태</label>
          <input id="vehicleStatus" v-model="vehicleForm.vehicleStatus" disabled />
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
        <span class="status-pill">{{ vehicles.length }}건</span>
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
                <span class="status-pill" :class="vehicle.isRegistered ? 'green' : 'amber'">
                  {{ vehicle.isRegistered }}
                </span>
              </td>
              <td>{{ vehicle.vehicleStatus }}</td>
              <td>{{ vehicle.tractorNo || '-' }}</td>
              <td>{{ vehicle.chassisNo || '-' }}</td>
              <td>{{ vehicle.carrierId }}</td>
              <td>
                <div class="action-row">
                  <button class="ghost-button" type="button" @click="editVehicle(vehicle)">수정</button>
                  <button class="ghost-button" type="button" @click="deleteVehicle(vehicle.vehicleId)">삭제</button>
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
.form-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 6px;
}
</style>
