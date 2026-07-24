<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { vehicleTypeLabel } from '@/config/vehicleType'

const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()

const { drivers } = storeToRefs(driverStore)
const { myVehicle, loading, error } = storeToRefs(vehicleStore)

const editing = ref(false)
const saving = ref(false)
const message = ref('')
const editError = ref('')
const editForm = ref({
  plateNumber: '',
})

const loginUser = computed(() => {
  return JSON.parse(localStorage.getItem('portGateUser') || 'null')
})

const myDriver = computed(() => {
  return drivers.value.find((driver) => driver.userId === loginUser.value?.userId) || null
})

const approvalText = computed(() => {
  if (!myVehicle.value) return '차량 정보 없음'
  return myVehicle.value.isRegistered ? '관리자 승인 완료' : '관리자 승인 대기'
})

const approvalClass = computed(() => {
  if (!myVehicle.value) return 'amber'
  return myVehicle.value.isRegistered ? 'green' : 'amber'
})

const canEnterText = computed(() => {
  if (!myDriver.value) return '기사 정보 없음'
  return myDriver.value.canEnter ? '출입 가능' : '출입 불가'
})

const canEnterClass = computed(() => {
  if (!myDriver.value) return 'amber'
  return myDriver.value.canEnter ? 'green' : 'red'
})

const loadData = async () => {
  message.value = ''
  editError.value = ''
  await driverStore.loadDrivers()

  if (myDriver.value?.driverId) {
    await vehicleStore.loadVehicleByDriver(myDriver.value.driverId)
  }
}

const startEdit = () => {
  if (!myVehicle.value) return

  editForm.value = {
    plateNumber: myVehicle.value.plateNumber || '',
  }
  message.value = ''
  editError.value = ''
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
  editError.value = ''
}

const saveVehicle = async () => {
  if (!myVehicle.value || !myDriver.value) return

  message.value = ''
  editError.value = ''

  if (!editForm.value.plateNumber.trim()) {
    editError.value = '트랙터 차량번호를 입력하세요.'
    return
  }

  saving.value = true

  try {
    await vehicleStore.editVehicle(myVehicle.value.vehicleId, {
      ...myVehicle.value,
      plateNumber: editForm.value.plateNumber.trim(),
    })
    await vehicleStore.loadVehicleByDriver(myDriver.value.driverId)
    editing.value = false
    message.value = '트랙터 정보가 수정되었습니다. 운송사 화면에도 반영됩니다.'
  } catch (error) {
    editError.value = error.message || '트랙터 정보 수정에 실패했습니다.'
  } finally {
    saving.value = false
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
        트랙터는 기사 본인 소유 차량입니다. 차량이 변경되면 아래 수정 기능으로 차량번호를 갱신하세요.
      </div>

      <div v-if="message" class="form-message success">
        {{ message }}
      </div>

      <div v-if="editError" class="form-message error">
        {{ editError }}
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
        등록된 차량 정보가 없습니다. 운송사 계정에서 차량 정보가 등록되었는지 확인하세요.
      </div>

      <div v-else class="vehicle-grid">
        <article class="info-card">
          <div class="card-heading">
            <h3>차량 정보</h3>
            <button
              v-if="!editing"
              class="ghost-button"
              type="button"
              @click="startEdit"
            >
              수정
            </button>
          </div>

          <form v-if="editing" class="edit-form" @submit.prevent="saveVehicle">
            <div class="field">
              <label for="driverTractorPlate">트랙터 차량번호</label>
              <input id="driverTractorPlate" v-model="editForm.plateNumber" />
            </div>

            <div class="edit-actions">
              <button class="secondary-button" type="button" @click="cancelEdit">취소</button>
              <button class="primary-button" type="submit" :disabled="saving">
                {{ saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </form>

          <table v-else class="data-table">
            <tbody>
              <tr><th>차량 번호</th><td>{{ myVehicle.plateNumber }}</td></tr>
              <tr><th>차량 유형</th><td>{{ vehicleTypeLabel(myVehicle.vehicleType) }}</td></tr>
              <tr><th>차량 상태</th><td>{{ myVehicle.vehicleStatus || '-' }}</td></tr>
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

.form-message {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  font-weight: 800;
}

.form-message.success {
  color: #155e3b;
  background: #ecfdf3;
  border-color: #b7ebc9;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border-color: #fecaca;
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

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.edit-form {
  display: grid;
  gap: 12px;
}

.edit-form .field {
  display: grid;
  gap: 6px;
}

.edit-form label {
  font-weight: 800;
}

.edit-form input {
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.edit-actions button {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 4px;
  font-weight: 800;
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
