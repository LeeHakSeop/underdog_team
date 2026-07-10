<script setup>
import { computed, onMounted, ref } from 'vue'
import { approveWorkOrder, fetchWorkOrders } from '@/api/adminApi/workOrderApi'

const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const pendingOrders = computed(() => orders.value.filter((order) => !order.isApproved))
const activeOrders = computed(() => orders.value.filter((order) => order.isApproved))

const statusText = (status) => ({
  DISPATCH_WAITING: '승인 대기', APPROVED: '승인 완료', IN_PROGRESS: '작업 진행 중', COMPLETED: '작업 완료', CANCELLED: '취소',
}[status] || status || '-')

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try { orders.value = (await fetchWorkOrders()) || [] }
  catch (error) { errorMessage.value = error.message || '작업 목록을 불러오지 못했습니다.' }
  finally { loading.value = false }
}

const approve = async (order) => {
  try { await approveWorkOrder(order.workOrderId); await load() }
  catch (error) { errorMessage.value = error.message || '작업 승인에 실패했습니다.' }
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <section v-if="errorMessage" class="panel error-panel">{{ errorMessage }}</section>
    <section class="panel">
      <div class="section-title"><h2>운송사 작업 승인</h2><span class="status-pill amber">승인 대기 {{ pendingOrders.length }}건</span></div>
      <div v-if="loading">불러오는 중...</div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead><tr><th>ID</th><th>운송사</th><th>기사</th><th>트레일러</th><th>컨테이너</th><th>작업</th><th>예약</th><th>처리</th></tr></thead>
          <tbody>
            <tr v-for="order in pendingOrders" :key="order.workOrderId">
              <td>{{ order.workOrderId }}</td><td>{{ order.carrierName || '-' }}</td><td>{{ order.driverName || '-' }}</td>
              <td>{{ order.trailerPlateNumber || order.plateNumber || '-' }}</td><td>{{ order.containerNumber || '-' }}</td>
              <td>{{ order.workType }}</td><td>{{ order.reservedTime }}</td>
              <td><button class="primary-button" type="button" @click="approve(order)">승인</button></td>
            </tr>
            <tr v-if="pendingOrders.length === 0"><td colspan="8">승인 대기 작업이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title"><h2>작업 현황</h2><span class="status-pill green">{{ activeOrders.length }}건</span></div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>ID</th><th>운송사</th><th>기사</th><th>트레일러</th><th>컨테이너</th><th>상태</th></tr></thead>
          <tbody>
            <tr v-for="order in activeOrders" :key="order.workOrderId">
              <td>{{ order.workOrderId }}</td><td>{{ order.carrierName || '-' }}</td><td>{{ order.driverName || '-' }}</td>
              <td>{{ order.trailerPlateNumber || order.plateNumber || '-' }}</td><td>{{ order.containerNumber || '-' }}</td>
              <td><span class="status-pill" :class="order.workStatus === 'COMPLETED' ? 'green' : ''">{{ statusText(order.workStatus) }}</span></td>
            </tr>
            <tr v-if="activeOrders.length === 0"><td colspan="6">승인된 작업이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.error-panel { color:#991b1b; background:#fff1f1; border-color:#fecaca; }
</style>
