<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { deleteDriver, fetchCarrierDriverManagement, updateDriver } from '@/api/driverApi'
import { updateVehicle } from '@/api/vehicleApi'

const currentUser = readCurrentUser()
const loading = ref(false)
const savingId = ref(null)
const deletingId = ref(null)
const message = ref('')
const errorMessage = ref('')
const drivers = ref([])
const keyword = ref('')
const statusFilter = ref('ALL')
const editingId = ref(null)
const editForm = ref({})

const filteredDrivers = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return drivers.value.filter((driver) => {
    const matchesKeyword = !q || [driver.driverName, driver.driverContact, driver.loginId,
      driver.plateNumber, driver.tractorNo, driver.chassisNo]
      .filter(Boolean).some((value) => String(value).toLowerCase().includes(q))
    const matchesStatus = statusFilter.value === 'ALL'
      || (statusFilter.value === 'PENDING' && !driver.isRegistered)
      || (statusFilter.value === 'ADMIN_WAITING' && driver.isRegistered && !driver.canEnter)
      || (statusFilter.value === 'ACTIVE' && driver.canEnter)
    return matchesKeyword && matchesStatus
  })
})

const clearMessages = () => { message.value = ''; errorMessage.value = '' }
const loadDrivers = async () => {
  clearMessages()
  if (!currentUser?.userId) { errorMessage.value = '로그인한 운송사 정보를 확인할 수 없습니다.'; return }
  loading.value = true
  try { drivers.value = (await fetchCarrierDriverManagement(currentUser.userId)) || [] }
  catch (error) { errorMessage.value = error.message || '기사관리 정보를 불러오지 못했습니다.' }
  finally { loading.value = false }
}

const statusText = (driver) => !driver.isRegistered ? '운송사 승인 대기' : !driver.canEnter ? '관리자 승인 대기' : '최종 승인 완료'
const statusClass = (driver) => !driver.isRegistered ? 'amber' : !driver.canEnter ? 'blue' : 'green'

const startEdit = (driver) => {
  clearMessages(); editingId.value = driver.driverId
  editForm.value = {
    driverName: driver.driverName || '', driverContact: driver.driverContact || '',
    plateNumber: driver.plateNumber || '', vehicleType: driver.vehicleType || 'TRACTOR',
    tonnage: driver.tonnage || '', tractorNo: driver.tractorNo || '', chassisNo: driver.chassisNo || '',
    vehicleStatus: driver.vehicleStatus || '', vehicleRegistered: !!driver.vehicleRegistered,
  }
}
const cancelEdit = () => { editingId.value = null; editForm.value = {} }

const saveDriver = async (driver) => {
  clearMessages()
  if (!editForm.value.driverName?.trim() || !editForm.value.driverContact?.trim()) {
    errorMessage.value = '기사명과 연락처를 입력하세요.'; return
  }
  savingId.value = driver.driverId
  try {
    await updateDriver(driver.driverId, {
      driverName: editForm.value.driverName.trim(), driverContact: editForm.value.driverContact.trim(),
      isRegistered: driver.isRegistered, canEnter: driver.canEnter,
      carrierId: driver.carrierId, userId: driver.userId,
    })
    if (driver.vehicleId) {
      await updateVehicle(driver.vehicleId, {
        plateNumber: editForm.value.plateNumber.trim(), vehicleType: driver.vehicleType || 'TRACTOR',
        tonnage: editForm.value.tonnage, isRegistered: editForm.value.vehicleRegistered,
        vehicleStatus: editForm.value.vehicleStatus, tractorNo: editForm.value.tractorNo,
        chassisNo: editForm.value.chassisNo, driverId: driver.driverId,
        carrierId: driver.carrierId, userId: driver.userId,
      })
    }
    message.value = `${editForm.value.driverName} 기사 정보를 수정했습니다.`
    cancelEdit(); await loadDrivers()
  } catch (error) { errorMessage.value = error.message || '기사 정보 수정에 실패했습니다.' }
  finally { savingId.value = null }
}

const removeDriver = async (driver) => {
  clearMessages()
  if (!window.confirm(`${driver.driverName} 기사와 연결된 트랙터 정보를 삭제하시겠습니까?`)) return
  deletingId.value = driver.driverId
  try {
    await deleteDriver(driver.driverId)
    message.value = `${driver.driverName} 기사 정보를 삭제했습니다.`
    await loadDrivers()
  } catch (error) {
    errorMessage.value = error.message || '작업오더 등 연결 데이터가 있는 기사는 삭제할 수 없습니다.'
  } finally { deletingId.value = null }
}

onMounted(loadDrivers)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <div><h2>기사 관리</h2><p>소속 기사와 회원가입 당시 등록한 트랙터 정보를 조회·수정·삭제합니다.</p></div>
        <span class="status-pill green">{{ drivers.length }}명</span>
      </div>
      <div class="toolbar">
        <input v-model="keyword" type="search" placeholder="기사명, 연락처, 차량번호, 트랙터번호, 샤시번호 검색" />
        <select v-model="statusFilter">
          <option value="ALL">전체 상태</option><option value="PENDING">운송사 승인 대기</option>
          <option value="ADMIN_WAITING">관리자 승인 대기</option><option value="ACTIVE">최종 승인 완료</option>
        </select>
        <button type="button" @click="loadDrivers">새로고침</button>
      </div>
      <div v-if="message" class="form-message success">{{ message }}</div>
      <div v-if="errorMessage" class="form-message error">{{ errorMessage }}</div>
      <div v-if="loading" class="empty-box">기사 정보를 불러오는 중...</div>
      <div v-else-if="filteredDrivers.length === 0" class="empty-box">조건에 맞는 기사가 없습니다.</div>
      <div v-else class="driver-list">
        <article v-for="driver in filteredDrivers" :key="driver.driverId" class="driver-card">
          <header>
            <div><h3>{{ driver.driverName }}</h3><span>{{ driver.driverContact || '-' }}</span><small>로그인 ID {{ driver.loginId || '-' }} · 기사 ID {{ driver.driverId }}</small></div>
            <div class="actions"><span class="status-pill" :class="statusClass(driver)">{{ statusText(driver) }}</span>
              <button v-if="editingId !== driver.driverId" class="edit" @click="startEdit(driver)">수정</button>
              <button v-if="editingId !== driver.driverId" class="delete" :disabled="deletingId === driver.driverId" @click="removeDriver(driver)">{{ deletingId === driver.driverId ? '삭제 중' : '삭제' }}</button>
              <button v-if="editingId === driver.driverId" class="save" :disabled="savingId === driver.driverId" @click="saveDriver(driver)">{{ savingId === driver.driverId ? '저장 중' : '저장' }}</button>
              <button v-if="editingId === driver.driverId" class="cancel" @click="cancelEdit">취소</button>
            </div>
          </header>
          <div class="grid">
            <section><h4>기사 정보</h4>
              <dl v-if="editingId !== driver.driverId"><div><dt>기사명</dt><dd>{{ driver.driverName }}</dd></div><div><dt>연락처</dt><dd>{{ driver.driverContact || '-' }}</dd></div><div><dt>계정 상태</dt><dd>{{ driver.userStatus || '-' }}</dd></div><div><dt>게이트 출입</dt><dd>{{ driver.canEnter ? '가능' : '불가' }}</dd></div></dl>
              <div v-else class="edit-grid"><label>기사명<input v-model="editForm.driverName" /></label><label>연락처<input v-model="editForm.driverContact" /></label></div>
            </section>
            <section><h4>회원가입 트랙터 정보</h4>
              <dl v-if="driver.vehicleId && editingId !== driver.driverId"><div><dt>차량번호</dt><dd class="plate">{{ driver.plateNumber || '-' }}</dd></div><div><dt>차량 종류</dt><dd>{{ driver.vehicleType || '-' }}</dd></div><div><dt>톤수</dt><dd>{{ driver.tonnage || '-' }}</dd></div><div><dt>트랙터번호</dt><dd>{{ driver.tractorNo || '-' }}</dd></div><div class="wide"><dt>샤시번호</dt><dd>{{ driver.chassisNo || '-' }}</dd></div><div><dt>차량 승인</dt><dd>{{ driver.vehicleRegistered ? '승인 완료' : '승인 대기' }}</dd></div><div><dt>차량 상태</dt><dd>{{ driver.vehicleStatus || '-' }}</dd></div></dl>
              <div v-else-if="driver.vehicleId" class="edit-grid tractor"><label>차량번호<input v-model="editForm.plateNumber" /></label><label>톤수<input v-model="editForm.tonnage" /></label><label>트랙터번호<input v-model="editForm.tractorNo" /></label><label>샤시번호<input v-model="editForm.chassisNo" /></label><label>차량 상태<input v-model="editForm.vehicleStatus" /></label><label class="check"><input v-model="editForm.vehicleRegistered" type="checkbox" /> 차량 승인</label></div>
              <div v-else class="empty-vehicle">회원가입 트랙터 정보가 없습니다.</div>
            </section>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.section-title p{margin:5px 0 0;color:var(--ink-500);font-size:12px}.toolbar{display:grid;grid-template-columns:1fr 190px auto;gap:8px;margin-bottom:12px}.toolbar input,.toolbar select,.toolbar button,.edit-grid input{min-height:36px;padding:0 10px;border:1px solid var(--line);border-radius:3px;background:#fff}.driver-list{display:grid;gap:10px}.driver-card{border:1px solid var(--line);background:#fff}.driver-card header{display:flex;justify-content:space-between;gap:12px;padding:12px;background:#f7fafc;border-bottom:1px solid var(--line)}h3{margin:0;font-size:15px}.driver-card header span,.driver-card header small{display:block;margin-top:3px;color:var(--ink-500);font-size:12px}.actions{display:flex;align-items:center;gap:6px;flex-wrap:wrap}.actions button{min-height:32px;padding:0 11px;border-radius:3px;font-weight:700;cursor:pointer}.edit{color:#075ca8;background:#eef7ff;border:1px solid #b8d8f4}.delete{color:#b42318;background:#fff1f0;border:1px solid #f7b9b3}.save{color:#067647;background:#ecfdf3;border:1px solid #abefc6}.cancel{color:#475467;background:#f2f4f7;border:1px solid #d0d5dd}.grid{display:grid;grid-template-columns:.8fr 1.4fr;gap:12px;padding:12px}.grid section{padding:12px;background:#fbfcfe;border:1px solid #d8e2ec}.grid h4{margin:0 0 10px;color:#173b5f}.grid dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px 16px;margin:0}.grid dl .wide{grid-column:1/-1}dt{font-size:11px;color:var(--ink-500);font-weight:700}dd{margin:3px 0 0;font-size:13px;font-weight:800;overflow-wrap:anywhere}.plate{color:#075ca8}.edit-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.edit-grid label{display:grid;gap:4px;color:var(--ink-500);font-size:11px;font-weight:700}.edit-grid.tractor{grid-template-columns:repeat(2,minmax(0,1fr))}.edit-grid .check{display:flex;align-items:center;gap:7px}.edit-grid .check input{min-height:auto}.empty-vehicle{padding:18px;text-align:center;color:var(--ink-500);border:1px dashed var(--line)}.status-pill.blue{color:#075ca8;background:#eef7ff;border-color:#b8d8f4}@media(max-width:900px){.toolbar,.grid{grid-template-columns:1fr}.driver-card header{flex-direction:column}.edit-grid,.edit-grid.tractor{grid-template-columns:1fr}}
</style>
