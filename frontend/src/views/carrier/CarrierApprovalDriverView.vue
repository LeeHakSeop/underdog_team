<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import {
  approveDriverByCarrier,
  fetchDrivers,
  reactivateDriver,
  updateDriver,
  withdrawDriver,
} from '@/api/driverApi'
import { fetchVehicles } from '@/api/vehicleApi'
import { vehicleTypeLabel } from '@/config/vehicleType'

const loading = ref(false)
const approvingId = ref(null)
const savingId = ref(null)
const editingDriverId = ref(null)
const message = ref('')
const errorMessage = ref('')
const pageSize = 10
const approvedSearch = ref('')
const withdrawnSearch = ref('')
const approvedPage = ref(1)
const withdrawnPage = ref(1)
const editForm = ref({
  driverName: '',
  driverContact: '',
})

const carriers = ref([])
const drivers = ref([])
const vehicles = ref([])

const currentUser = readCurrentUser()

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const pendingDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.userStatus !== 'WITHDRAWN' &&
      driver.isRegistered === false,
  )
})

const approvedDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.userStatus !== 'WITHDRAWN' &&
      driver.isRegistered === true,
  )
})

const withdrawnDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) =>
      driver.carrierId === myCarrier.value.carrierId &&
      driver.userStatus === 'WITHDRAWN',
  )
})

const matchesSearch = (driver, search) => {
  const keyword = search.trim().toLowerCase()
  if (!keyword) return true

  return [driver.driverName, driver.driverContact, driver.driverId, driver.userId]
    .filter((value) => value !== null && value !== undefined)
    .some((value) => String(value).toLowerCase().includes(keyword))
}

const filteredApprovedDrivers = computed(() =>
  approvedDrivers.value.filter((driver) => matchesSearch(driver, approvedSearch.value)),
)

const filteredWithdrawnDrivers = computed(() =>
  withdrawnDrivers.value.filter((driver) => matchesSearch(driver, withdrawnSearch.value)),
)

const approvedPageCount = computed(() =>
  Math.max(1, Math.ceil(filteredApprovedDrivers.value.length / pageSize)),
)

const withdrawnPageCount = computed(() =>
  Math.max(1, Math.ceil(filteredWithdrawnDrivers.value.length / pageSize)),
)

const pagedApprovedDrivers = computed(() => {
  const start = (approvedPage.value - 1) * pageSize
  return filteredApprovedDrivers.value.slice(start, start + pageSize)
})

const pagedWithdrawnDrivers = computed(() => {
  const start = (withdrawnPage.value - 1) * pageSize
  return filteredWithdrawnDrivers.value.slice(start, start + pageSize)
})

watch(approvedSearch, () => {
  approvedPage.value = 1
})

watch(withdrawnSearch, () => {
  withdrawnPage.value = 1
})

watch(approvedPageCount, (pageCount) => {
  if (approvedPage.value > pageCount) approvedPage.value = pageCount
})

watch(withdrawnPageCount, (pageCount) => {
  if (withdrawnPage.value > pageCount) withdrawnPage.value = pageCount
})

const myVehicles = computed(() => {
  if (!myCarrier.value) return []

  return vehicles.value.filter(
    (vehicle) => vehicle.carrierId === myCarrier.value.carrierId,
  )
})

const clearMessage = () => {
  message.value = ''
  errorMessage.value = ''
}

const loadData = async () => {
  loading.value = true

  try {
    const [carrierData, driverData, vehicleData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
      fetchVehicles(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
    vehicles.value = vehicleData || []
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

  if (!window.confirm(`${driver.driverName} 기사를 탈퇴 처리하시겠습니까? 기존 작업·출입 기록은 보존됩니다.`)) return

  savingId.value = driver.driverId

  try {
    await withdrawDriver(driver.driverId)
    if (editingDriverId.value === driver.driverId) cancelEdit()
    message.value = `${driver.driverName} 기사를 탈퇴 처리했습니다.`
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 탈퇴 처리에 실패했습니다.'
  } finally {
    savingId.value = null
  }
}

const reactivate = async (driver) => {
  clearMessage()
  savingId.value = driver.driverId

  try {
    await reactivateDriver(driver.driverId)
    message.value = `${driver.driverName} 기사 계정을 재활성화했습니다. 관리자 최종 승인 완료 후 다시 출입할 수 있습니다.`
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 재활성화에 실패했습니다.'
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
        <h2>승인·회원 현황 요약</h2>
        <span class="status-pill green">회원·차량 통합 조회</span>
      </div>

      <div class="approval-summary">
        <div>
          <span>운송사 상태</span>
          <strong>{{ myCarrier?.carrierStatus || '-' }}</strong>
        </div>
        <div>
          <span>기사 승인 완료</span>
          <strong>{{ approvedDrivers.length }}명</strong>
        </div>
        <div>
          <span>기사 승인 대기</span>
          <strong>{{ pendingDrivers.length }}명</strong>
        </div>
        <div>
          <span>소속 차량</span>
          <strong>{{ myVehicles.length }}대</strong>
        </div>
      </div>
    </section>

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
                탈퇴 처리
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
        <h2>승인 완료 기사 목록</h2>
        <span class="status-pill green">{{ filteredApprovedDrivers.length }}명</span>
      </div>

      <div class="list-toolbar">
        <label class="search-field">
          <span>기사 검색</span>
          <input
            v-model="approvedSearch"
            type="search"
            placeholder="기사명·연락처·ID"
          />
        </label>
        <span class="list-count">전체 {{ filteredApprovedDrivers.length }}명</span>
      </div>

      <div v-if="filteredApprovedDrivers.length === 0" class="empty-box">
        검색 조건에 맞는 승인 완료 기사가 없습니다.
      </div>

      <div v-else class="compact-list">
        <div
          v-for="driver in pagedApprovedDrivers"
          :key="driver.driverId"
          class="compact-row"
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
          <div v-else class="compact-main">
            <strong>{{ driver.driverName }}</strong>
            <span>{{ driver.driverContact || '-' }}</span>
            <small>기사 ID {{ driver.driverId }}</small>
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
              <span class="status-pill" :class="driver.canEnter ? 'green' : 'amber'">
                {{ driver.canEnter ? '출입 가능' : '최종 승인 대기' }}
              </span>
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
                탈퇴 처리
              </button>
            </template>
          </div>
        </div>
      </div>

      <nav v-if="filteredApprovedDrivers.length > pageSize" class="pagination" aria-label="승인 완료 기사 페이지">
        <button type="button" :disabled="approvedPage === 1" @click="approvedPage -= 1">이전</button>
        <span>{{ approvedPage }} / {{ approvedPageCount }}</span>
        <button type="button" :disabled="approvedPage >= approvedPageCount" @click="approvedPage += 1">다음</button>
      </nav>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>탈퇴 회원 목록</h2>
        <span class="status-pill red">{{ filteredWithdrawnDrivers.length }}명</span>
      </div>

      <div class="list-toolbar">
        <label class="search-field">
          <span>기사 검색</span>
          <input
            v-model="withdrawnSearch"
            type="search"
            placeholder="기사명·연락처·ID"
          />
        </label>
        <span class="list-count">전체 {{ filteredWithdrawnDrivers.length }}명</span>
      </div>

      <div v-if="filteredWithdrawnDrivers.length === 0" class="empty-box">
        검색 조건에 맞는 탈퇴 기사가 없습니다.
      </div>

      <div v-else class="compact-list">
        <div
          v-for="driver in pagedWithdrawnDrivers"
          :key="driver.driverId"
          class="compact-row"
        >
          <div class="compact-main">
            <strong>{{ driver.driverName }}</strong>
            <span>{{ driver.driverContact || '-' }}</span>
            <small>기사 ID {{ driver.driverId }} · 작업·출입 기록 보존</small>
          </div>
          <div class="driver-actions">
            <span class="status-pill red">탈퇴</span>
            <button
              class="approve-btn"
              type="button"
              :disabled="savingId === driver.driverId"
              @click="reactivate(driver)"
            >
              {{ savingId === driver.driverId ? '처리 중...' : '재활성화' }}
            </button>
          </div>
        </div>
      </div>

      <nav v-if="filteredWithdrawnDrivers.length > pageSize" class="pagination" aria-label="탈퇴 기사 페이지">
        <button type="button" :disabled="withdrawnPage === 1" @click="withdrawnPage -= 1">이전</button>
        <span>{{ withdrawnPage }} / {{ withdrawnPageCount }}</span>
        <button type="button" :disabled="withdrawnPage >= withdrawnPageCount" @click="withdrawnPage += 1">다음</button>
      </nav>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>소속 차량 승인 현황</h2>
        <span class="status-pill">차량 {{ myVehicles.length }}대</span>
      </div>

      <div v-if="myVehicles.length === 0" class="empty-box">
        등록된 차량이 없습니다.
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>차량 ID</th>
              <th>차량번호</th>
              <th>차량종류</th>
              <th>톤수</th>
              <th>등록 승인</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="vehicle in myVehicles" :key="vehicle.vehicleId">
              <td>{{ vehicle.vehicleId }}</td>
              <td>{{ vehicle.plateNumber || '-' }}</td>
              <td>{{ vehicleTypeLabel(vehicle.vehicleType) }}</td>
              <td>{{ vehicle.tonnage || '-' }}</td>
              <td>
                <span class="status-pill" :class="vehicle.isRegistered ? 'green' : 'amber'">
                  {{ vehicle.isRegistered ? '승인' : '승인 대기' }}
                </span>
              </td>
              <td>{{ vehicle.vehicleStatus || '-' }}</td>
            </tr>
          </tbody>
        </table>
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

.list-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.search-field {
  display: grid;
  width: min(360px, 100%);
  gap: 5px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.search-field input {
  min-height: 34px;
  padding: 0 10px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 3px;
}

.list-count {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 800;
}

.compact-list {
  display: grid;
  gap: 6px;
}

.compact-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 8px 10px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.compact-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.compact-main strong {
  min-width: 90px;
  color: var(--ink-900);
}

.compact-main span,
.compact-main small {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 12px;
}

.pagination button {
  min-width: 56px;
  min-height: 30px;
  padding: 0 10px;
  color: var(--ink-700);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 3px;
  font-weight: 800;
  cursor: pointer;
}

.pagination button:disabled {
  cursor: default;
  opacity: 0.5;
}

.pagination span {
  min-width: 52px;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 800;
  text-align: center;
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

.approval-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.approval-summary div {
  display: grid;
  gap: 4px;
  padding: 12px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.approval-summary span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.approval-summary strong {
  color: var(--ink-900);
  font-size: 16px;
}

@media (max-width: 760px) {
  .list-toolbar,
  .compact-row,
  .compact-main {
    align-items: stretch;
    flex-direction: column;
  }

  .list-toolbar {
    gap: 8px;
  }

  .search-field {
    width: 100%;
  }

  .approval-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
