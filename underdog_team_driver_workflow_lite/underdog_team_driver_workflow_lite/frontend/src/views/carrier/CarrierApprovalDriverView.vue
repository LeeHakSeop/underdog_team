<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import {
  approveDriverByCarrier,
  deleteDriver,
  fetchCarrierDriverManagement,
  updateDriver,
} from '@/api/driverApi'

const currentUser = readCurrentUser()
const loading = ref(false)
const approvingId = ref(null)
const editingDriverId = ref(null)
const deletingDriverId = ref(null)
const message = ref('')
const errorMessage = ref('')
const drivers = ref([])
const keyword = ref('')

const editForm = ref({
  driverName: '',
  driverContact: '',
})

const carrierName = computed(() => drivers.value[0]?.carrierName || '운송사')
const pendingDrivers = computed(() => drivers.value.filter((driver) => !driver.isRegistered))
const managedDrivers = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return drivers.value

  return drivers.value.filter((driver) =>
    [
      driver.driverName,
      driver.driverContact,
      driver.loginId,
      driver.plateNumber,
      driver.tractorNo,
      driver.chassisNo,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  )
})

const clearMessage = () => {
  message.value = ''
  errorMessage.value = ''
}

const loadData = async () => {
  clearMessage()

  if (!currentUser?.userId) {
    errorMessage.value = '로그인한 운송사 정보를 확인할 수 없습니다.'
    return
  }

  loading.value = true

  try {
    drivers.value = (await fetchCarrierDriverManagement(currentUser.userId)) || []
  } catch (error) {
    errorMessage.value = error.message || '소속 기사 정보를 불러오지 못했습니다.'
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
  clearMessage()
  editingDriverId.value = driver.driverId
  editForm.value = {
    driverName: driver.driverName || '',
    driverContact: driver.driverContact || '',
  }
}

const cancelEdit = () => {
  editingDriverId.value = null
  editForm.value = { driverName: '', driverContact: '' }
}

const saveDriver = async (driver) => {
  clearMessage()

  if (!editForm.value.driverName.trim() || !editForm.value.driverContact.trim()) {
    errorMessage.value = '기사명과 연락처를 모두 입력하세요.'
    return
  }

  try {
    await updateDriver(driver.driverId, {
      driverId: driver.driverId,
      userId: driver.userId,
      carrierId: driver.carrierId,
      driverName: editForm.value.driverName.trim(),
      driverContact: editForm.value.driverContact.trim(),
      isRegistered: driver.isRegistered,
      canEnter: driver.canEnter,
    })

    message.value = `${editForm.value.driverName} 기사 정보를 수정했습니다.`
    cancelEdit()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 수정에 실패했습니다.'
  }
}

const removeDriver = async (driver) => {
  clearMessage()
  if (!window.confirm(`${driver.driverName} 기사를 삭제하시겠습니까?`)) return

  deletingDriverId.value = driver.driverId

  try {
    await deleteDriver(driver.driverId)
    message.value = `${driver.driverName} 기사를 삭제했습니다.`
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 삭제에 실패했습니다.'
  } finally {
    deletingDriverId.value = null
  }
}

const driverStatus = (driver) => {
  if (!driver.isRegistered) return { text: '운송사 승인 대기', className: 'amber' }
  if (!driver.canEnter) return { text: '관리자 최종 승인 대기', className: 'blue' }
  return { text: '최종 승인 완료', className: 'green' }
}

const vehicleStatus = (driver) => {
  if (!driver.vehicleId) return { text: '트랙터 미등록', className: 'gray' }
  if (driver.vehicleRegistered) return { text: driver.vehicleStatus || '차량 승인 완료', className: 'green' }
  return { text: driver.vehicleStatus || '차량 승인 대기', className: 'amber' }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>기사 가입 승인</h2>
        <span class="status-pill">
          {{ carrierName }} / 승인대기 {{ pendingDrivers.length }}명
        </span>
      </div>

      <div v-if="message" class="form-message success">{{ message }}</div>
      <div v-if="errorMessage" class="form-message error">{{ errorMessage }}</div>

      <div v-if="loading" class="empty-box">불러오는 중...</div>
      <div v-else-if="pendingDrivers.length === 0" class="empty-box">
        승인 대기 중인 기사가 없습니다.
      </div>
      <div v-else class="driver-list">
        <article v-for="driver in pendingDrivers" :key="driver.driverId" class="approval-row">
          <div>
            <b>{{ driver.driverName }}</b>
            <span>연락처 {{ driver.driverContact || '-' }}</span>
            <small>
              차량번호 {{ driver.plateNumber || '미등록' }} · 트랙터번호 {{ driver.tractorNo || '미등록' }}
            </small>
          </div>
          <button
            class="approve-btn"
            type="button"
            :disabled="approvingId === driver.driverId"
            @click="approve(driver)"
          >
            {{ approvingId === driver.driverId ? '승인 중...' : '기사 승인' }}
          </button>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="section-title management-title">
        <div>
          <h2>소속 기사 관리</h2>
          <p>기사 회원가입 때 입력한 계정·트랙터 정보를 함께 확인합니다.</p>
        </div>
        <span class="status-pill green">{{ drivers.length }}명</span>
      </div>

      <div class="search-row">
        <input
          v-model="keyword"
          type="search"
          placeholder="기사명, 연락처, 차량번호, 트랙터번호, 샤시번호 검색"
        />
        <button type="button" class="refresh-btn" :disabled="loading" @click="loadData">
          새로고침
        </button>
      </div>

      <div v-if="!loading && managedDrivers.length === 0" class="empty-box">
        조건에 맞는 소속 기사가 없습니다.
      </div>

      <div v-else class="management-list">
        <article v-for="driver in managedDrivers" :key="driver.driverId" class="driver-card">
          <header class="driver-card-header">
            <div v-if="editingDriverId !== driver.driverId" class="driver-name-block">
              <h3>{{ driver.driverName || '이름 미등록' }}</h3>
              <span>{{ driver.driverContact || '연락처 미등록' }}</span>
              <small>기사 ID {{ driver.driverId }} / 사용자 ID {{ driver.userId }}</small>
            </div>

            <div v-else class="edit-fields">
              <input v-model="editForm.driverName" type="text" placeholder="기사명" />
              <input v-model="editForm.driverContact" type="text" placeholder="연락처" />
            </div>

            <div class="header-actions">
              <span class="status-pill" :class="driverStatus(driver).className">
                {{ driverStatus(driver).text }}
              </span>

              <template v-if="editingDriverId === driver.driverId">
                <button class="save-btn" type="button" @click="saveDriver(driver)">저장</button>
                <button class="cancel-btn" type="button" @click="cancelEdit">취소</button>
              </template>
              <template v-else>
                <button class="edit-btn" type="button" @click="startEdit(driver)">수정</button>
                <button
                  class="delete-btn"
                  type="button"
                  :disabled="deletingDriverId === driver.driverId"
                  @click="removeDriver(driver)"
                >
                  {{ deletingDriverId === driver.driverId ? '삭제 중' : '삭제' }}
                </button>
              </template>
            </div>
          </header>

          <div class="detail-grid">
            <section class="detail-box">
              <h4>가입 및 승인 정보</h4>
              <dl>
                <div><dt>로그인 아이디</dt><dd>{{ driver.loginId || '-' }}</dd></div>
                <div><dt>계정 상태</dt><dd>{{ driver.userStatus || '-' }}</dd></div>
                <div><dt>운송사 승인</dt><dd>{{ driver.isRegistered ? '승인 완료' : '승인 대기' }}</dd></div>
                <div><dt>게이트 출입</dt><dd>{{ driver.canEnter ? '가능' : '불가' }}</dd></div>
              </dl>
            </section>

            <section class="detail-box tractor-box">
              <div class="box-title-row">
                <h4>회원가입 트랙터 정보</h4>
                <span class="status-pill compact" :class="vehicleStatus(driver).className">
                  {{ vehicleStatus(driver).text }}
                </span>
              </div>

              <dl v-if="driver.vehicleId">
                <div><dt>차량번호</dt><dd class="plate">{{ driver.plateNumber || '-' }}</dd></div>
                <div><dt>차량 종류</dt><dd>{{ driver.vehicleType || 'TRACTOR' }}</dd></div>
                <div><dt>톤수</dt><dd>{{ driver.tonnage || '-' }}</dd></div>
                <div><dt>트랙터 관리번호</dt><dd>{{ driver.tractorNo || '-' }}</dd></div>
                <div class="wide"><dt>샤시번호</dt><dd>{{ driver.chassisNo || '-' }}</dd></div>
                <div><dt>차량 승인</dt><dd>{{ driver.vehicleRegistered ? '승인 완료' : '승인 대기' }}</dd></div>
                <div><dt>차량 상태</dt><dd>{{ driver.vehicleStatus || '-' }}</dd></div>
              </dl>
              <div v-else class="no-vehicle">연결된 트랙터 정보가 없습니다.</div>
            </section>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.driver-list,
.management-list {
  display: grid;
  gap: 10px;
}

.approval-row,
.driver-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.approval-row {
  padding: 12px;
  background: #f8fbfe;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.approval-row b,
.approval-row span,
.approval-row small,
.driver-name-block span,
.driver-name-block small {
  display: block;
}

.approval-row span,
.approval-row small,
.driver-name-block span,
.driver-name-block small,
.management-title p {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 12px;
}

.management-title p {
  margin-bottom: 0;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin: 0 0 12px;
}

.search-row input,
.edit-fields input {
  min-height: 38px;
  padding: 0 11px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: #fff;
}

.refresh-btn,
.edit-btn,
.save-btn,
.cancel-btn,
.delete-btn {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 4px;
  font-weight: 800;
  cursor: pointer;
}

.refresh-btn,
.edit-btn {
  color: #075ca8;
  background: #eef7ff;
  border: 1px solid #b8d8f4;
}

.save-btn {
  color: #067647;
  background: #ecfdf3;
  border: 1px solid #abefc6;
}

.cancel-btn {
  color: #475467;
  background: #f2f4f7;
  border: 1px solid #d0d5dd;
}

.delete-btn {
  color: #b42318;
  background: #fff1f0;
  border: 1px solid #f7b9b3;
}

.driver-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: #fff;
}

.driver-card-header {
  padding: 12px;
  background: #f8fbfe;
  border-bottom: 1px solid var(--line);
}

.driver-name-block h3 {
  margin: 0;
  font-size: 15px;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 7px;
}

.edit-fields {
  display: grid;
  grid-template-columns: 160px 180px;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(420px, 1.5fr);
  gap: 12px;
  padding: 12px;
}

.detail-box {
  padding: 12px;
  background: #fbfcfe;
  border: 1px solid #d8e2ec;
  border-radius: 4px;
}

.detail-box h4 {
  margin: 0 0 10px;
  color: #173b5f;
  font-size: 14px;
}

.box-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-box dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
  margin: 0;
}

.detail-box dl div {
  min-width: 0;
}

.detail-box dl .wide {
  grid-column: 1 / -1;
}

.detail-box dt {
  margin-bottom: 3px;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.detail-box dd {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: 13px;
  font-weight: 800;
}

.plate {
  color: #075ca8;
  letter-spacing: 0.4px;
}

.no-vehicle {
  padding: 20px 10px;
  color: var(--ink-500);
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 4px;
}

.status-pill.blue {
  color: #075ca8;
  background: #eef7ff;
  border-color: #b8d8f4;
}

.status-pill.gray {
  color: #475467;
  background: #f2f4f7;
  border-color: #d0d5dd;
}

.status-pill.compact {
  white-space: nowrap;
  font-size: 11px;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .driver-card-header,
  .approval-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .edit-fields {
    width: 100%;
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .search-row,
  .detail-box dl {
    grid-template-columns: 1fr;
  }

  .detail-box dl .wide {
    grid-column: auto;
  }
}
</style>
