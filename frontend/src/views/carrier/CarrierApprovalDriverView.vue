<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import {
  approveDriverByCarrier,
  deleteDriver,
  fetchDrivers,
  updateDriver,
} from '@/api/driverApi'

const loading = ref(false)
const approvingId = ref(null)
const savingId = ref(null)
const editingDriverId = ref(null)
const message = ref('')
const errorMessage = ref('')
const editForm = ref({
  driverName: '',
  driverContact: '',
})

const carriers = ref([])
const drivers = ref([])

const currentUser = readCurrentUser()

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const pendingDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.isRegistered === false,
  )
})

const approvedDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.isRegistered === true,
  )
})

const clearMessage = () => {
  message.value = ''
  errorMessage.value = ''
}

const loadData = async () => {
  loading.value = true

  try {
    const [carrierData, driverData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
  } catch (error) {
    errorMessage.value = error.message || '기사 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const approve = async (driver) => {
  clearMessage()
  approvingId.value = driver.driverId

  try {
    await approveDriverByCarrier(driver.userId)
    message.value = `${driver.driverName} 기사 가입을 승인했습니다.`
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 승인에 실패했습니다.'
  } finally {
    approvingId.value = null
  }
}

const startEdit = (driver) => {
  editingDriverId.value = driver.driverId
  editForm.value = {
    driverName: driver.driverName || '',
    driverContact: driver.driverContact || '',
  }
  clearMessage()
}

const cancelEdit = () => {
  editingDriverId.value = null
  editForm.value = { driverName: '', driverContact: '' }
}

const saveEdit = async (driver) => {
  clearMessage()

  if (!editForm.value.driverName.trim() || !editForm.value.driverContact.trim()) {
    errorMessage.value = '기사명과 연락처를 입력하세요.'
    return
  }

  savingId.value = driver.driverId

  try {
    await updateDriver(driver.driverId, {
      driverName: editForm.value.driverName.trim(),
      driverContact: editForm.value.driverContact.trim(),
      isRegistered: driver.isRegistered,
      carrierId: driver.carrierId,
      canEnter: driver.canEnter,
      userId: driver.userId,
    })
    message.value = `${editForm.value.driverName} 기사 정보가 수정되었습니다.`
    cancelEdit()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 정보 수정에 실패했습니다.'
  } finally {
    savingId.value = null
  }
}

const removeDriver = async (driver) => {
  clearMessage()

  if (!window.confirm(`${driver.driverName} 기사를 삭제하시겠습니까?`)) return

  savingId.value = driver.driverId

  try {
    await deleteDriver(driver.driverId)
    if (editingDriverId.value === driver.driverId) cancelEdit()
    message.value = `${driver.driverName} 기사 정보가 삭제되었습니다.`
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 정보 삭제에 실패했습니다.'
  } finally {
    savingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>기사 가입 승인</h2>
        <span class="status-pill">
          {{ myCarrier?.carrierName || '운송사 조회 중' }} / 승인대기 {{ pendingDrivers.length }}명
        </span>
      </div>

      <div v-if="message" class="form-message success">
        {{ message }}
      </div>

      <div v-if="errorMessage" class="form-message error">
        {{ errorMessage }}
      </div>

      <div v-if="loading" class="empty-box">
        불러오는 중...
      </div>

      <div v-else-if="pendingDrivers.length === 0" class="empty-box">
        승인 대기 중인 기사가 없습니다.
      </div>

      <div v-else class="driver-list">
        <div
          v-for="driver in pendingDrivers"
          :key="driver.driverId"
          class="driver-row"
        >
          <div v-if="editingDriverId === driver.driverId" class="driver-edit-form">
            <label>
              기사명
              <input v-model="editForm.driverName" type="text" />
            </label>
            <label>
              연락처
              <input v-model="editForm.driverContact" type="text" />
            </label>
          </div>
          <div v-else>
            <b>{{ driver.driverName }}</b>
            <span>연락처 {{ driver.driverContact || '-' }}</span>
            <small>기사 ID {{ driver.driverId }} / 사용자 ID {{ driver.userId }}</small>
          </div>

          <div class="driver-actions">
            <template v-if="editingDriverId === driver.driverId">
              <button
                class="approve-btn"
                type="button"
                :disabled="savingId === driver.driverId"
                @click="saveEdit(driver)"
              >
                {{ savingId === driver.driverId ? '저장 중...' : '저장' }}
              </button>
              <button class="ghost-button" type="button" @click="cancelEdit">취소</button>
            </template>
            <template v-else>
              <button
                class="ghost-button"
                type="button"
                :disabled="savingId === driver.driverId"
                @click="startEdit(driver)"
              >
                수정
              </button>
              <button
                class="ghost-button reject"
                type="button"
                :disabled="savingId === driver.driverId"
                @click="removeDriver(driver)"
              >
                삭제
              </button>
              <button
                class="approve-btn"
                type="button"
                :disabled="approvingId === driver.driverId"
                @click="approve(driver)"
              >
                {{ approvingId === driver.driverId ? '승인 중...' : '기사 승인' }}
              </button>
            </template>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>승인 완료 기사</h2>
        <span class="status-pill green">{{ approvedDrivers.length }}명</span>
      </div>

      <div v-if="approvedDrivers.length === 0" class="empty-box">
        승인 완료된 기사가 없습니다.
      </div>

      <div v-else class="driver-list">
        <div
          v-for="driver in approvedDrivers"
          :key="driver.driverId"
          class="driver-row"
        >
          <div v-if="editingDriverId === driver.driverId" class="driver-edit-form">
            <label>
              기사명
              <input v-model="editForm.driverName" type="text" />
            </label>
            <label>
              연락처
              <input v-model="editForm.driverContact" type="text" />
            </label>
          </div>
          <div v-else>
            <b>{{ driver.driverName }}</b>
            <span>연락처 {{ driver.driverContact || '-' }}</span>
            <small>관리자 최종 승인 전까지 기사 로그인은 제한됩니다.</small>
          </div>

          <div class="driver-actions">
            <template v-if="editingDriverId === driver.driverId">
              <button
                class="approve-btn"
                type="button"
                :disabled="savingId === driver.driverId"
                @click="saveEdit(driver)"
              >
                {{ savingId === driver.driverId ? '저장 중...' : '저장' }}
              </button>
              <button class="ghost-button" type="button" @click="cancelEdit">취소</button>
            </template>
            <template v-else>
              <button
                class="ghost-button"
                type="button"
                :disabled="savingId === driver.driverId"
                @click="startEdit(driver)"
              >
                수정
              </button>
              <button
                class="ghost-button reject"
                type="button"
                :disabled="savingId === driver.driverId"
                @click="removeDriver(driver)"
              >
                삭제
              </button>
              <span class="status-pill" :class="driver.canEnter ? 'green' : 'amber'">
                {{ driver.canEnter ? '최종 승인 완료' : '관리자 최종 승인 대기' }}
              </span>
            </template>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
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
  background: #f8fbfe;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.driver-row b,
.driver-row span,
.driver-row small {
  display: block;
}

.driver-row b {
  margin-bottom: 4px;
}

.driver-row span,
.driver-row small {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.driver-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.driver-edit-form {
  display: grid;
  min-width: 240px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.driver-edit-form label {
  display: grid;
  gap: 4px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.driver-edit-form input {
  min-height: 32px;
  padding: 0 8px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.approve-btn {
  min-width: 96px;
  min-height: 34px;
  padding: 0 12px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 4px;
  font-weight: 800;
  cursor: pointer;
}

.approve-btn:disabled {
  cursor: wait;
  opacity: 0.65;
}

.form-message {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

.form-message.success {
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}
</style>
