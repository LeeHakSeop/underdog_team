<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { fetchVehicles } from '@/api/vehicleApi'
import { createWorkOrder, fetchCarrierWorkOrders } from '@/api/adminApi/workOrderApi'

const currentUser = readCurrentUser()
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')
const carriers = ref([])
const drivers = ref([])
const vehicles = ref([])
const workOrders = ref([])

const form = ref({ driverId: null, vehicleId: null, workType: 'DELIVERY', reservedTime: '' })

const myCarrier = computed(() => carriers.value.find((item) => item.userId === currentUser?.userId))
const myDrivers = computed(() => {
  if (!myCarrier.value) return []
  return drivers.value.filter((driver) =>
    driver.carrierId === myCarrier.value.carrierId && driver.isRegistered === true,
  )
})
const availableTrailers = computed(() => {
  if (!myCarrier.value) return []
  return vehicles.value.filter((vehicle) =>
    vehicle.carrierId === myCarrier.value.carrierId
    && ['TRAILER', '트레일러'].includes(String(vehicle.vehicleType || '').toUpperCase())
    && vehicle.isRegistered === true
    && (!vehicle.driverId || vehicle.driverId === form.value.driverId),
  )
})

const statusText = (status) => ({
  DISPATCH_WAITING: '관리자 승인 대기',
  APPROVED: '기사 전달 완료',
  IN_PROGRESS: '작업 진행 중',
  COMPLETED: '작업 완료',
  CANCELLED: '취소',
}[status] || status || '-')

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [carrierData, driverData, vehicleData] = await Promise.all([
      fetchCarriers(), fetchDrivers(), fetchVehicles(),
    ])
    carriers.value = carrierData || []
    drivers.value = driverData || []
    vehicles.value = vehicleData || []
    if (currentUser?.userId) {
      workOrders.value = (await fetchCarrierWorkOrders(currentUser.userId)) || []
    }
  } catch (error) {
    errorMessage.value = error.message || '작업오더 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  message.value = ''
  errorMessage.value = ''
  if (!form.value.driverId || !form.value.vehicleId || !form.value.reservedTime) {
    errorMessage.value = '기사, 트레일러, 예약 시간을 모두 선택하세요.'
    return
  }
  saving.value = true
  try {
    await createWorkOrder({
      driverId: form.value.driverId,
      vehicleId: form.value.vehicleId,
      trailerVehicleId: form.value.vehicleId,
      workType: form.value.workType,
      reservedTime: form.value.reservedTime,
      workStatus: 'DISPATCH_WAITING',
      isApproved: false,
      containerId: null,
    })
    message.value = '작업오더를 등록했습니다. 관리자 승인 후 기사 페이지에서 시작할 수 있습니다.'
    form.value = { driverId: null, vehicleId: null, workType: 'DELIVERY', reservedTime: '' }
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '작업오더 등록에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section v-if="message" class="panel success-panel">{{ message }}</section>
    <section v-if="errorMessage" class="panel error-panel">{{ errorMessage }}</section>

    <section class="panel">
      <div class="section-title">
        <h2>기사 작업오더 전달</h2>
        <span class="status-pill">{{ myCarrier?.carrierName || '운송사' }}</span>
      </div>
      <form class="form-grid" @submit.prevent="submit">
        <div class="field">
          <label>기사</label>
          <select v-model.number="form.driverId" @change="form.vehicleId = null">
            <option :value="null" disabled>소속 기사 선택</option>
            <option v-for="driver in myDrivers" :key="driver.driverId" :value="driver.driverId">
              {{ driver.driverName }} / {{ driver.driverContact || '-' }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>트레일러</label>
          <select v-model.number="form.vehicleId">
            <option :value="null" disabled>승인된 트레일러 선택</option>
            <option v-for="vehicle in availableTrailers" :key="vehicle.vehicleId" :value="vehicle.vehicleId">
              {{ vehicle.plateNumber }} / {{ vehicle.tonnage || '-' }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>작업 유형</label>
          <select v-model="form.workType">
            <option value="DELIVERY">운송 작업</option>
            <option value="PICKUP">픽업 작업</option>
            <option value="RETURN">반납 작업</option>
          </select>
        </div>
        <div class="field">
          <label>예약 시간</label>
          <input v-model="form.reservedTime" type="datetime-local" />
        </div>
        <button class="primary-button full" type="submit" :disabled="saving">
          {{ saving ? '전달 중...' : '작업오더 전달' }}
        </button>
      </form>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>작업오더 현황</h2>
        <span class="status-pill">{{ workOrders.length }}건</span>
      </div>
      <div v-if="loading" class="empty-box">불러오는 중...</div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>기사</th><th>트랙터</th><th>트레일러</th><th>작업</th><th>예약</th><th>승인</th><th>진행상태</th></tr>
          </thead>
          <tbody>
            <tr v-for="order in workOrders" :key="order.workOrderId">
              <td>{{ order.workOrderId }}</td>
              <td>{{ order.driverName || '-' }}</td>
              <td>{{ order.tractorPlateNumber || '-' }}</td>
              <td>{{ order.trailerPlateNumber || order.plateNumber || '-' }}</td>
              <td>{{ order.workType || '-' }}</td>
              <td>{{ order.reservedTime || '-' }}</td>
              <td><span class="status-pill" :class="order.isApproved ? 'green' : 'amber'">{{ order.isApproved ? '승인' : '대기' }}</span></td>
              <td><span class="status-pill" :class="order.workStatus === 'COMPLETED' ? 'green' : ''">{{ statusText(order.workStatus) }}</span></td>
            </tr>
            <tr v-if="workOrders.length === 0"><td colspan="8">전달된 작업오더가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.full{grid-column:1/-1}.error-panel{color:#991b1b;background:#fff1f1;border-color:#fecaca}.success-panel{color:#166534;background:#f0fdf4;border-color:#bbf7d0}.empty-box{padding:24px;text-align:center;color:var(--ink-500)}
</style>
